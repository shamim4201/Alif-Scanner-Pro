package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.Checkbox
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
    BackHandler {
        onClose()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isProcessing by remember { mutableStateOf(false) }
    var processingMsg by remember { mutableStateOf("") }

    // Compress Dialog
    var showCompressDialog by remember { mutableStateOf(false) }
    var selectedPdfToCompress by remember { mutableStateOf<ScanItem?>(null) }
    var compressQuality by remember { mutableFloatStateOf(60f) }

    // Merge Selection Dialog
    var showMergePickerDialog by remember { mutableStateOf(false) }
    var selectedMergeIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    // Split / Compress Picker Dialog when multiple PDFs exist
    var pickerMode by remember { mutableStateOf<String?>(null) } // "split" or "compress"

    fun runMerge(itemsToMerge: List<ScanItem>) {
        isProcessing = true
        processingMsg = "Merging ${itemsToMerge.size} PDFs..."
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val files = itemsToMerge.mapNotNull { it.filePath?.let { p -> File(p) } }.filter { it.exists() }
                val merged = PdfManager.mergePdfs(context, files, "Merged_Doc_${System.currentTimeMillis() / 1000}")
                val totalPages = itemsToMerge.sumOf { it.pageCount.coerceAtLeast(1) }
                val item = ScanItem(
                    title = merged.nameWithoutExtension,
                    type = ScanType.PDF_FILE,
                    content = "Merged ${files.size} documents",
                    subType = "MERGED_PDF",
                    filePath = merged.absolutePath,
                    pageCount = totalPages,
                    fileSizeFormatted = PdfManager.formatFileSize(merged.length())
                )
                withContext(Dispatchers.Main) {
                    onSaveNewPdf(item)
                    Toast.makeText(context, "PDFs merged successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Merge failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isProcessing = false
                }
            }
        }
    }

    fun runSplit(target: ScanItem) {
        isProcessing = true
        processingMsg = "Splitting ${target.title}..."
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val splitFiles = PdfManager.splitPdf(context, File(target.filePath!!), target.title)
                val newItems = splitFiles.map { f ->
                    ScanItem(
                        title = f.nameWithoutExtension,
                        type = ScanType.PDF_FILE,
                        content = "Page extracted from ${target.title}",
                        subType = "SPLIT_PAGE",
                        filePath = f.absolutePath,
                        pageCount = 1,
                        fileSizeFormatted = PdfManager.formatFileSize(f.length())
                    )
                }
                withContext(Dispatchers.Main) {
                    newItems.forEach { onSaveNewPdf(it) }
                    Toast.makeText(context, "Created ${splitFiles.size} split page(s)!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Split failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    isProcessing = false
                }
            }
        }
    }

    // Multi-Image to PDF launcher
    val multipleImagesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            isProcessing = true
            processingMsg = "Converting ${uris.size} image(s) to PDF..."
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val bitmaps = uris.mapNotNull { uri ->
                        DocumentProcessor.decodeSampledBitmapFromUri(context, uri, maxDimension = 1600)
                    }
                    if (bitmaps.isNotEmpty()) {
                        val pdfFile = PdfManager.createPdfFromBitmaps(
                            context = context,
                            bitmaps = bitmaps,
                            filename = "Converted_Doc_${System.currentTimeMillis() / 1000}"
                        )
                        val thumbBmp = DocumentProcessor.createThumbnail(bitmaps.first(), 300)
                        val thumbFile = DocumentProcessor.saveBitmapToFile(context, thumbBmp, "thumb")
                        if (thumbBmp !== bitmaps.first()) thumbBmp.recycle()

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
                            Toast.makeText(context, "PDF successfully created!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Could not decode selected images", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    withContext(Dispatchers.Main) {
                        isProcessing = false
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                                when {
                                    pdfsWithFiles.size < 2 -> {
                                        Toast.makeText(context, "Scan or import at least 2 PDFs first", Toast.LENGTH_SHORT).show()
                                    }
                                    pdfsWithFiles.size == 2 -> {
                                        runMerge(pdfsWithFiles)
                                    }
                                    else -> {
                                        selectedMergeIds = pdfsWithFiles.take(2).map { it.id }.toSet()
                                        showMergePickerDialog = true
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
                                val pdfsWithFiles = pdfItems.filter { it.filePath != null && File(it.filePath).exists() }
                                when {
                                    pdfsWithFiles.isEmpty() -> {
                                        Toast.makeText(context, "No existing PDF documents to compress", Toast.LENGTH_SHORT).show()
                                    }
                                    pdfsWithFiles.size == 1 -> {
                                        selectedPdfToCompress = pdfsWithFiles.first()
                                        showCompressDialog = true
                                    }
                                    else -> {
                                        pickerMode = "compress"
                                    }
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
                                val pdfsWithFiles = pdfItems.filter { it.filePath != null && File(it.filePath).exists() }
                                when {
                                    pdfsWithFiles.isEmpty() -> {
                                        Toast.makeText(context, "No PDF documents found to split", Toast.LENGTH_SHORT).show()
                                    }
                                    pdfsWithFiles.size == 1 -> {
                                        runSplit(pdfsWithFiles.first())
                                    }
                                    else -> {
                                        pickerMode = "split"
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
                                    pageCount = target.pageCount.coerceAtLeast(1),
                                    fileSizeFormatted = PdfManager.formatFileSize(compressedFile.length())
                                )
                                withContext(Dispatchers.Main) {
                                    onSaveNewPdf(newItem)
                                    Toast.makeText(context, "Compressed to ${newItem.fileSizeFormatted}!", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Compression error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            } finally {
                                withContext(Dispatchers.Main) {
                                    isProcessing = false
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

    // Merge PDF Picker Dialog (when 3+ PDFs exist)
    if (showMergePickerDialog) {
        val availablePdfs = pdfItems.filter { it.filePath != null && File(it.filePath).exists() }
        AlertDialog(
            onDismissRequest = { showMergePickerDialog = false },
            title = { Text("Select PDFs to Merge") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    availablePdfs.forEach { pdf ->
                        val checked = selectedMergeIds.contains(pdf.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedMergeIds = if (checked) {
                                        selectedMergeIds - pdf.id
                                    } else {
                                        selectedMergeIds + pdf.id
                                    }
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    selectedMergeIds = if (isChecked) {
                                        selectedMergeIds + pdf.id
                                    } else {
                                        selectedMergeIds - pdf.id
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(pdf.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(pdf.fileSizeFormatted, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = selectedMergeIds.size >= 2,
                    onClick = {
                        val chosen = availablePdfs.filter { selectedMergeIds.contains(it.id) }
                        showMergePickerDialog = false
                        if (chosen.size >= 2) runMerge(chosen)
                    }
                ) {
                    Text("Merge (${selectedMergeIds.size})")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMergePickerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Single PDF Picker Dialog for Split / Compress (when 2+ PDFs exist)
    if (pickerMode != null) {
        val mode = pickerMode!!
        val availablePdfs = pdfItems.filter { it.filePath != null && File(it.filePath).exists() }
        AlertDialog(
            onDismissRequest = { pickerMode = null },
            title = { Text(if (mode == "split") "Choose PDF to Split" else "Choose PDF to Compress") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    availablePdfs.forEach { pdf ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    pickerMode = null
                                    if (mode == "split") {
                                        runSplit(pdf)
                                    } else {
                                        selectedPdfToCompress = pdf
                                        showCompressDialog = true
                                    }
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = Color(0xFFE63946))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(pdf.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("${pdf.fileSizeFormatted} • ${pdf.pageCount} page(s)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { pickerMode = null }) {
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
