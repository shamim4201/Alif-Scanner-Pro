package com.example.scanner

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.PrintManager
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale

object PdfManager {

    // Standard A4 dimensions in points (72 points per inch)
    const val PAGE_WIDTH_A4 = 595
    const val PAGE_HEIGHT_A4 = 842

    const val PAGE_WIDTH_LETTER = 612
    const val PAGE_HEIGHT_LETTER = 792

    fun createPdfFromBitmaps(
        context: Context,
        bitmaps: List<Bitmap>,
        filename: String,
        pageSize: String = "A4",
        compressionQuality: Int = 85
    ): File {
        val storageDir = File(context.filesDir, "pdfs").apply { if (!exists()) mkdirs() }
        val safeName = filename.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").let {
            if (it.endsWith(".pdf", ignoreCase = true)) it else "$it.pdf"
        }
        val outputFile = File(storageDir, safeName)

        val docWidth = if (pageSize.equals("Letter", ignoreCase = true)) PAGE_WIDTH_LETTER else PAGE_WIDTH_A4
        val docHeight = if (pageSize.equals("Letter", ignoreCase = true)) PAGE_HEIGHT_LETTER else PAGE_HEIGHT_A4

        val pdfDocument = PdfDocument()

        bitmaps.forEachIndexed { index, rawBmp ->
            val pageInfo = PdfDocument.PageInfo.Builder(docWidth, docHeight, index + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            // Margin of 24 points
            val margin = 24f
            val printableWidth = docWidth - (margin * 2)
            val printableHeight = docHeight - (margin * 2)

            // Compress / scale bitmap if requested
            val scaledBmp = if (compressionQuality < 80) {
                val scale = (compressionQuality / 100f).coerceAtLeast(0.5f)
                Bitmap.createScaledBitmap(
                    rawBmp,
                    (rawBmp.width * scale).toInt().coerceAtLeast(10),
                    (rawBmp.height * scale).toInt().coerceAtLeast(10),
                    true
                )
            } else {
                rawBmp
            }

            val bmpAspect = scaledBmp.width.toFloat() / scaledBmp.height.toFloat()
            val pageAspect = printableWidth / printableHeight

            val destRect = if (bmpAspect > pageAspect) {
                // Width constrained
                val h = printableWidth / bmpAspect
                val top = margin + (printableHeight - h) / 2f
                RectF(margin, top, margin + printableWidth, top + h)
            } else {
                // Height constrained
                val w = printableHeight * bmpAspect
                val left = margin + (printableWidth - w) / 2f
                RectF(left, margin, left + w, margin + printableHeight)
            }

            val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
            canvas.drawBitmap(scaledBmp, null, destRect, paint)

            pdfDocument.finishPage(page)
        }

        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return outputFile
    }

    fun renderPdfPagesToBitmaps(context: Context, pdfFile: File): List<Bitmap> {
        val result = mutableListOf<Bitmap>()
        if (!pdfFile.exists()) return result

        try {
            val fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(fileDescriptor)
            val pageCount = pdfRenderer.pageCount

            for (i in 0 until pageCount) {
                val page = pdfRenderer.openPage(i)
                // Render at high resolution (1.5x of points for clarity)
                val width = (page.width * 1.5f).toInt().coerceIn(400, 2000)
                val height = (page.height * 1.5f).toInt().coerceIn(600, 3000)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                result.add(bitmap)
            }
            pdfRenderer.close()
            fileDescriptor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    fun mergePdfs(context: Context, pdfFiles: List<File>, outputTitle: String): File {
        val allBitmaps = mutableListOf<Bitmap>()
        for (file in pdfFiles) {
            val pages = renderPdfPagesToBitmaps(context, file)
            allBitmaps.addAll(pages)
        }
        return createPdfFromBitmaps(context, allBitmaps, outputTitle)
    }

    fun splitPdf(context: Context, inputFile: File, outputTitlePrefix: String): List<File> {
        val pages = renderPdfPagesToBitmaps(context, inputFile)
        val createdFiles = mutableListOf<File>()
        pages.forEachIndexed { index, bitmap ->
            val pageFile = createPdfFromBitmaps(
                context,
                listOf(bitmap),
                "${outputTitlePrefix}_page_${index + 1}.pdf"
            )
            createdFiles.add(pageFile)
        }
        return createdFiles
    }

    fun compressPdf(context: Context, inputFile: File, outputTitle: String, compressionQuality: Int = 60): File {
        val pages = renderPdfPagesToBitmaps(context, inputFile)
        return createPdfFromBitmaps(context, pages, outputTitle, compressionQuality = compressionQuality)
    }

    fun sharePdf(context: Context, pdfFile: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Share PDF via...").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun printPdf(context: Context, pdfFile: File) {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
            val printAdapter = object : android.print.PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes?,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: android.os.Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }
                    val info = android.print.PrintDocumentInfo.Builder(pdfFile.name)
                        .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .build()
                    callback?.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out android.print.PageRange>?,
                    destination: ParcelFileDescriptor?,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    try {
                        val input = FileInputStream(pdfFile)
                        val output = FileOutputStream(destination?.fileDescriptor)
                        input.copyTo(output)
                        input.close()
                        output.close()
                        callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.message)
                    }
                }
            }
            printManager.print("Alif Scanner Pro - ${pdfFile.name}", printAdapter, PrintAttributes.Builder().build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024f)
            else -> String.format(Locale.US, "%.2f MB", bytes / (1024f * 1024f))
        }
    }
}
