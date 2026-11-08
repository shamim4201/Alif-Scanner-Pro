package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scanner.PdfManager
import com.example.scanner.QrBarcodeParser
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrScreen(
    initialText: String,
    isLoading: Boolean,
    onSaveOcr: (title: String, text: String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
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
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
                            // Export as TXT file
                            try {
                                val storageDir = File(context.filesDir, "exports").apply { if (!exists()) mkdirs() }
                                val txtFile = File(storageDir, "ocr_note_${System.currentTimeMillis() / 1000}.txt")
                                txtFile.writeText(editableText)
                                QrBarcodeParser.shareText(context, editableText, "Share TXT Note")
                                Toast.makeText(context, "Exported: ${txtFile.name}", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                e.printStackTrace()
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
                            // Quick text to PDF conversion
                            try {
                                val bmp = android.graphics.Bitmap.createBitmap(595, 842, android.graphics.Bitmap.Config.ARGB_8888)
                                val canvas = android.graphics.Canvas(bmp)
                                canvas.drawColor(android.graphics.Color.WHITE)
                                val paint = android.graphics.Paint().apply {
                                    color = android.graphics.Color.BLACK
                                    textSize = 14f
                                    isAntiAlias = true
                                }
                                val lines = editableText.split("\n")
                                var y = 40f
                                for (line in lines.take(45)) {
                                    canvas.drawText(line.take(70), 30f, y, paint)
                                    y += 18f
                                }
                                val pdfFile = PdfManager.createPdfFromBitmaps(
                                    context,
                                    listOf(bmp),
                                    "OCR_Note_${System.currentTimeMillis() / 1000}"
                                )
                                PdfManager.sharePdf(context, pdfFile)
                            } catch (e: Exception) {
                                e.printStackTrace()
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
