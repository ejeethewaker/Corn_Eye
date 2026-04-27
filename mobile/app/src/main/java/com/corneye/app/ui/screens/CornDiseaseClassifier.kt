// Corn Disease Classifier
// Wraps the TFLite model for on-device corn leaf disease inference.
package com.corneye.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Corn Disease Classifier using TensorFlow Lite.
 *
 * Single-model pipeline (5 classes: 4 corn diseases + "Invalid"):
 *   - Input:  [1, 224, 224, 3]  float32, values normalized to [0.0, 1.0]
 *   - Output: [1, 5]             float32 softmax probabilities
 *
 * OOD rejection gates (applied by the caller via [Result.isValid]):
 *   1. Predicted class == "Invalid"       → reject
 *   2. Top confidence < 55%              → reject (tune up once real-photo logs collected)
 *   3. Normalized Shannon entropy > 0.8  → reject (entropy is divided by ln(5) so 1.0 = uniform)
 *
 * Labels (labels.txt, must match training order):
 *   0 - Common Rust
 *   1 - Gray Leaf Spot
 *   2 - Healthy
 *   3 - Northern Leaf Blight
 *   4 - Invalid
 *
 * GPU acceleration is attempted automatically and falls back to CPU if unavailable.
 */
class CornDiseaseClassifier(private val context: Context) {

    companion object {
        private const val MODEL_FILE = "corn_disease_model.tflite"
        private const val LABELS_FILE = "labels.txt"
        private const val INPUT_SIZE = 224  // pixels (width & height)
        private const val TAG = "CornClassifier"

        // OOD gate thresholds — must match test_tflite.py
        // Confidence: 0.72 — restored to original value. 0.60 was too permissive
        // and allowed non-corn images (phone screens, blank images) to pass.
        private const val CONFIDENCE_MIN = 0.72f
        // Entropy is normalized to [0,1] by dividing by ln(numClasses).
        // Lowered from 0.80 to 0.70 — at 0.80 the model could be highly uncertain
        // and still pass. 0.70 means: reject if the model is unsure about more
        // than 70% of its maximum possible uncertainty.
        private const val ENTROPY_MAX    = 0.70f
        private const val INVALID_LABEL  = "Invalid"

        // Minimum confidence for forced-corn fallback classification.
        // Used when the color pre-filter passed (image has corn-like colors) but the
        // model predicted Invalid due to domain shift (trained on lab photos, tested
        // on real-world field photos). Raised to 0.55 — the model must be at least
        // 55% confident in a corn disease class (ignoring Invalid) for the fallback
        // to accept. This prevents borderline non-corn images from slipping through
        // the forced classification path.
        private const val FORCE_CORN_CONFIDENCE_MIN = 0.55f
    }

    data class Result(
        val label: String,
        val confidence: Float,
        val entropy: Float,
        val isValid: Boolean
    )

    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()
    private var gpuDelegate: GpuDelegate? = null
    private var isReady = false

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    /**
     * Load the TFLite model and labels.
     * Returns true when ready; false if the model file is missing or fails to load.
     * Must be called from a background thread.
     */
    fun initialize(): Boolean {
        if (isReady) return true
        return try {
            val modelBuffer = loadModelBuffer()
            val options = buildInterpreterOptions()
            interpreter = Interpreter(modelBuffer, options)
            labels = FileUtil.loadLabels(context, LABELS_FILE)
            isReady = labels.isNotEmpty()
            android.util.Log.d(TAG, "Initialized OK — labels: $labels")
            isReady
        } catch (e: IOException) {
            android.util.Log.e(TAG, "Init IO error: ${e.message}", e)
            isReady = false
            false
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Init error: ${e.message}", e)
            isReady = false
            false
        }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
        gpuDelegate?.close()
        gpuDelegate = null
        isReady = false
    }

    // -----------------------------------------------------------------------
    // Classification entry-points
    // -----------------------------------------------------------------------

    /**
     * Classify a Bitmap — single-model pipeline with OOD gates.
     *
     * The model outputs 5 classes (4 corn diseases + "Invalid").
     * The result includes [isValid] which is false when any OOD gate triggers:
     *   - predicted class is "Invalid"
     *   - top confidence < 70%
     *   - Shannon entropy > 0.8
     *
     * Returns null only if the model is not ready or inference fails.
     * Must be called from a background thread.
     */
    fun classify(bitmap: Bitmap): Result? {
        if (!isReady) {
            android.util.Log.e(TAG, "classify() called but model not ready")
            return null
        }
        val scores = runInference(bitmap) ?: return null
        val bestIdx = scores.indices.maxByOrNull { scores[it] } ?: return null
        val label = labels.getOrElse(bestIdx) { "Unknown" }
        val confidence = scores[bestIdx]
        val ent = shannonEntropy(scores)
        val isInvalid = label.equals(INVALID_LABEL, ignoreCase = true)
        val isValid = !isInvalid && confidence >= CONFIDENCE_MIN && ent <= ENTROPY_MAX
        android.util.Log.d(TAG, "Predicted: $label @ ${"%.3f".format(confidence)}, " +
            "entropy=${"%.3f".format(ent)}, isValid=$isValid")
        return Result(label, confidence, ent, isValid)
    }

    /**
     * Returns the highest-scoring non-Invalid class, ignoring the Invalid label entirely.
     *
     * Used as a last-resort fallback when:
     *   1. The color pre-filter PASSED (image has corn-like green colors), AND
     *   2. The normal classify() rejected (predicted Invalid or low confidence).
     *
     * The model was trained on PlantVillage lab images (isolated leaves, white backgrounds)
     * but real-world field photos (natural lighting, multiple leaves, hands, soil) look very
     * different.  The model mistakes these conditions for "Invalid", but the color filter
     * already confirmed the image contains a corn-like plant.
     *
     * Uses FORCE_CORN_CONFIDENCE_MIN (0.10) instead of CONFIDENCE_MIN (0.60).
     */
    fun classifyForceCorn(bitmap: Bitmap): Result? {
        if (!isReady) {
            android.util.Log.e(TAG, "classifyForceCorn() called but model not ready")
            return null
        }
        val scores = runInference(bitmap) ?: return null
        val invalidIdx = labels.indexOfFirst { it.equals(INVALID_LABEL, ignoreCase = true) }
        val bestCornIdx = scores.indices
            .filter { it != invalidIdx }
            .maxByOrNull { scores[it] } ?: return null
        val label = labels[bestCornIdx]
        val confidence = scores[bestCornIdx]
        val ent = shannonEntropy(scores)
        val isValid = confidence >= FORCE_CORN_CONFIDENCE_MIN
        android.util.Log.d(TAG, "ForceCorn: $label @ ${"%.3f".format(confidence)}, " +
            "entropy=${"%.3f".format(ent)}, isValid=$isValid")
        return Result(label, confidence, ent, isValid)
    }

    fun classifyForceCornFromFile(file: java.io.File): Result? {
        val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, opts) ?: run {
            android.util.Log.e(TAG, "decodeFile returned null for ${file.absolutePath}")
            return null
        }
        return classifyForceCorn(bitmap)
    }

    fun classifyForceCornFromUri(uri: Uri): Result? {
        return try {
            val stream = context.contentResolver.openInputStream(uri) ?: run {
                android.util.Log.e(TAG, "openInputStream returned null for $uri")
                return null
            }
            val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
            val bitmap = BitmapFactory.decodeStream(stream, null, opts)
            stream.close()
            if (bitmap == null) {
                android.util.Log.e(TAG, "decodeStream returned null for $uri")
                return null
            }
            classifyForceCorn(bitmap)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "classifyForceCornFromUri error: ${e.message}", e)
            null
        }
    }

    /** Classify from a JPEG/PNG file on disk. */
    fun classifyFromFile(file: java.io.File): Result? {
        // Force ARGB_8888 so getPixels() always works
        val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, opts) ?: run {
            android.util.Log.e(TAG, "decodeFile returned null for ${file.absolutePath}")
            return null
        }
        return classify(bitmap)
    }

    /** Classify from a content URI (e.g., picked from gallery). */
    fun classifyFromUri(uri: Uri): Result? {
        return try {
            val stream = context.contentResolver.openInputStream(uri) ?: run {
                android.util.Log.e(TAG, "openInputStream returned null for $uri")
                return null
            }
            // Force ARGB_8888 so getPixels() always works on gallery images
            val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
            val bitmap = BitmapFactory.decodeStream(stream, null, opts)
            stream.close()
            if (bitmap == null) {
                android.util.Log.e(TAG, "decodeStream returned null for $uri")
                return null
            }
            classify(bitmap)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "classifyFromUri error: ${e.message}", e)
            null
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Runs the full preprocessing pipeline and TFLite inference.
     * Returns the raw softmax score array, or null on error.
     * Shared by classify() and classifyForceCorn().
     */
    private fun runInference(bitmap: Bitmap): FloatArray? {
        val interp = interpreter ?: run {
            android.util.Log.e(TAG, "Interpreter is null")
            return null
        }
        // Gallery/camera photos on modern Android may be HARDWARE bitmaps.
        val softBitmap: Bitmap = if (bitmap.config == Bitmap.Config.HARDWARE ||
                                     bitmap.config != Bitmap.Config.ARGB_8888) {
            android.util.Log.d(TAG, "Converting bitmap config ${bitmap.config} → ARGB_8888")
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else bitmap
        // Center-square crop then scale to 224×224
        val squareSize = minOf(softBitmap.width, softBitmap.height)
        val cropX = (softBitmap.width  - squareSize) / 2
        val cropY = (softBitmap.height - squareSize) / 2
        val croppedBitmap = Bitmap.createBitmap(softBitmap, cropX, cropY, squareSize, squareSize)
        val scaled = Bitmap.createScaledBitmap(croppedBitmap, INPUT_SIZE, INPUT_SIZE, true)
        val argbScaled: Bitmap = if (scaled.config != Bitmap.Config.ARGB_8888)
            scaled.copy(Bitmap.Config.ARGB_8888, false) else scaled
        // Build float32 input buffer: 1×224×224×3, normalized to [0,1]
        val byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
        byteBuffer.order(ByteOrder.nativeOrder())
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        argbScaled.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        for (pixel in pixels) {
            byteBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f)
            byteBuffer.putFloat(((pixel shr 8)  and 0xFF) / 255.0f)
            byteBuffer.putFloat((pixel          and 0xFF) / 255.0f)
        }
        val numModelOutputs = interp.getOutputTensor(0).shape()[1]
        val outputArray = Array(1) { FloatArray(numModelOutputs) }
        interp.run(byteBuffer, outputArray)
        val scores = outputArray[0]
        val scoresLog = scores.mapIndexed { i, s ->
            "${labels.getOrElse(i) { "?" }}=${"%.3f".format(s)}"
        }.joinToString(", ")
        android.util.Log.d(TAG, "Scores: [$scoresLog]")
        return if (scores.isEmpty()) null else scores
    }

    /**
     * Normalized Shannon entropy of a probability distribution.
     * Divides by ln(numClasses) so the result is always in [0, 1]:
     *   0.0 = completely certain (one class = 1.0)
     *   1.0 = totally uncertain (uniform distribution)
     * Without normalization, ln(5) ≈ 1.609 would be the max for 5 classes,
     * making a raw threshold of 0.8 far too strict.
     */
    private fun shannonEntropy(probs: FloatArray): Float {
        if (probs.size <= 1) return 0f
        var h = 0.0
        for (p in probs) {
            if (p > 1e-10f) h -= p * Math.log(p.toDouble())
        }
        val maxEntropy = Math.log(probs.size.toDouble())  // ln(5) ≈ 1.609
        return (h / maxEntropy).toFloat()
    }

    private fun loadModelBuffer(): MappedByteBuffer {
        val afd = context.assets.openFd(MODEL_FILE)
        return FileInputStream(afd.fileDescriptor).channel.map(
            FileChannel.MapMode.READ_ONLY,
            afd.startOffset,
            afd.declaredLength
        )
    }

    private fun buildInterpreterOptions(): Interpreter.Options {
        val options = Interpreter.Options().apply { numThreads = 4 }
        // Attempt GPU acceleration; silently fall back to CPU on failure or unsupported arch
        return try {
            gpuDelegate = GpuDelegate()
            options.addDelegate(gpuDelegate!!)
            options
        } catch (t: Throwable) {
            gpuDelegate = null
            options
        }
    }
}
