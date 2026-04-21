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
 *   1. Green pixel ratio    ≥ 10%  — at least 1/10 of sampled pixels are green-hued
 *   2. Green saturation     ≥ 20%  — those green pixels have vivid color (not washed out)
 *   3. Combined plant ratio ≥ 25%  — green + orange/brown diseased tissue pixels combined
 *   4. Green dominance      ≥ 27%  — green channel is reasonably strong overall
 */
class CornLeafDetector(private val context: Context) {

    companion object {
        private const val TAG = "CornLeafDetector"
        private const val INPUT_SIZE = 224

        // ── Color pre-check constants ──
        // Restored to original values — prevents non-plant images from passing the color gate.
        private const val MIN_GREEN_RATIO          = 0.10f
        private const val MIN_GREEN_SATURATION     = 0.20f
        private const val MIN_GREEN_DOMINANCE      = 0.27f

        // Diseased/necrotic leaf tissue (rust, blight) — orange-brown hues
        private const val MIN_PLANT_COMBINED_RATIO = 0.25f
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
     * Checks:
     * 1. Green pixel ratio: ≥10% of sampled pixels are in the green/yellow-green hue range
     * 2. Green saturation quality: average saturation of green pixels ≥20%
     * 3. Combined plant ratio: green + orange/brown diseased pixels ≥25%
     * 4. Green channel dominance: G/(R+G+B) ≥ 0.27
     * 5. Green spatial spread: plant pixels must appear in at least 2 of 4 quadrants
     *    (prevents a single green sticker/spot from passing the check)
     *
     * Uses every 4th pixel for speed (~12,500 samples on 224×224).
     */
    private fun hasPlantLikeColors(pixels: IntArray): Boolean {
        val hsv = FloatArray(3)
        var greenCount = 0
        var diseasedCount = 0
        var greenSatSum = 0f
        var sampledCount = 0
        var totalR = 0L
        var totalG = 0L
        var totalB = 0L

        // Quadrant plant pixel counts (TL, TR, BL, BR)
        val quadrantPlant = IntArray(4)
        val quadrantTotal = IntArray(4)
        val half = INPUT_SIZE / 2

        for (i in pixels.indices step 4) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            totalR += r
            totalG += g
            totalB += b
            sampledCount++

            // Determine quadrant from pixel index
            val pixelIdx = i / 4  // actual pixel position (step 4 sampling)
            val row = (pixelIdx / INPUT_SIZE)
            val col = (pixelIdx % INPUT_SIZE)
            val quadrant = (if (row >= half) 2 else 0) + (if (col >= half) 1 else 0)
            quadrantTotal[quadrant]++

            android.graphics.Color.RGBToHSV(r, g, b, hsv)

            val isGreen    = hsv[0] in 25f..165f && hsv[1] >= 0.20f && hsv[2] >= 0.10f
            val isDiseased = hsv[0] in 8f..45f   && hsv[1] >= 0.25f && hsv[2] >= 0.15f

            if (isGreen) {
                greenCount++
                greenSatSum += hsv[1]
                quadrantPlant[quadrant]++
            } else if (isDiseased) {
                diseasedCount++
                quadrantPlant[quadrant]++
            }
        }

        if (sampledCount == 0) return false

        val greenRatio     = greenCount.toFloat() / sampledCount
        val combinedRatio  = (greenCount + diseasedCount).toFloat() / sampledCount
        val avgGreenSat    = if (greenCount > 0) greenSatSum / greenCount else 0f
        val rgbSum         = (totalR + totalG + totalB).toFloat()
        val greenDominance = if (rgbSum > 0f) totalG.toFloat() / rgbSum else 0f

        // Count how many quadrants have ≥5% plant pixels
        val activeQuadrants = quadrantPlant.indices.count { q ->
            quadrantTotal[q] > 0 && quadrantPlant[q].toFloat() / quadrantTotal[q] >= 0.05f
        }

        android.util.Log.d(TAG, "Color pre-check: greenRatio=${"%.3f".format(greenRatio)} " +
            "combinedRatio=${"%.3f".format(combinedRatio)} " +
            "avgGreenSat=${"%.3f".format(avgGreenSat)} " +
            "greenDominance=${"%.3f".format(greenDominance)} " +
            "activeQuadrants=$activeQuadrants/4")

        if (greenRatio < MIN_GREEN_RATIO) {
            android.util.Log.d(TAG, "→ FAIL: not enough green pixels")
            return false
        }
        if (avgGreenSat < MIN_GREEN_SATURATION) {
            android.util.Log.d(TAG, "→ FAIL: green pixels not saturated enough")
            return false
        }
        if (combinedRatio < MIN_PLANT_COMBINED_RATIO) {
            android.util.Log.d(TAG, "→ FAIL: not enough plant-like pixels (green + diseased)")
            return false
        }
        if (greenDominance < MIN_GREEN_DOMINANCE) {
            android.util.Log.d(TAG, "→ FAIL: green channel not dominant enough overall")
            return false
        }
        if (activeQuadrants < 1) {
            android.util.Log.d(TAG, "→ FAIL: no plant pixels found in any quadrant ($activeQuadrants/4)")
            return false
        }

        android.util.Log.d(TAG, "→ PASS: image looks plant-like")
        return true
    }
}
