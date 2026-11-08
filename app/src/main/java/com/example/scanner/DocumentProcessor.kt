package com.example.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

enum class DocumentFilter {
    COLOR,
    MAGIC_COLOR,
    BLACK_AND_WHITE,
    GRAYSCALE
}

data class ScannedPage(
    val id: String = UUID.randomUUID().toString(),
    val originalBitmap: Bitmap,
    var processedBitmap: Bitmap,
    var rotation: Int = 0,
    var filter: DocumentFilter = DocumentFilter.MAGIC_COLOR,
    var cropLeft: Float = 0f,
    var cropTop: Float = 0f,
    var cropRight: Float = 1f,
    var cropBottom: Float = 1f
)

object DocumentProcessor {

    fun applyFilterAndTransform(
        source: Bitmap,
        filter: DocumentFilter,
        rotationDegrees: Int = 0,
        cropRect: Rect? = null
    ): Bitmap {
        var bmp = source

        // Apply crop if provided
        if (cropRect != null && cropRect.width() > 10 && cropRect.height() > 10) {
            val safeLeft = cropRect.left.coerceIn(0, bmp.width - 1)
            val safeTop = cropRect.top.coerceIn(0, bmp.height - 1)
            val safeWidth = cropRect.width().coerceAtMost(bmp.width - safeLeft)
            val safeHeight = cropRect.height().coerceAtMost(bmp.height - safeTop)
            if (safeWidth > 0 && safeHeight > 0) {
                bmp = Bitmap.createBitmap(bmp, safeLeft, safeTop, safeWidth, safeHeight)
            }
        }

        // Apply rotation if needed
        if (rotationDegrees % 360 != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
        }

        return when (filter) {
            DocumentFilter.COLOR -> bmp
            DocumentFilter.MAGIC_COLOR -> applyMagicColor(bmp)
            DocumentFilter.BLACK_AND_WHITE -> applyBlackAndWhite(bmp)
            DocumentFilter.GRAYSCALE -> applyGrayscaleShadowRemoval(bmp)
        }
    }

    private fun applyMagicColor(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()

        // Contrast: 1.35x, Brightness: +15, Saturation: 1.15
        val cmContrast = ColorMatrix().apply {
            val scale = 1.35f
            val translate = 15f
            set(floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        val cmSat = ColorMatrix().apply { setSaturation(1.15f) }
        cmContrast.postConcat(cmSat)

        paint.colorFilter = ColorMatrixColorFilter(cmContrast)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    private fun applyGrayscaleShadowRemoval(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()

        val cm = ColorMatrix().apply {
            setSaturation(0f)
            // Boost brightness to remove background shadows
            val scale = 1.25f
            val translate = 25f
            val boost = ColorMatrix(floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
            postConcat(boost)
        }
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    private fun applyBlackAndWhite(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        // Calculate average luminance for adaptive thresholding
        var totalLum = 0L
        val step = 4 // sample every 4th pixel for speed
        var count = 0
        for (i in pixels.indices step step) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            totalLum += (r * 299 + g * 587 + b * 114) / 1000
            count++
        }
        val threshold = if (count > 0) ((totalLum / count) * 0.95f).toInt().coerceIn(80, 180) else 128

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val lum = (r * 299 + g * 587 + b * 114) / 1000
            pixels[i] = if (lum > threshold) Color.WHITE else Color.BLACK
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }

    fun saveBitmapToFile(context: Context, bitmap: Bitmap, prefix: String = "scan_page"): File {
        val storageDir = File(context.filesDir, "scans").apply { if (!exists()) mkdirs() }
        val file = File(storageDir, "${prefix}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return file
    }

    fun createThumbnail(bitmap: Bitmap, maxDimension: Int = 300): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val scale = maxDimension.toFloat() / maxOf(width, height)
        val newWidth = (width * scale).toInt().coerceAtLeast(1)
        val newHeight = (height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    fun mergeIdCard(front: Bitmap, back: Bitmap): Bitmap {
        val canvasWidth = 1240
        val canvasHeight = 1754
        val result = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            textSize = 28f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 36f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }

        // Header Title
        canvas.drawText("ID CARD & BADGE SCAN", canvasWidth / 2f, 90f, headerPaint)

        val cardWidth = 980
        val cardHeight = 620
        val cardLeft = ((canvasWidth - cardWidth) / 2).toFloat()
        val frontTop = 160f
        val backTop = 930f

        // Draw Front label & outline
        canvas.drawText("— FRONT SIDE —", canvasWidth / 2f, frontTop - 18f, textPaint)
        val frontRect = android.graphics.RectF(cardLeft, frontTop, cardLeft + cardWidth, frontTop + cardHeight)
        canvas.drawRoundRect(frontRect, 16f, 16f, borderPaint)

        val scaledFront = Bitmap.createScaledBitmap(front, cardWidth - 8, cardHeight - 8, true)
        canvas.drawBitmap(scaledFront, cardLeft + 4, frontTop + 4, paint)

        // Draw Back label & outline
        canvas.drawText("— BACK SIDE —", canvasWidth / 2f, backTop - 18f, textPaint)
        val backRect = android.graphics.RectF(cardLeft, backTop, cardLeft + cardWidth, backTop + cardHeight)
        canvas.drawRoundRect(backRect, 16f, 16f, borderPaint)

        val scaledBack = Bitmap.createScaledBitmap(back, cardWidth - 8, cardHeight - 8, true)
        canvas.drawBitmap(scaledBack, cardLeft + 4, backTop + 4, paint)

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.GRAY
            textSize = 22f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Scanned with Alif Scanner Pro", canvasWidth / 2f, canvasHeight - 50f, footerPaint)

        return result
    }
}
