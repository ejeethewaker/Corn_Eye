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
        // Confidence: 0.55 is a safe starting point for real phone photos;
        //   tune upward once you have real-world logcat data.
        private const val CONFIDENCE_MIN = 0.55f
        // Entropy is normalized to [0,1] by dividing by ln(numClasses),
        //   so 0.80 now correctly means "reject if uncertainty > 80% of max".
        private const val ENTROPY_MAX    = 0.80f
        private const val INVALID_LABEL  = "Invalid"
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
        val interp = interpreter ?: run {
            android.util.Log.e(TAG, "Interpreter is null")
            return null
        }

        // Gallery/camera photos on modern Android may be HARDWARE bitmaps.
        // HARDWARE bitmaps cannot be read with getPixels() — must copy to ARGB_8888 first.
        val softBitmap: Bitmap = if (bitmap.config == Bitmap.Config.HARDWARE ||
                                     bitmap.config != Bitmap.Config.ARGB_8888) {
            android.util.Log.d(TAG, "Converting bitmap config ${bitmap.config} → ARGB_8888")
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else bitmap

        // Center-square crop before scaling — avoids aspect ratio distortion when
        // feeding tall 9:16 phone photos into the square 224×224 model input.
        // Crops the largest centered square from the image, then scales to 224×224.
        val squareSize = minOf(softBitmap.width, softBitmap.height)
        val cropX = (softBitmap.width  - squareSize) / 2
        val cropY = (softBitmap.height - squareSize) / 2
        val croppedBitmap = Bitmap.createBitmap(softBitmap, cropX, cropY, squareSize, squareSize)

        // Scale to 224×224 (matches training resize)
        val scaled = Bitmap.createScaledBitmap(croppedBitmap, INPUT_SIZE, INPUT_SIZE, true)
        // Ensure scaled result is also software-backed
        val argbScaled: Bitmap = if (scaled.config != Bitmap.Config.ARGB_8888)
            scaled.copy(Bitmap.Config.ARGB_8888, false) else scaled

        // Build float32 input buffer: 1×224×224×3
        // Normalize to [0, 1] — identical to Python's: image / 255.0
        val byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
        byteBuffer.order(ByteOrder.nativeOrder())
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        argbScaled.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        for (pixel in pixels) {
            byteBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f)  // R
            byteBuffer.putFloat(((pixel shr 8)  and 0xFF) / 255.0f)  // G
            byteBuffer.putFloat((pixel          and 0xFF) / 255.0f)  // B
        }

        // Run inference
        val numModelOutputs = interp.getOutputTensor(0).shape()[1]
        val outputArray = Array(1) { FloatArray(numModelOutputs) }
        interp.run(byteBuffer, outputArray)
        val scores = outputArray[0]

        val scoresLog = scores.mapIndexed { i, s ->
            "${labels.getOrElse(i) { "?" }}=${"%.3f".format(s)}"
        }.joinToString(", ")
        android.util.Log.d(TAG, "Scores: [$scoresLog]")

        if (scores.isEmpty()) return null

        // Find best class
        val bestIdx = scores.indices.maxByOrNull { scores[it] } ?: return null
        val label = labels.getOrElse(bestIdx) { "Unknown" }
        val confidence = scores[bestIdx]

        // Compute Shannon entropy for uncertainty detection
        val ent = shannonEntropy(scores)

        // OOD gates
        val isInvalid = label.equals(INVALID_LABEL, ignoreCase = true)
        val isValid = !isInvalid && confidence >= CONFIDENCE_MIN && ent <= ENTROPY_MAX

        android.util.Log.d(TAG, "Predicted: $label @ ${"%.3f".format(confidence)}, " +
            "entropy=${"%.3f".format(ent)}, isValid=$isValid")

        return Result(label, confidence, ent, isValid)
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
