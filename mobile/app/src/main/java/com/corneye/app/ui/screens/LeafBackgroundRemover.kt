// Leaf Background Remover
// Uses ML Kit Subject Segmentation to isolate the corn leaf from its background.
// Background pixels are replaced with solid black, matching the reference lab-photo style.
package com.corneye.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Removes the background from a corn leaf image using ML Kit Subject Segmentation.
 *
 * The segmentation model runs fully on-device (no internet required). It produces
 * a per-pixel foreground confidence mask; pixels below [MASK_THRESHOLD] are replaced
 * with [bgColor] (default: black).
 *
 * All methods must be called from a background thread — they use [Tasks.await]
 * internally so they block until the ML Kit task completes.
 */
object LeafBackgroundRemover {

    private const val TAG = "LeafBgRemover"
    private val segmentationEnabled = AtomicBoolean(true)
    private val emulatorSkipLogged = AtomicBoolean(false)

    /**
     * Pixels with foreground confidence ≥ this value are kept as-is.
     * Pixels below this threshold are replaced with [bgColor].
     * 0.5f gives a clean cut; lower values keep more fringe pixels.
     */
    private const val MASK_THRESHOLD = 0.5f

    /**
     * Segments the foreground leaf from [bitmap] and returns a new [Bitmap]
     * with background pixels set to [bgColor].
     *
     * Returns null if segmentation fails — callers should fall back to the
     * original bitmap rather than crashing.
     */
    fun removeBackground(bitmap: Bitmap, bgColor: Int = Color.BLACK): Bitmap? {
        // ML Kit subject segmentation frequently fails on emulators with
        // MediaPipe tensor-dimension errors. Skip there to avoid noisy failures.
        if (isLikelyEmulator()) {
            if (emulatorSkipLogged.compareAndSet(false, true)) {
                android.util.Log.w(TAG, "Skipping background removal on emulator due to ML Kit instability")
            }
            return null
        }

        // Circuit breaker: once a known segmentation backend failure happens,
        // skip future attempts for this app process.
        if (!segmentationEnabled.get()) {
            return null
        }

        if (bitmap.width <= 0 || bitmap.height <= 0) {
            android.util.Log.w(TAG, "Invalid bitmap size for segmentation: ${bitmap.width}x${bitmap.height}")
            return null
        }

        return try {
            val options = SubjectSegmenterOptions.Builder()
                .enableForegroundConfidenceMask()
                .build()
            val segmenter = SubjectSegmentation.getClient(options)

            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val segResult = Tasks.await(segmenter.process(inputImage))

            val mask = segResult.foregroundConfidenceMask
                ?: return null.also { android.util.Log.w(TAG, "No foreground mask returned") }

            // Ensure we have a software-backed ARGB_8888 bitmap for getPixels()
            val argbBitmap = if (bitmap.config != Bitmap.Config.ARGB_8888)
                bitmap.copy(Bitmap.Config.ARGB_8888, false) else bitmap

            val w = argbBitmap.width
            val h = argbBitmap.height
            val pixels = IntArray(w * h)
            argbBitmap.getPixels(pixels, 0, w, 0, 0, w, h)

            // Apply the mask: replace background pixels with bgColor
            // foregroundConfidenceMask is a FloatBuffer — use capacity() and get(i)
            val maskCapacity = mask.capacity()
            for (i in pixels.indices) {
                if (i < maskCapacity && mask.get(i) < MASK_THRESHOLD) {
                    pixels[i] = bgColor
                }
            }

            val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            output.setPixels(pixels, 0, w, 0, 0, w, h)
            android.util.Log.d(TAG, "Background removed successfully (${w}×${h})")
            output
        } catch (e: Exception) {
            val msg = (e.message ?: "") + " " + (e.cause?.message ?: "")
            if (msg.contains("Tensor should have 2, 3, or 4 dims", ignoreCase = true) ||
                msg.contains("TensorsToSegmentationCalculator", ignoreCase = true) ||
                msg.contains("Failed to run thin subject segmenter", ignoreCase = true)) {
                segmentationEnabled.set(false)
                android.util.Log.w(TAG, "Disabling background removal for this session after segmentation backend failure")
            }
            android.util.Log.e(TAG, "removeBackground failed: ${e.message}", e)
            null
        }
    }

    private fun isLikelyEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT ?: ""
        val model = Build.MODEL ?: ""
        val manufacturer = Build.MANUFACTURER ?: ""
        val brand = Build.BRAND ?: ""
        val device = Build.DEVICE ?: ""
        val product = Build.PRODUCT ?: ""

        return fingerprint.startsWith("generic") ||
            fingerprint.contains("emulator", ignoreCase = true) ||
            fingerprint.contains("sdk_gphone", ignoreCase = true) ||
            model.contains("Emulator", ignoreCase = true) ||
            model.contains("Android SDK built for", ignoreCase = true) ||
            manufacturer.contains("Genymotion", ignoreCase = true) ||
            (brand.startsWith("generic") && device.startsWith("generic")) ||
            product.contains("sdk", ignoreCase = true)
    }

    /**
     * Saves [bitmap] as a JPEG to a temp file in the app cache directory.
     * Returns the saved [File].
     */
    fun saveToCacheFile(context: Context, bitmap: Bitmap, filename: String): File {
        val file = File(context.cacheDir, filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        return file
    }
}
