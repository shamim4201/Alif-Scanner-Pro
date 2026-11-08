package com.example.ui.screens

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.filled.Wifi
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.scanner.QrBarcodeGenerator
import com.example.ui.theme.ScannerBluePrimary
import com.example.ui.theme.ScannerCyanAccent
import com.example.ui.theme.ScannerEmerald
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

enum class QrContentType(val label: String, val icon: ImageVector) {
    TEXT("Text", Icons.Filled.TextFields),
    URL("Website", Icons.Filled.Language),
    WIFI("Wi-Fi", Icons.Filled.Wifi),
    PHONE("Phone", Icons.Filled.Phone),
    WHATSAPP("WhatsApp", Icons.Filled.Message),
    EMAIL("Email", Icons.Filled.Email),
    VCARD("Contact", Icons.Filled.ContactPage),
    BARCODE("Barcode", Icons.Filled.ViewWeek)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrGeneratorScreen(
    onClose: () -> Unit,
    onSaveToHistory: (title: String, content: String) -> Unit
) {
    BackHandler { onClose() }

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var selectedType by remember { mutableStateOf(QrContentType.URL) }

    // Input States
    var textInput by remember { mutableStateOf("https://") }
    var wifiSsid by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }
    var wifiSecurity by remember { mutableStateOf("WPA") } // WPA, WEP, nopass

    var phoneInput by remember { mutableStateOf("") }
    var waPhoneInput by remember { mutableStateOf("") }
    var waMessageInput by remember { mutableStateOf("") }

    var emailRecipient by remember { mutableStateOf("") }
    var emailSubject by remember { mutableStateOf("") }
    var emailBody by remember { mutableStateOf("") }

    var vcardName by remember { mutableStateOf("") }
    var vcardPhone by remember { mutableStateOf("") }
    var vcardEmail by remember { mutableStateOf("") }
    var vcardOrg by remember { mutableStateOf("") }

    var barcodeInput by remember { mutableStateOf("1234567890") }

    // Color Choice
    val colorOptions = listOf(
        Pair("Black", Color.Black),
        Pair("Blue", Color(0xFF0D47A1)),
        Pair("Emerald", Color(0xFF00796B)),
        Pair("Purple", Color(0xFF4A148C))
    )
    var selectedColorIndex by remember { mutableIntStateOf(0) }

    // Compile payload
    val compiledContent = remember(
        selectedType, textInput, wifiSsid, wifiPassword, wifiSecurity,
        phoneInput, waPhoneInput, waMessageInput,
        emailRecipient, emailSubject, emailBody,
        vcardName, vcardPhone, vcardEmail, vcardOrg,
        barcodeInput
    ) {
        when (selectedType) {
            QrContentType.TEXT, QrContentType.URL -> textInput
            QrContentType.WIFI -> "WIFI:T:$wifiSecurity;S:$wifiSsid;P:$wifiPassword;;"
            QrContentType.PHONE -> "tel:$phoneInput"
            QrContentType.WHATSAPP -> "https://wa.me/${waPhoneInput.replace("+", "").replace(" ", "")}?text=${Uri.encode(waMessageInput)}"
            QrContentType.EMAIL -> "mailto:$emailRecipient?subject=${Uri.encode(emailSubject)}&body=${Uri.encode(emailBody)}"
            QrContentType.VCARD -> """
                BEGIN:VCARD
                VERSION:3.0
                FN:$vcardName
                TEL:$vcardPhone
                EMAIL:$vcardEmail
                ORG:$vcardOrg
                END:VCARD
            """.trimIndent()
            QrContentType.BARCODE -> barcodeInput
        }
    }

    val qrBitmap = remember(compiledContent, selectedType, selectedColorIndex) {
        if (compiledContent.isBlank()) null
        else if (selectedType == QrContentType.BARCODE) {
            QrBarcodeGenerator.generateBarcodeBitmap(compiledContent)
        } else {
            val fgColor = colorOptions[selectedColorIndex].second.toArgb()
            QrBarcodeGenerator.generateQrBitmap(
                content = compiledContent,
                width = 512,
                height = 512,
                foregroundColor = fgColor,
                backgroundColor = android.graphics.Color.WHITE
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QR & Barcode Creator", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose, modifier = Modifier.testTag("qr_creator_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Type Selector
            Text("Select QR Type", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(QrContentType.values()) { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = {
                            selectedType = type
                            if (type == QrContentType.URL && textInput.isBlank()) textInput = "https://"
                        },
                        label = { Text(type.label, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(type.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ScannerBluePrimary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic Input Fields
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    when (selectedType) {
                        QrContentType.TEXT, QrContentType.URL -> {
                            OutlinedTextField(
                                value = textInput,
                                onValueChange = { textInput = it },
                                label = { Text(if (selectedType == QrContentType.URL) "Website URL" else "Text Content") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("qr_input_text"),
                                shape = RoundedCornerShape(12.dp),
                                maxLines = 4
                            )
                        }
                        QrContentType.WIFI -> {
                            OutlinedTextField(
                                value = wifiSsid,
                                onValueChange = { wifiSsid = it },
                                label = { Text("Network Name (SSID)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = wifiPassword,
                                onValueChange = { wifiPassword = it },
                                label = { Text("Wi-Fi Password") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }
                        QrContentType.PHONE -> {
                            OutlinedTextField(
                                value = phoneInput,
                                onValueChange = { phoneInput = it },
                                label = { Text("Phone Number") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }
                        QrContentType.WHATSAPP -> {
                            OutlinedTextField(
                                value = waPhoneInput,
                                onValueChange = { waPhoneInput = it },
                                label = { Text("Phone Number (with Country Code)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = waMessageInput,
                                onValueChange = { waMessageInput = it },
                                label = { Text("Pre-filled Message (optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                maxLines = 3
                            )
                        }
                        QrContentType.EMAIL -> {
                            OutlinedTextField(
                                value = emailRecipient,
                                onValueChange = { emailRecipient = it },
                                label = { Text("Recipient Email") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = emailSubject,
                                onValueChange = { emailSubject = it },
                                label = { Text("Subject") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = emailBody,
                                onValueChange = { emailBody = it },
                                label = { Text("Body") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                maxLines = 3
                            )
                        }
                        QrContentType.VCARD -> {
                            OutlinedTextField(
                                value = vcardName,
                                onValueChange = { vcardName = it },
                                label = { Text("Full Name") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = vcardPhone,
                                onValueChange = { vcardPhone = it },
                                label = { Text("Phone Number") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = vcardEmail,
                                onValueChange = { vcardEmail = it },
                                label = { Text("Email Address") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }
                        QrContentType.BARCODE -> {
                            OutlinedTextField(
                                value = barcodeInput,
                                onValueChange = { barcodeInput = it },
                                label = { Text("Barcode Digits or Code") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Color Selector (for QR)
            if (selectedType != QrContentType.BARCODE) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("QR Code Color", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        colorOptions.forEachIndexed { index, pair ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(pair.second, CircleShape)
                                    .border(
                                        width = if (selectedColorIndex == index) 3.dp else 1.dp,
                                        color = if (selectedColorIndex == index) ScannerCyanAccent else Color.LightGray,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorIndex = index },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedColorIndex == index) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // QR Code Preview Display
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Generated QR Code",
                            modifier = Modifier
                                .size(if (selectedType == QrContentType.BARCODE) 280.dp else 220.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .testTag("generated_qr_preview")
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .background(Color(0xFFF0F0F0), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Enter details above to generate",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = compiledContent.take(60) + if (compiledContent.length > 60) "..." else "",
                        fontSize = 12.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
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
                        if (qrBitmap != null) {
                            shareBitmap(context, qrBitmap, compiledContent)
                        } else {
                            Toast.makeText(context, "Please enter content first", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("qr_share_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ScannerBluePrimary)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share")
                }

                Button(
                    onClick = {
                        if (qrBitmap != null) {
                            saveBitmapToGallery(context, qrBitmap)
                        } else {
                            Toast.makeText(context, "Please enter content first", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("qr_save_gallery_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ScannerEmerald)
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Image")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    if (compiledContent.isNotBlank()) {
                        val title = when (selectedType) {
                            QrContentType.TEXT -> "Text: ${compiledContent.take(15)}"
                            QrContentType.URL -> "Web: $compiledContent"
                            QrContentType.WIFI -> "Wi-Fi: $wifiSsid"
                            QrContentType.PHONE -> "Call: $phoneInput"
                            QrContentType.WHATSAPP -> "WhatsApp: $waPhoneInput"
                            QrContentType.EMAIL -> "Email: $emailRecipient"
                            QrContentType.VCARD -> "Contact: $vcardName"
                            QrContentType.BARCODE -> "Barcode: $barcodeInput"
                        }
                        onSaveToHistory(title, compiledContent)
                        Toast.makeText(context, "Saved to Scan History!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Nothing to save", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("qr_save_history_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.QrCode, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add to History Vault")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun saveBitmapToGallery(context: Context, bitmap: Bitmap) {
    try {
        val filename = "QR_${System.currentTimeMillis()}.png"
        val fos: OutputStream?

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ScannerPro")
            }
            val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            fos = imageUri?.let { resolver.openOutputStream(it) }
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val subDir = File(imagesDir, "ScannerPro").apply { if (!exists()) mkdirs() }
            val file = File(subDir, filename)
            fos = FileOutputStream(file)
        }

        fos?.use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            Toast.makeText(context, "QR saved to Gallery (Pictures/ScannerPro)", Toast.LENGTH_LONG).show()
        } ?: run {
            Toast.makeText(context, "Could not open storage stream", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error saving: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun shareBitmap(context: Context, bitmap: Bitmap, content: String) {
    try {
        val cachePath = File(context.cacheDir, "images").apply { if (!exists()) mkdirs() }
        val file = File(cachePath, "shared_qr.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val authority = "${context.packageName}.fileprovider"
        val contentUri: Uri = FileProvider.getUriForFile(context, authority, file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_TEXT, content)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share QR Code"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error sharing: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
