package com.example.ui.screens

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ScanItem
import com.example.model.ScanType
import com.example.scanner.PdfManager
import com.example.scanner.QrBarcodeGenerator
import com.example.scanner.QrBarcodeParser
import com.example.ui.components.PinLockScreen
import com.example.ui.theme.ScannerCyanAccent
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    scans: List<ScanItem>,
    privateScans: List<ScanItem>,
    folders: List<String>,
    searchQuery: String,
    selectedFilterType: ScanType?,
    selectedFolder: String?,
    sortByNewest: Boolean,
    isPrivateFolderUnlocked: Boolean,
    onSearchChanged: (String) -> Unit,
    onFilterTypeChanged: (ScanType?) -> Unit,
    onFolderChanged: (String?) -> Unit,
    onToggleSortOrder: () -> Unit,
    onUpdateTitle: (id: Long, title: String) -> Unit,
    onMoveFolder: (id: Long, folder: String) -> Unit,
    onTogglePrivate: (id: Long, isPrivate: Boolean) -> Unit,
    onDeleteScan: (ScanItem) -> Unit,
    onClearAll: () -> Unit,
    onUnlockPrivateFolder: (pin: String) -> Boolean,
    onLockPrivateFolder: () -> Unit
) {
    val context = LocalContext.current
    var isPrivateTabSelected by remember { mutableStateOf(false) }
    var selectedItemForDetail by remember { mutableStateOf<ScanItem?>(null) }
    var itemToRename by remember { mutableStateOf<ScanItem?>(null) }
    var itemToMove by remember { mutableStateOf<ScanItem?>(null) }
    var showClearAllConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = if (isPrivateTabSelected) "Private Vault" else "Scan History",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                // Toggle Sort
                IconButton(onClick = onToggleSortOrder, modifier = Modifier.testTag("history_sort_btn")) {
                    Icon(
                        imageVector = if (sortByNewest) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                        contentDescription = "Sort order"
                    )
                }

                // Private Vault Toggle
                IconButton(
                    onClick = {
                        isPrivateTabSelected = !isPrivateTabSelected
                    },
                    modifier = Modifier.testTag("history_vault_toggle_btn")
                ) {
                    Icon(
                        imageVector = if (isPrivateTabSelected) Icons.Filled.LockOpen else Icons.Filled.Lock,
                        contentDescription = "Private Vault",
                        tint = if (isPrivateTabSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }

                if (!isPrivateTabSelected && scans.isNotEmpty()) {
                    IconButton(onClick = { showClearAllConfirm = true }, modifier = Modifier.testTag("history_clear_all_btn")) {
                        Icon(Icons.Filled.Delete, contentDescription = "Clear all", tint = MaterialTheme.colorScheme.error)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        if (isPrivateTabSelected && !isPrivateFolderUnlocked) {
            // Private Folder Locked Screen
            PinLockScreen(
                title = "Unlock Private Vault",
                subtitle = "Enter PIN to access your password-protected documents",
                onPinEntered = { pin ->
                    onUnlockPrivateFolder(pin)
                },
                onCancel = {
                    isPrivateTabSelected = false
                }
            )
        } else {
            val displayScans = if (isPrivateTabSelected) privateScans else scans

            // Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChanged,
                    placeholder = { Text("Search by title or text...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChanged("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_search_input"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
            }

            // Filter Tabs Carousel (All, QR, Barcode, Document, OCR, PDF)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedFilterType == null,
                        onClick = { onFilterTypeChanged(null) },
                        label = { Text("All (${displayScans.size})") },
                        modifier = Modifier.testTag("history_chip_all")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilterType == ScanType.QR_CODE,
                        onClick = { onFilterTypeChanged(ScanType.QR_CODE) },
                        label = { Text("QR Codes") },
                        leadingIcon = { Icon(Icons.Filled.QrCode, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("history_chip_qr")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilterType == ScanType.BARCODE,
                        onClick = { onFilterTypeChanged(ScanType.BARCODE) },
                        label = { Text("Barcodes") },
                        leadingIcon = { Icon(Icons.Filled.ViewWeek, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("history_chip_barcode")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilterType == ScanType.DOCUMENT,
                        onClick = { onFilterTypeChanged(ScanType.DOCUMENT) },
                        label = { Text("Documents") },
                        modifier = Modifier.testTag("history_chip_doc")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilterType == ScanType.OCR_TEXT,
                        onClick = { onFilterTypeChanged(ScanType.OCR_TEXT) },
                        label = { Text("OCR Notes") },
                        modifier = Modifier.testTag("history_chip_ocr")
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilterType == ScanType.PDF_FILE,
                        onClick = { onFilterTypeChanged(ScanType.PDF_FILE) },
                        label = { Text("PDFs") },
                        leadingIcon = { Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("history_chip_pdf")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Folder Filter Row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedFolder == null,
                        onClick = { onFolderChanged(null) },
                        label = { Text("All Folders") }
                    )
                }
                items(folders) { folder ->
                    FilterChip(
                        selected = selectedFolder == folder,
                        onClick = { onFolderChanged(folder) },
                        label = { Text(folder) },
                        leadingIcon = { Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Items List
            if (displayScans.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isPrivateTabSelected) "Private Vault is empty" else "No matching scans found",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isPrivateTabSelected) "Lock scans into your Private Vault to keep them protected here." else "Scan QR codes, barcodes, or documents to build your history.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(displayScans, key = { it.id }) { item ->
                        ScanHistoryItemCard(
                            item = item,
                            onClick = { selectedItemForDetail = item },
                            onRename = { itemToRename = item },
                            onMoveFolder = { itemToMove = item },
                            onTogglePrivate = { onTogglePrivate(item.id, !item.isPrivate) },
                            onDelete = { onDeleteScan(item) }
                        )
                    }
                }
            }
        }
    }

    // Detail Bottom Sheet
    if (selectedItemForDetail != null) {
        val detailItem = selectedItemForDetail!!
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { selectedItemForDetail = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            DetailScanBottomSheetContent(
                item = detailItem,
                onDismiss = { selectedItemForDetail = null }
            )
        }
    }

    // Rename Dialog
    if (itemToRename != null) {
        val item = itemToRename!!
        var newTitle by remember { mutableStateOf(item.title) }
        AlertDialog(
            onDismissRequest = { itemToRename = null },
            title = { Text("Rename Item") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth().testTag("rename_input_field")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) onUpdateTitle(item.id, newTitle)
                        itemToRename = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Move to Folder Dialog
    if (itemToMove != null) {
        val item = itemToMove!!
        var chosenFolder by remember { mutableStateOf(item.folder) }
        var customFolder by remember { mutableStateOf("") }
        var isCustom by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { itemToMove = null },
            title = { Text("Move to Folder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    folders.forEach { f ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    chosenFolder = f
                                    isCustom = false
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.RadioButton(
                                selected = !isCustom && chosenFolder == f,
                                onClick = {
                                    chosenFolder = f
                                    isCustom = false
                                }
                            )
                            Text(f, fontSize = 15.sp)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isCustom = true }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = isCustom,
                            onClick = { isCustom = true }
                        )
                        Text("New Folder...", fontSize = 15.sp)
                    }

                    if (isCustom) {
                        OutlinedTextField(
                            value = customFolder,
                            onValueChange = { customFolder = it },
                            placeholder = { Text("Folder name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = if (isCustom && customFolder.isNotBlank()) customFolder else chosenFolder
                        onMoveFolder(item.id, target)
                        itemToMove = null
                    }
                ) {
                    Text("Move")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToMove = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear All Confirmation Dialog
    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = { Text("Clear Scan History") },
            text = { Text("Are you sure you want to delete all public scan history? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAll()
                        showClearAllConfirm = false
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ScanHistoryItemCard(
    item: ScanItem,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onMoveFolder: () -> Unit,
    onTogglePrivate: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("history_item_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Thumbnail or Icon Box
                val thumbFile = item.thumbnailPath?.let { File(it) }
                if (thumbFile != null && thumbFile.exists()) {
                    val bmp = remember(item.thumbnailPath) { BitmapFactory.decodeFile(thumbFile.absolutePath) }
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        ItemTypeIconBox(item.type)
                    }
                } else {
                    ItemTypeIconBox(item.type)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (item.isPrivate) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Filled.Lock, contentDescription = "Private", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = item.content,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = dateFormat.format(Date(item.timestamp)),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = item.folder,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Options")
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Move Folder") },
                        leadingIcon = { Icon(Icons.Filled.DriveFileMove, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onMoveFolder()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (item.isPrivate) "Move to Public" else "Lock in Private Vault") },
                        leadingIcon = { Icon(if (item.isPrivate) Icons.Filled.LockOpen else Icons.Filled.Lock, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onTogglePrivate()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ItemTypeIconBox(type: ScanType) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = when (type) {
                ScanType.QR_CODE -> Icons.Filled.QrCode
                ScanType.BARCODE -> Icons.Filled.ViewWeek
                ScanType.DOCUMENT -> Icons.Filled.Folder
                ScanType.OCR_TEXT -> Icons.Filled.TextFields
                ScanType.PDF_FILE -> Icons.Filled.PictureAsPdf
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun DetailScanBottomSheetContent(item: ScanItem, onDismiss: () -> Unit) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 36.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(item.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("${item.type} • ${item.folder}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "Close")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // If QR code or Barcode: render regenerated barcode/QR image preview!
        if (item.type == ScanType.QR_CODE) {
            val qrBmp = remember(item.content) { QrBarcodeGenerator.generateQrBitmap(item.content, 400, 400) }
            if (qrBmp != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = qrBmp.asImageBitmap(),
                        contentDescription = "QR Code Preview",
                        modifier = Modifier
                            .size(180.dp)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    )
                }
            }
        } else if (item.type == ScanType.BARCODE) {
            val barcodeBmp = remember(item.content) { QrBarcodeGenerator.generateBarcodeBitmap(item.content) }
            if (barcodeBmp != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = barcodeBmp.asImageBitmap(),
                        contentDescription = "Barcode Preview",
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(90.dp)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    )
                }
            }
        }

        // Full content card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Raw Content:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Text(item.content, fontSize = 14.sp)
                if (item.fileSizeFormatted.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("File details: ${item.fileSizeFormatted} • ${item.pageCount} page(s)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    QrBarcodeParser.copyToClipboard(context, item.title, item.content)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy")
            }

            OutlinedButton(
                onClick = {
                    if (item.type == ScanType.PDF_FILE && item.filePath != null) {
                        val f = File(item.filePath)
                        if (f.exists()) PdfManager.sharePdf(context, f)
                        else QrBarcodeParser.shareText(context, item.content, item.title)
                    } else {
                        QrBarcodeParser.shareText(context, item.content, item.title)
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share")
            }
        }

        if (item.type == ScanType.PDF_FILE && item.filePath != null) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    val f = File(item.filePath)
                    if (f.exists()) PdfManager.printPdf(context, f)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Print Document")
            }
        }
    }
}
