// Corn Leaf Detector
// Fast green pixel pre-filter — rejects obviously non-plant images before model inference.
package com.corneye.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

/**
 * Corn Leaf Detector — green pixel ratio pre-filter (Stage 1).
 *
 * Single-model pipeline:
 *   1. CornLeafDetector.hasPlantColors() — this class (fast color check, no model)
 *   2. CornDiseaseClassifier.classify()  — TFLite model with OOD gates
 *
 * No TFLite model is loaded. This class only checks whether the image has
 * enough green/plant-like pixels to be worth sending to the classifier.
 *
 * Three checks must ALL pass:
 *   1. Green pixel ratio ≥ 25%  — at least 1/4 of sampled pixels are green-hued
 *   2. Green saturation  ≥ 25%  — those green pixels have vivid color (not washed out)
 *   3. Green dominance   ≥ 38%  — green channel is strongest overall
 */
class CornLeafDetector(private val context: Context) {

    companion object {
        private const val TAG = "CornLeafDetector"
        private const val INPUT_SIZE = 224

        // ── Color pre-check constants ──
        private const val MIN_GREEN_RATIO        = 0.25f
        private const val MIN_GREEN_SATURATION   = 0.25f
        private const val MIN_GREEN_DOMINANCE    = 0.38f
    }

    // No model to load — always ready
    fun initialize(): Boolean = true
    fun close() { /* no-op */ }

    // -----------------------------------------------------------------------
    // Detection entry-points
    // -----------------------------------------------------------------------

    /**
     * Returns true if the bitmap has enough green/plant-like colors.
     * Fast check — runs on pixel data only, no model inference.
     */
    fun isCornLeaf(bitmap: Bitmap): Boolean {
        val softBitmap: Bitmap = if (bitmap.config == Bitmap.Config.HARDWARE ||
                                     bitmap.config != Bitmap.Config.ARGB_8888) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else bitmap

        val scaled = Bitmap.createScaledBitmap(softBitmap, INPUT_SIZE, INPUT_SIZE, true)
        val argbScaled: Bitmap = if (scaled.config != Bitmap.Config.ARGB_8888)
            scaled.copy(Bitmap.Config.ARGB_8888, false) else scaled

        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        argbScaled.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        return hasPlantLikeColors(pixels)
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

    /**
     * Multi-layer color sanity check to reject non-plant images.
     *
     * Three checks must ALL pass:
     * 1. Green pixel ratio: ≥25% of sampled pixels are in the green/yellow-green hue range
     *    with meaningful saturation (≥20%) and brightness (≥10%). This rejects obviously
     *    non-green images.
     * 2. Green saturation quality: the average saturation of those green pixels must be
     *    ≥25%. Real corn leaves have vivid greens; screen/monitor light is washed out.
     * 3. Green channel dominance: across ALL pixels, the green channel must be the
     *    strongest on average (G / (R+G+B) ≥ 0.38). Corn leaf photos are green-dominant;
     *    indoor scenes with mixed colors are not.
     *
     * Uses every 4th pixel for speed (~12,500 samples on 224×224).
     */
    private fun hasPlantLikeColors(pixels: IntArray): Boolean {
        val hsv = FloatArray(3)
        var greenCount = 0
        var greenSatSum = 0f
        var sampledCount = 0
        var totalR = 0L
        var totalG = 0L
        var totalB = 0L

        for (i in pixels.indices step 4) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            totalR += r
            totalG += g
            totalB += b
            sampledCount++

            android.graphics.Color.RGBToHSV(r, g, b, hsv)

            // Hue 25-165 covers yellow-green through green (corn leaf range)
            // Saturation >= 0.20 excludes washed-out grays/whites/screen light
            // Value >= 0.10 excludes very dark pixels
            if (hsv[0] in 25f..165f && hsv[1] >= 0.20f && hsv[2] >= 0.10f) {
                greenCount++
                greenSatSum += hsv[1]
            }
        }

        if (sampledCount == 0) return false

        val greenRatio = greenCount.toFloat() / sampledCount
        val avgGreenSat = if (greenCount > 0) greenSatSum / greenCount else 0f
        val rgbSum = (totalR + totalG + totalB).toFloat()
        val greenDominance = if (rgbSum > 0f) totalG.toFloat() / rgbSum else 0f

        android.util.Log.d(TAG, "Color pre-check: greenRatio=${"%.3f".format(greenRatio)} " +
            "($greenCount/$sampledCount, need>=$MIN_GREEN_RATIO), " +
            "avgGreenSat=${"%.3f".format(avgGreenSat)} (need>=$MIN_GREEN_SATURATION), " +
            "greenDominance=${"%.3f".format(greenDominance)} (need>=$MIN_GREEN_DOMINANCE)")

        if (greenRatio < MIN_GREEN_RATIO) {
            android.util.Log.d(TAG, "→ FAIL: not enough green pixels")
            return false
        }
        if (avgGreenSat < MIN_GREEN_SATURATION) {
            android.util.Log.d(TAG, "→ FAIL: green pixels are not saturated enough (screen/artificial light?)")
            return false
        }
        if (greenDominance < MIN_GREEN_DOMINANCE) {
            android.util.Log.d(TAG, "→ FAIL: green channel not dominant overall")
            return false
        }

        android.util.Log.d(TAG, "→ PASS: image looks plant-like")
        return true
    }
}
