package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.ScanItem
import com.example.ui.components.ScanResultBottomSheet
import com.example.ui.components.ScannerViewfinder
import com.example.ui.theme.GoldStar
import com.example.ui.theme.ScannerCyanAccent
import com.example.ui.theme.ScannerEmerald
import com.example.viewmodel.ScannerMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveScannerScreen(
    currentMode: ScannerMode,
    isPremium: Boolean,
    batchItems: List<ScanItem>,
    lastDetectedCode: Pair<String, String>?,
    onModeChanged: (ScannerMode) -> Unit,
    onCodeScanned: (rawValue: String, format: String) -> Unit,
    onDocumentCaptured: (Bitmap) -> Unit,
    onIdCardCaptured: (Bitmap) -> Unit = {},
    idCardFront: Bitmap? = null,
    onOcrCaptured: (Bitmap) -> Unit,
    onCloseScanner: () -> Unit,
    onDismissScanResult: () -> Unit,
    onSaveBatch: () -> Unit,
    onClearBatch: () -> Unit,
    onExportBatchCsv: () -> Unit,
    onExportBatchTxt: () -> Unit,
    onOpenPremium: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var showBatchSheet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            ScannerViewfinder(
                mode = currentMode,
                onCodeScanned = onCodeScanned,
                onDocumentCaptured = onDocumentCaptured,
                onIdCardCaptured = onIdCardCaptured,
                idCardFront = idCardFront,
                onOcrCaptured = onOcrCaptured,
                onClose = onCloseScanner
            )

            // Bottom Mode Selector Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (currentMode == ScannerMode.DOCUMENT || currentMode == ScannerMode.OCR || currentMode == ScannerMode.ID_CARD) 140.dp else 24.dp)
            ) {
                // Batch Mode HUD
                if (currentMode == ScannerMode.BATCH && batchItems.isNotEmpty()) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(18.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ScannerCyanAccent),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 12.dp)
                            .clickable { showBatchSheet = true }
                            .testTag("batch_hud_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .background(ScannerCyanAccent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${batchItems.size}",
                                    color = Color.Black,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Items in Batch • Tap to Review",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Modes Carousel
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        ModeChip(
                            label = "QR Code",
                            isSelected = currentMode == ScannerMode.QR,
                            onClick = { onModeChanged(ScannerMode.QR) },
                            testTag = "mode_chip_qr"
                        )
                    }
                    item {
                        ModeChip(
                            label = "Barcode",
                            isSelected = currentMode == ScannerMode.BARCODE,
                            onClick = { onModeChanged(ScannerMode.BARCODE) },
                            testTag = "mode_chip_barcode"
                        )
                    }
                    item {
                        ModeChip(
                            label = "Document",
                            isSelected = currentMode == ScannerMode.DOCUMENT,
                            onClick = { onModeChanged(ScannerMode.DOCUMENT) },
                            testTag = "mode_chip_document"
                        )
                    }
                    item {
                        ModeChip(
                            label = "ID Card",
                            isSelected = currentMode == ScannerMode.ID_CARD,
                            onClick = { onModeChanged(ScannerMode.ID_CARD) },
                            testTag = "mode_chip_id_card"
                        )
                    }
                    item {
                        ModeChip(
                            label = "OCR Text",
                            isSelected = currentMode == ScannerMode.OCR,
                            onClick = { onModeChanged(ScannerMode.OCR) },
                            testTag = "mode_chip_ocr"
                        )
                    }
                    item {
                        ModeChip(
                            label = "Batch Scan (PRO)",
                            isSelected = currentMode == ScannerMode.BATCH,
                            isPremiumBadge = true,
                            onClick = {
                                if (isPremium) {
                                    onModeChanged(ScannerMode.BATCH)
                                } else {
                                    onOpenPremium()
                                }
                            },
                            testTag = "mode_chip_batch"
                        )
                    }
                }
            }
        } else {
            // Permission Fallback Screen
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Camera Permission Needed",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Alif Scanner Pro needs camera access to scan QR codes, barcodes, and documents. Your pictures remain private on your device.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.testTag("btn_grant_camera_permission"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Grant Permission")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onCloseScanner,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }

        // Single Scan Detection Result Bottom Sheet
        if (lastDetectedCode != null && currentMode != ScannerMode.BATCH) {
            ScanResultBottomSheet(
                rawValue = lastDetectedCode.first,
                format = lastDetectedCode.second,
                onDismiss = onDismissScanResult
            )
        }

        // Batch Review Bottom Sheet
        if (showBatchSheet) {
            val batchSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showBatchSheet = false },
                sheetState = batchSheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Batch Scanning", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("${batchItems.size} items collected", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        IconButton(onClick = { showBatchSheet = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(batchItems) { index, item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "#${index + 1} - ${item.subType}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = item.content,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onSaveBatch()
                                showBatchSheet = false
                            },
                            modifier = Modifier.weight(1f).testTag("btn_save_batch"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save All")
                        }

                        OutlinedButton(
                            onClick = {
                                onClearBatch()
                                showBatchSheet = false
                            },
                            modifier = Modifier.weight(1f).testTag("btn_clear_batch"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onExportBatchCsv()
                                showBatchSheet = false
                            },
                            modifier = Modifier.weight(1f).testTag("btn_export_csv"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export CSV")
                        }

                        OutlinedButton(
                            onClick = {
                                onExportBatchTxt()
                                showBatchSheet = false
                            },
                            modifier = Modifier.weight(1f).testTag("btn_export_txt"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export TXT")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModeChip(
    label: String,
    isSelected: Boolean,
    isPremiumBadge: Boolean = false,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.65f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.25f)
        ),
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isPremiumBadge) {
                Icon(
                    Icons.Filled.WorkspacePremium,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else GoldStar,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
