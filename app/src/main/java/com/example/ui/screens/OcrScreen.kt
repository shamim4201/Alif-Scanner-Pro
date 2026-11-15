package com.example.ui.screens

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.scanner.PdfManager
import com.example.scanner.QrBarcodeParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrScreen(
    initialText: String,
    isLoading: Boolean,
    onSaveOcr: (title: String, text: String) -> Unit,
    onClose: () -> Unit
) {
    BackHandler {
        onClose()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var editableText by remember(initialText) { mutableStateOf(initialText) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val wordCount = remember(editableText) {
        editableText.split(Regex("\\s+")).filter { it.isNotBlank() }.size
    }
    val charCount = editableText.length

    val searchMatches = remember(editableText, searchQuery) {
        if (searchQuery.isBlank()) 0
        else {
            Regex.escape(searchQuery).toRegex(RegexOption.IGNORE_CASE).findAll(editableText).count()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Extracted Text (OCR)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onClose, modifier = Modifier.testTag("ocr_btn_back")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { isSearchActive = !isSearchActive }) {
                    Icon(Icons.Filled.Search, contentDescription = "Search text")
                }
                IconButton(
                    onClick = {
                        onSaveOcr("OCR Note ${System.currentTimeMillis() / 1000}", editableText)
                        Toast.makeText(context, "Saved to Scan History", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("ocr_btn_save")
                ) {
                    Icon(Icons.Filled.Save, contentDescription = "Save")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        // In-text search field
        if (isSearchActive) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search words in document...") },
                        modifier = Modifier.weight(1f).testTag("ocr_search_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (searchQuery.isNotBlank()) {
                        Text(
                            text = "$searchMatches found",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { isSearchActive = false; searchQuery = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close search")
                    }
                }
            }
        }

        // Stats Bar (Characters, Words)
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$charCount characters • $wordCount words",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Tap text to edit",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Extracting text with ML OCR engine...", fontSize = 14.sp)
                }
            }
        } else {
            // Editable Text Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = editableText,
                    onValueChange = { editableText = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("ocr_text_content_field"),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("No text detected. You can type or paste text here.") }
                )
            }
        }

        // Bottom Action Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            QrBarcodeParser.copyToClipboard(context, "OCR Text", editableText)
                        },
                        modifier = Modifier.weight(1f).testTag("ocr_copy_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Text")
                    }

                    OutlinedButton(
                        onClick = {
                            QrBarcodeParser.shareText(context, editableText, "Share OCR Text")
                        },
                        modifier = Modifier.weight(1f).testTag("ocr_share_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (editableText.isBlank()) {
                                Toast.makeText(context, "No text to export", Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val storageDir = File(context.filesDir, "exports").apply { if (!exists()) mkdirs() }
                                    val txtFile = File(storageDir, "ocr_note_${System.currentTimeMillis() / 1000}.txt")
                                    txtFile.writeText(editableText)
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        txtFile
                                    )
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_TEXT, editableText)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    val chooser = Intent.createChooser(sendIntent, "Share TXT Note").apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    withContext(Dispatchers.Main) {
                                        context.startActivity(chooser)
                                        Toast.makeText(context, "Exported: ${txtFile.name}", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("ocr_export_txt_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export TXT")
                    }

                    OutlinedButton(
                        onClick = {
                            if (editableText.isBlank()) {
                                Toast.makeText(context, "No text to export", Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    val pdfFile = createMultiPageTextPdf(context, editableText)
                                    withContext(Dispatchers.Main) {
                                        PdfManager.sharePdf(context, pdfFile)
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "PDF export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("ocr_export_pdf_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export PDF")
                    }
                }
            }
        }
    }
}

private fun createMultiPageTextPdf(context: android.content.Context, text: String): File {
    val storageDir = File(context.filesDir, "pdfs").apply { if (!exists()) mkdirs() }
    val outputFile = File(storageDir, "OCR_Note_${System.currentTimeMillis() / 1000}.pdf")

    val pageWidth = PdfManager.PAGE_WIDTH_A4
    val pageHeight = PdfManager.PAGE_HEIGHT_A4
    val margin = 40f
    val contentWidth = (pageWidth - margin * 2).toInt()
    val maxContentHeight = pageHeight - margin * 2

    val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 12f
    }

    val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        StaticLayout.Builder.obtain(text, 0, text.length, textPaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(4f, 1.15f)
            .setIncludePad(false)
            .build()
    } else {
        @Suppress("DEPRECATION")
        StaticLayout(text, textPaint, contentWidth, Layout.Alignment.ALIGN_NORMAL, 1.15f, 4f, false)
    }

    val pdfDocument = PdfDocument()
    var pageNumber = 1
    var startLine = 0

    while (startLine < staticLayout.lineCount) {
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        canvas.drawColor(Color.WHITE)

        val startTop = staticLayout.getLineTop(startLine)
        var endLine = startLine
        while (endLine < staticLayout.lineCount &&
            (staticLayout.getLineBottom(endLine) - startTop) <= maxContentHeight
        ) {
            endLine++
        }
        if (endLine == startLine) endLine++

        canvas.save()
        canvas.translate(margin, margin - startTop)
        canvas.clipRect(0f, startTop.toFloat(), contentWidth.toFloat(), staticLayout.getLineBottom(endLine - 1).toFloat())
        staticLayout.draw(canvas)
        canvas.restore()

        pdfDocument.finishPage(page)
        startLine = endLine
        pageNumber++
    }

    FileOutputStream(outputFile).use { out ->
        pdfDocument.writeTo(out)
    }
    pdfDocument.close()
    return outputFile
}
