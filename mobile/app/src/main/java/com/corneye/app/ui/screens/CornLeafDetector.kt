// Corn Leaf Detector
// Validates whether a photo contains a corn (maize) leaf before disease classification.
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
 * Corn Leaf Detector — Stage 1 of the two-model pipeline.
 *
 * Uses corn_leaf_detector.tflite (the 5-class model: Common Rust, Gray Leaf Spot,
 * Healthy, Northern Leaf Blight, Other) to decide whether the image contains a
 * corn leaf before running the disease classifier.
 *
 * Decision rule:
 *   - Sum all non-"Other" class probabilities.
 *   - If that sum >= CORN_THRESHOLD (0.35) → it IS a corn leaf.
 *   - If the "Other" class alone has probability >= OTHER_REJECT_THRESHOLD (0.65) → NOT a corn leaf.
 *   - Otherwise fall back to the sum check.
 *
 * When you have a dedicated binary leaf detection model, simply replace
 * corn_leaf_detector.tflite in assets; this class will continue to work as long
 * as the "Other" label remains present in leaf_labels.txt.
 */
class CornLeafDetector(private val context: Context) {

    companion object {
        private const val MODEL_FILE  = "corn_leaf_detector.tflite"
        private const val LABELS_FILE = "leaf_labels.txt"
        private const val INPUT_SIZE  = 224
        private const val TAG         = "CornLeafDetector"

        // Sum of all corn-disease class probabilities must exceed this to accept as corn leaf
        private const val CORN_THRESHOLD         = 0.35f
        // If "Other" probability alone exceeds this, reject without further checks
        private const val OTHER_REJECT_THRESHOLD = 0.65f
    }

    private var interpreter: Interpreter? = null
    private var labels: List<String>      = emptyList()
    private var gpuDelegate: GpuDelegate? = null
    private var isReady                   = false

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    fun initialize(): Boolean {
        if (isReady) return true
        return try {
            val modelBuffer = loadModelBuffer()
            val options     = buildInterpreterOptions()
            interpreter = Interpreter(modelBuffer, options)
            labels      = FileUtil.loadLabels(context, LABELS_FILE)
            isReady     = labels.isNotEmpty()
            android.util.Log.d(TAG, "Initialized OK — labels: $labels")
            isReady
        } catch (e: IOException) {
            android.util.Log.e(TAG, "Init IO error: ${e.message}", e)
            false
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Init error: ${e.message}", e)
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
    // Detection entry-points
    // -----------------------------------------------------------------------

    /**
     * Returns true if the bitmap appears to be a corn leaf.
     * Must be called from a background thread.
     */
    fun isCornLeaf(bitmap: Bitmap): Boolean {
        if (!isReady) {
            android.util.Log.e(TAG, "isCornLeaf() called but model not ready — treating as corn to avoid false rejections")
            return true
        }
        val interp = interpreter ?: return true

        val softBitmap: Bitmap = if (bitmap.config == Bitmap.Config.HARDWARE ||
                                     bitmap.config != Bitmap.Config.ARGB_8888) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else bitmap

        val scaled = Bitmap.createScaledBitmap(softBitmap, INPUT_SIZE, INPUT_SIZE, true)
        val argbScaled: Bitmap = if (scaled.config != Bitmap.Config.ARGB_8888)
            scaled.copy(Bitmap.Config.ARGB_8888, false) else scaled

        val byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
        byteBuffer.order(ByteOrder.nativeOrder())
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        argbScaled.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        for (pixel in pixels) {
            byteBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f)
            byteBuffer.putFloat(((pixel shr 8)  and 0xFF) / 255.0f)
            byteBuffer.putFloat((pixel          and 0xFF) / 255.0f)
        }

        val outputArray = Array(1) { FloatArray(labels.size) }
        interp.run(byteBuffer, outputArray)
        val scores = outputArray[0]

        val scoresLog = scores.mapIndexed { i, s ->
            "${labels.getOrElse(i) { "?" }}=${"%.3f".format(s)}"
        }.joinToString(", ")
        android.util.Log.d(TAG, "LeafDetect scores: [$scoresLog]")

        // Find the "Other" label index
        val otherIdx = labels.indexOfFirst { it.equals("Other", ignoreCase = true) }

        // If "Other" probability is very high, definitely not a corn leaf
        if (otherIdx >= 0 && scores.getOrElse(otherIdx) { 0f } >= OTHER_REJECT_THRESHOLD) {
            android.util.Log.d(TAG, "→ Other probability ${scores[otherIdx]} >= $OTHER_REJECT_THRESHOLD, NOT a corn leaf")
            return false
        }

        // Sum all non-"Other" probabilities
        var cornSum = 0f
        for (i in scores.indices) {
            if (i != otherIdx) {
                cornSum += scores[i]
            }
        }
        android.util.Log.d(TAG, "→ Corn class probability sum: ${"%.3f".format(cornSum)}")

        val isCorn = cornSum >= CORN_THRESHOLD
        android.util.Log.d(TAG, "→ isCornLeaf = $isCorn")
        return isCorn
    }

    fun isCornLeafFromFile(file: java.io.File): Boolean {
        val opts   = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return false
        return isCornLeaf(bitmap)
    }

    fun isCornLeafFromUri(uri: Uri): Boolean {
        return try {
            val stream = context.contentResolver.openInputStream(uri) ?: return false
            val opts   = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
            val bitmap = BitmapFactory.decodeStream(stream, null, opts)
            stream.close()
            if (bitmap == null) return false
            isCornLeaf(bitmap)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "isCornLeafFromUri error: ${e.message}", e)
            false
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
