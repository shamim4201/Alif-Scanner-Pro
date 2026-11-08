package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scanner.DocumentFilter
import com.example.scanner.ScannedPage
import com.example.ui.theme.ScannerCyanAccent
import com.example.ui.theme.ScannerEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentEditorScreen(
    pages: List<ScannedPage>,
    folders: List<String>,
    onFilterChanged: (pageIndex: Int, filter: DocumentFilter) -> Unit,
    onRotatePage: (pageIndex: Int) -> Unit,
    onDeletePage: (pageIndex: Int) -> Unit,
    onAddPageClicked: () -> Unit,
    onSaveDocument: (title: String, folder: String, saveAsPdf: Boolean, isPrivate: Boolean) -> Unit,
    onClose: () -> Unit
) {
    if (pages.isEmpty()) {
        onClose()
        return
    }

    var selectedPageIndex by remember { mutableIntStateOf(0) }
    val safeIndex = selectedPageIndex.coerceIn(0, pages.size - 1)
    val currentPage = pages[safeIndex]

    var docTitle by remember { mutableStateOf("Scanned_Doc_${System.currentTimeMillis() / 1000}") }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var selectedFolder by remember { mutableStateOf(folders.firstOrNull() ?: "General") }
    var saveAsPdf by remember { mutableStateOf(true) }
    var isPrivate by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showRenameDialog = true }
                ) {
                    Text(
                        text = docTitle,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Rename",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onClose, modifier = Modifier.testTag("doc_editor_btn_back")) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                Button(
                    onClick = { showSaveDialog = true },
                    modifier = Modifier.testTag("doc_editor_btn_save_done"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Main Page Preview Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.DarkGray),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = currentPage.processedBitmap.asImageBitmap(),
                        contentDescription = "Scanned Page ${safeIndex + 1}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    // Page Indicator Badge
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Page ${safeIndex + 1} of ${pages.size}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Quick Editing Controls (Rotate, Delete)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onRotatePage(safeIndex) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("doc_editor_rotate_btn")
                ) {
                    Icon(Icons.Filled.RotateRight, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rotate 90°")
                }
            }

            if (pages.size > 1) {
                IconButton(
                    onClick = {
                        onDeletePage(safeIndex)
                        if (selectedPageIndex >= pages.size - 1) {
                            selectedPageIndex = (pages.size - 2).coerceAtLeast(0)
                        }
                    },
                    modifier = Modifier.testTag("doc_editor_delete_page_btn")
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete Page",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Image Enhancement Filter Selector Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = currentPage.filter == DocumentFilter.MAGIC_COLOR,
                    onClick = { onFilterChanged(safeIndex, DocumentFilter.MAGIC_COLOR) },
                    label = { Text("Magic Color") },
                    leadingIcon = { Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("filter_magic_color")
                )
            }
            item {
                FilterChip(
                    selected = currentPage.filter == DocumentFilter.BLACK_AND_WHITE,
                    onClick = { onFilterChanged(safeIndex, DocumentFilter.BLACK_AND_WHITE) },
                    label = { Text("B&W Clean") },
                    modifier = Modifier.testTag("filter_bw")
                )
            }
            item {
                FilterChip(
                    selected = currentPage.filter == DocumentFilter.GRAYSCALE,
                    onClick = { onFilterChanged(safeIndex, DocumentFilter.GRAYSCALE) },
                    label = { Text("Grayscale") },
                    modifier = Modifier.testTag("filter_grayscale")
                )
            }
            item {
                FilterChip(
                    selected = currentPage.filter == DocumentFilter.COLOR,
                    onClick = { onFilterChanged(safeIndex, DocumentFilter.COLOR) },
                    label = { Text("Original") },
                    modifier = Modifier.testTag("filter_original")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom Page Thumbnails Carousel & Add Page Button
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(pages) { index, page ->
                    val isSelected = index == safeIndex
                    Box(
                        modifier = Modifier
                            .size(54.dp, 72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedPageIndex = index }
                            .testTag("page_thumb_$index")
                    ) {
                        Image(
                            bitmap = page.processedBitmap.asImageBitmap(),
                            contentDescription = "Page ${index + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                item {
                    // Add Page Button
                    Box(
                        modifier = Modifier
                            .size(54.dp, 72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .clickable(onClick = onAddPageClicked)
                            .testTag("doc_editor_add_page_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Add Page",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                "Add",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Save Document Options Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Document", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        value = docTitle,
                        onValueChange = { docTitle = it },
                        label = { Text("Document Title") },
                        modifier = Modifier.fillMaxWidth().testTag("input_save_doc_title")
                    )

                    // Choose Folder
                    Text("Folder:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(folders) { folder ->
                            FilterChip(
                                selected = selectedFolder == folder,
                                onClick = { selectedFolder = folder },
                                label = { Text(folder) }
                            )
                        }
                    }

                    // Format Options (PDF vs Image)
                    Text("Export Format:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = saveAsPdf,
                            onClick = { saveAsPdf = true }
                        )
                        Text("PDF Document (Multi-page)", fontSize = 14.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !saveAsPdf,
                            onClick = { saveAsPdf = false }
                        )
                        Text("JPEG Image Files", fontSize = 14.sp)
                    }

                    // Private folder option
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isPrivate,
                            onCheckedChange = { isPrivate = it }
                        )
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Lock in Private Folder", fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSaveDialog = false
                        onSaveDocument(docTitle, selectedFolder, saveAsPdf, isPrivate)
                    },
                    modifier = Modifier.testTag("btn_confirm_save_doc")
                ) {
                    Text("Save Document")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename Dialog
    if (showRenameDialog) {
        var tempName by remember { mutableStateOf(docTitle) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Document") },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("New Name") },
                    modifier = Modifier.fillMaxWidth().testTag("input_rename_doc")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempName.isNotBlank()) docTitle = tempName
                        showRenameDialog = false
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
