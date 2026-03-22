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
 * Expected model: corn_disease_model.tflite in app/src/main/assets/
 *   - Input:  [1, 224, 224, 3]  float32, values normalized to [0.0, 1.0]
 *   - Output: [1, N]             float32 softmax probabilities, N = number of classes
 *
 * Expected labels: labels.txt in app/src/main/assets/
 *   - One label per line, in the same order as model output indices
 *
 * To obtain a compatible model:
 *   1. Train or fine-tune a MobileNetV2/EfficientNet model on a corn disease dataset
 *      (e.g., PlantVillage Corn subset on Kaggle) with the classes matching labels.txt
 *   2. Export to TFLite format with the exact input/output shape above
 *   3. Place the .tflite file in app/src/main/assets/
 *
 * GPU acceleration is attempted automatically and falls back to CPU if unavailable.
 */
class CornDiseaseClassifier(private val context: Context) {

    companion object {
        private const val MODEL_FILE = "corn_disease_model.tflite"
        private const val LABELS_FILE = "labels.txt"
        private const val INPUT_SIZE = 224  // pixels (width & height)
        private const val TAG = "CornClassifier"
    }

    data class Result(val label: String, val confidence: Float)

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
     * Classify a Bitmap — Stage 2 of the two-model pipeline.
     *
     * This method assumes the image has ALREADY been validated as a corn leaf by
     * CornLeafDetector. It classifies which corn disease is present.
     *
     * Supports both model variants:
     *   - 4-class model (labels.txt has 4 entries): probabilities used directly.
     *   - 5-class legacy model (labels.txt includes "Other"): "Other" is excluded
     *     and the remaining probabilities are renormalized so confidence reflects
     *     certainty among corn diseases only (e.g. 0.50/0.70 = 71%).
     *
     * Returns null if the model is not ready or corn-class confidence is too low.
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

        // Scale to 224×224 (matches training resize)
        val scaled = Bitmap.createScaledBitmap(softBitmap, INPUT_SIZE, INPUT_SIZE, true)
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

        // Run inference — use the model's actual output size, not labels.size,
        // so this works whether the deployed model has 4 or 5 output classes.
        val numModelOutputs = interp.getOutputTensor(0).shape()[1]
        val outputArray = Array(1) { FloatArray(numModelOutputs) }
        interp.run(byteBuffer, outputArray)
        val scores = outputArray[0]

        val scoresLog = scores.mapIndexed { i, s ->
            "${labels.getOrElse(i) { "?" }}=${"%.3f".format(s)}"
        }.joinToString(", ")
        android.util.Log.d(TAG, "Scores: [$scoresLog]")

        if (scores.isEmpty()) return null

        // Find the "Other" class index (present in legacy 5-class model; -1 if not found)
        val otherIdx = labels.indexOfFirst { it.equals("Other", ignoreCase = true) }

        // Build a list of (index, score) pairs for corn-disease classes only
        val cornScores = scores.indices
            .filter { it != otherIdx }
            .map { it to scores[it] }

        if (cornScores.isEmpty()) return null

        // Renormalize so confidence is relative to corn classes only
        val cornSum = cornScores.sumOf { it.second.toDouble() }.toFloat()
        val bestCorn = cornScores.maxByOrNull { it.second } ?: return null

        val label = labels.getOrElse(bestCorn.first) { "Unknown" }
        // Renormalized confidence: how sure the model is among the 4 corn diseases
        val confidence = if (cornSum > 0f) bestCorn.second / cornSum else bestCorn.second

        android.util.Log.d(TAG, "Predicted (renorm): $label @ ${"%.3f".format(confidence)} (raw=${bestCorn.second})")

        // Require at least 15% renormalized confidence — the leaf detector already
        // confirmed this is a corn leaf, so we just need a clear winner
        if (confidence < 0.15f) {
            android.util.Log.d(TAG, "→ renormalized confidence too low ($confidence), returning null")
            return null
        }

        return Result(label, confidence)
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
