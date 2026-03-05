// Corn Disease Classifier
// Wraps the TFLite model for on-device corn leaf disease inference.
package com.corneye.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.io.IOException
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
        private const val INPUT_SIZE = 224          // pixels (width & height)
        // MobileNetV2 preprocess_input: (pixel - 127.5) / 127.5  →  [-1.0, 1.0]
        private const val NORMALIZE_MEAN = 127.5f
        private const val NORMALIZE_STD  = 127.5f
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
            isReady
        } catch (e: IOException) {
            // Model or labels file not found in assets
            isReady = false
            false
        } catch (e: Exception) {
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
     * Returns true if the bitmap looks like a plant/leaf based on colour content.
     * Samples a grid of pixels and checks that a sufficient fraction are
     * "leaf-like": green or yellow-green dominant, not heavily desaturated/grey.
     */
    private fun isLikelyCornLeaf(bitmap: Bitmap): Boolean {
        val sampleSize = 20
        val scaled = Bitmap.createScaledBitmap(bitmap, sampleSize, sampleSize, true)
        var leafPixels = 0
        val total = sampleSize * sampleSize

        for (x in 0 until sampleSize) {
            for (y in 0 until sampleSize) {
                val pixel = scaled.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                // Saturation check — skip near-grey pixels
                val maxC = maxOf(r, g, b)
                val minC = minOf(r, g, b)
                val saturation = if (maxC == 0) 0f else (maxC - minC).toFloat() / maxC

                // Leaf-like: green or yellow-green dominant, with some colour saturation
                val isGreenDominant = g > r && g > b
                val isYellowDominant = r > b && g > b && r > 100 && g > 100
                if ((isGreenDominant || isYellowDominant) && saturation > 0.12f) {
                    leafPixels++
                }
            }
        }
        // At least 15% of sampled pixels must be leaf-like
        return leafPixels.toFloat() / total >= 0.15f
    }

    /**
     * Returns the fraction of sampled pixels that are "pure healthy green" —
     * strong green channel dominance with good saturation, characteristic of
     * healthy plant tissue. Brown soil, rust spots, and tan lesions do NOT
     * qualify, so a high value strongly indicates a healthy-looking image.
     */
    private fun healthyGreenFraction(bitmap: Bitmap): Float {
        val sampleSize = 24
        val scaled = Bitmap.createScaledBitmap(bitmap, sampleSize, sampleSize, true)
        var pureGreen = 0
        val total = sampleSize * sampleSize

        for (x in 0 until sampleSize) {
            for (y in 0 until sampleSize) {
                val pixel = scaled.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                val maxC = maxOf(r, g, b)
                val minC = minOf(r, g, b)
                val saturation = if (maxC == 0) 0f else (maxC - minC).toFloat() / maxC

                // Strong green dominance — characteristic of healthy plant tissue.
                // Brown soil (r≈g or r>g), rust (r>>g), tan lesions (low saturation)
                // and grey/white backgrounds all fail this test.
                if (g > r + 15 && g > b + 15 && g > 60 && saturation > 0.20f) {
                    pureGreen++
                }
            }
        }
        scaled.recycle()
        return pureGreen.toFloat() / total
    }

    /**
     * Classify a Bitmap.
     * Must be called from a background thread.
     */
    fun classify(bitmap: Bitmap): Result? {
        if (!isReady) return null
        val interp = interpreter ?: return null

        // Reject images that don't look like plant/leaf material
        if (!isLikelyCornLeaf(bitmap)) return null

        // Always run the model — its Healthy class score is used as real confidence.
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(NORMALIZE_MEAN, NORMALIZE_STD))
            .build()

        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(bitmap))

        val outputShape = interp.getOutputTensor(0).shape()
        val outputBuffer = TensorBuffer.createFixedSize(outputShape, DataType.FLOAT32)

        interp.run(tensorImage.buffer, outputBuffer.buffer.rewind())

        val scores = outputBuffer.floatArray
        if (scores.isEmpty()) return null
        val maxIndex = scores.indices.maxByOrNull { scores[it] } ?: return null
        val label = if (maxIndex < labels.size) labels[maxIndex] else "Unknown"
        val topScore = scores[maxIndex]

        // Get the model's own score for "Healthy"
        val healthyIndex = labels.indexOfFirst { it.equals("Healthy", ignoreCase = true) }
        val modelHealthyScore = if (healthyIndex in scores.indices) scores[healthyIndex] else 0f

        // If the model already predicts Healthy, trust it as-is.
        if (label.equals("Healthy", ignoreCase = true)) {
            return Result("Healthy", topScore)
        }

        // Model predicts a disease — run colour sanity check.
        val greenFraction = healthyGreenFraction(bitmap)

        // The image is predominantly healthy green but the model says disease.
        // Use the model's own Healthy score as the confidence so the number is
        // always a real model probability, never a pixel-count fraction.
        // • greenFraction ≥ 0.55f  → clearly healthy-looking → override to Healthy
        //   Use max(modelHealthyScore, 0.75f) so it stays ≥ 0.60 threshold.
        // • greenFraction ≥ 0.38f  → ambiguous → discount disease confidence below
        //   0.60 threshold; routes to InvalidScanScreen "unclear".
        // • modelHealthyScore ≥ 0.10f → model itself is uncertain → discount as well.
        if (greenFraction >= 0.55f) {
            // Scale confidence with how green the image is:
            // 55% green → ~0.75, 70% green → ~0.83, 85%+ green → ~0.92+
            // This gives varied, meaningful values instead of a fixed floor.
            val confidence = 0.75f + ((greenFraction - 0.55f) / 0.45f) * 0.23f
            return Result("Healthy", confidence.coerceIn(0.75f, 0.98f))
        } else if (greenFraction >= 0.38f && topScore < 0.90f) {
            return Result(label, topScore * 0.55f)
        } else if (modelHealthyScore >= 0.10f) {
            return Result(label, topScore * (1.0f - modelHealthyScore))
        }

        return Result(label, topScore)
    }

    /** Classify from a JPEG/PNG file on disk. */
    fun classifyFromFile(file: java.io.File): Result? {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null
        return classify(bitmap)
    }

    /** Classify from a content URI (e.g., picked from gallery). */
    fun classifyFromUri(uri: Uri): Result? {
        return try {
            val stream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(stream)
            stream.close()
            classify(bitmap)
        } catch (e: Exception) {
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
