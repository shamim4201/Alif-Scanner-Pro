package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ScanItem
import com.example.model.ScanType
import com.example.scanner.DocumentProcessor
import com.example.scanner.PdfManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfToolboxScreen(
    pdfItems: List<ScanItem>,
    onSaveNewPdf: (ScanItem) -> Unit,
    onDeletePdf: (ScanItem) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isProcessing by remember { mutableStateOf(false) }
    var processingMsg by remember { mutableStateOf("") }

    // Compress Dialog
    var showCompressDialog by remember { mutableStateOf(false) }
    var selectedPdfToCompress by remember { mutableStateOf<ScanItem?>(null) }
    var compressQuality by remember { mutableFloatStateOf(60f) }

    // Multi-Image to PDF launcher
    val multipleImagesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            isProcessing = true
            processingMsg = "Converting ${uris.size} images to PDF..."
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val bitmaps = uris.mapNotNull { uri ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            val src = ImageDecoder.createSource(context.contentResolver, uri)
                            ImageDecoder.decodeBitmap(src) { d, _, _ -> d.isMutableRequired = true }
                        } else {
                            @Suppress("DEPRECATION")
                            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                        }
                    }
                    if (bitmaps.isNotEmpty()) {
                        val pdfFile = PdfManager.createPdfFromBitmaps(
                            context = context,
                            bitmaps = bitmaps,
                            filename = "Converted_Doc_${System.currentTimeMillis() / 1000}"
                        )
                        val thumbFile = DocumentProcessor.saveBitmapToFile(context, bitmaps.first(), "thumb")
                        val item = ScanItem(
                            title = pdfFile.nameWithoutExtension,
                            type = ScanType.PDF_FILE,
                            content = "Created from ${bitmaps.size} image(s)",
                            subType = "CONVERTED_PDF",
                            filePath = pdfFile.absolutePath,
                            thumbnailPath = thumbFile.absolutePath,
                            pageCount = bitmaps.size,
                            fileSizeFormatted = PdfManager.formatFileSize(pdfFile.length())
                        )
                        withContext(Dispatchers.Main) {
                            onSaveNewPdf(item)
                            isProcessing = false
                            Toast.makeText(context, "PDF successfully created!", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isProcessing = false
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
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
                    text = "PDF Productivity Tools",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onClose, modifier = Modifier.testTag("pdf_tools_back_btn")) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(processingMsg, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Quick Actions",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PdfActionCard(
                            title = "Images to PDF",
                            desc = "Combine photos into PDF",
                            icon = Icons.Filled.Image,
                            color = Color(0xFF3A86FF),
                            modifier = Modifier.weight(1f).testTag("pdf_action_images_to_pdf"),
                            onClick = {
                                multipleImagesLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )

                        PdfActionCard(
                            title = "Merge PDFs",
                            desc = "Combine 2+ documents",
                            icon = Icons.Filled.Merge,
                            color = Color(0xFF8338EC),
                            modifier = Modifier.weight(1f).testTag("pdf_action_merge_pdfs"),
                            onClick = {
                                val pdfsWithFiles = pdfItems.filter { it.filePath != null && File(it.filePath).exists() }
                                if (pdfsWithFiles.size < 2) {
                                    Toast.makeText(context, "Scan or import at least 2 PDFs first", Toast.LENGTH_SHORT).show()
                                } else {
                                    isProcessing = true
                                    processingMsg = "Merging selected PDFs..."
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val files = pdfsWithFiles.take(2).map { File(it.filePath!!) }
                                        val merged = PdfManager.mergePdfs(context, files, "Merged_Doc_${System.currentTimeMillis() / 1000}")
                                        val item = ScanItem(
                                            title = merged.nameWithoutExtension,
                                            type = ScanType.PDF_FILE,
                                            content = "Merged 2 documents",
                                            subType = "MERGED_PDF",
                                            filePath = merged.absolutePath,
                                            fileSizeFormatted = PdfManager.formatFileSize(merged.length())
                                        )
                                        withContext(Dispatchers.Main) {
                                            onSaveNewPdf(item)
                                            isProcessing = false
                                            Toast.makeText(context, "PDFs merged successfully!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PdfActionCard(
                            title = "Compress PDF",
                            desc = "Reduce file size",
                            icon = Icons.Filled.Compress,
                            color = Color(0xFFFB5607),
                            modifier = Modifier.weight(1f).testTag("pdf_action_compress_pdf"),
                            onClick = {
                                val first = pdfItems.firstOrNull { it.filePath != null && File(it.filePath).exists() }
                                if (first == null) {
                                    Toast.makeText(context, "No existing PDF documents to compress", Toast.LENGTH_SHORT).show()
                                } else {
                                    selectedPdfToCompress = first
                                    showCompressDialog = true
                                }
                            }
                        )

                        PdfActionCard(
                            title = "Split PDF",
                            desc = "Extract individual pages",
                            icon = Icons.Filled.CallSplit,
                            color = Color(0xFFFF006E),
                            modifier = Modifier.weight(1f).testTag("pdf_action_split_pdf"),
                            onClick = {
                                val first = pdfItems.firstOrNull { it.filePath != null && File(it.filePath).exists() }
                                if (first == null) {
                                    Toast.makeText(context, "No PDF documents found to split", Toast.LENGTH_SHORT).show()
                                } else {
                                    isProcessing = true
                                    processingMsg = "Splitting document into individual pages..."
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val splitFiles = PdfManager.splitPdf(context, File(first.filePath!!), first.title)
                                        splitFiles.forEach { f ->
                                            val splitItem = ScanItem(
                                                title = f.nameWithoutExtension,
                                                type = ScanType.PDF_FILE,
                                                content = "Page extracted from ${first.title}",
                                                subType = "SPLIT_PAGE",
                                                filePath = f.absolutePath,
                                                fileSizeFormatted = PdfManager.formatFileSize(f.length())
                                            )
                                            onSaveNewPdf(splitItem)
                                        }
                                        withContext(Dispatchers.Main) {
                                            isProcessing = false
                                            Toast.makeText(context, "Created ${splitFiles.size} split pages!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Your PDF Documents (${pdfItems.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (pdfItems.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Filled.PictureAsPdf,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("No PDF files yet", fontWeight = FontWeight.Bold)
                                Text(
                                    "Tap 'Images to PDF' above or scan a document to generate PDFs.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(pdfItems) { item ->
                        PdfDocumentRow(
                            item = item,
                            onShare = {
                                item.filePath?.let { p ->
                                    val f = File(p)
                                    if (f.exists()) PdfManager.sharePdf(context, f)
                                }
                            },
                            onPrint = {
                                item.filePath?.let { p ->
                                    val f = File(p)
                                    if (f.exists()) PdfManager.printPdf(context, f)
                                }
                            },
                            onDelete = { onDeletePdf(item) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Compress Quality Dialog
    if (showCompressDialog && selectedPdfToCompress != null) {
        val target = selectedPdfToCompress!!
        AlertDialog(
            onDismissRequest = { showCompressDialog = false },
            title = { Text("Compress PDF") },
            text = {
                Column {
                    Text("Target: ${target.title}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Current size: ${target.fileSizeFormatted}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Compression Level: ${compressQuality.toInt()}%", fontSize = 13.sp)
                    Slider(
                        value = compressQuality,
                        onValueChange = { compressQuality = it },
                        valueRange = 25f..85f,
                        steps = 5
                    )
                    Text(
                        text = if (compressQuality < 50) "High compression (smaller file, lower resolution)" else "Balanced compression (good quality & size)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCompressDialog = false
                        isProcessing = true
                        processingMsg = "Compressing ${target.title}..."
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val compressedFile = PdfManager.compressPdf(
                                    context = context,
                                    inputFile = File(target.filePath!!),
                                    outputTitle = "${target.title}_compressed",
                                    compressionQuality = compressQuality.toInt()
                                )
                                val newItem = ScanItem(
                                    title = compressedFile.nameWithoutExtension,
                                    type = ScanType.PDF_FILE,
                                    content = "Compressed version of ${target.title}",
                                    subType = "COMPRESSED_PDF",
                                    filePath = compressedFile.absolutePath,
                                    fileSizeFormatted = PdfManager.formatFileSize(compressedFile.length())
                                )
                                withContext(Dispatchers.Main) {
                                    onSaveNewPdf(newItem)
                                    isProcessing = false
                                    Toast.makeText(context, "Compressed to ${newItem.fileSizeFormatted}!", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                    Toast.makeText(context, "Compression error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                ) {
                    Text("Compress Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCompressDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PdfActionCard(
    title: String,
    desc: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(115.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }

            Column {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(text = desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun PdfDocumentRow(
    item: ScanItem,
    onShare: () -> Unit,
    onPrint: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pdf_row_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0xFFE63946).copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.PictureAsPdf,
                        contentDescription = null,
                        tint = Color(0xFFE63946),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = item.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${item.fileSizeFormatted} • ${item.pageCount} page(s)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row {
                IconButton(onClick = onShare) {
                    Icon(Icons.Filled.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onPrint) {
                    Icon(Icons.Filled.Print, contentDescription = "Print", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
