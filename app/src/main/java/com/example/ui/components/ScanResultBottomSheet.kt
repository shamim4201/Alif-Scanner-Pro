package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scanner.ParsedResult
import com.example.scanner.QrBarcodeParser
import com.example.ui.theme.ScannerCyanAccent
import com.example.ui.theme.ScannerEmerald

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScanResultBottomSheet(
    rawValue: String,
    format: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val parsed = QrBarcodeParser.parse(rawValue, format)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header with Type badge and close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = when (parsed) {
                                is ParsedResult.Url -> Icons.Filled.Language
                                is ParsedResult.Wifi -> Icons.Filled.Wifi
                                is ParsedResult.Phone -> Icons.Filled.Phone
                                is ParsedResult.Email -> Icons.Filled.Email
                                is ParsedResult.Geo -> Icons.Filled.Map
                                is ParsedResult.ProductBarcode -> Icons.Filled.Search
                                else -> Icons.Filled.CheckCircle
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (parsed) {
                                is ParsedResult.Url -> "Website URL"
                                is ParsedResult.Wifi -> "Wi-Fi Network"
                                is ParsedResult.Phone -> "Phone Number"
                                is ParsedResult.Email -> "Email Address"
                                is ParsedResult.Contact -> "Contact Information"
                                is ParsedResult.Geo -> "Map Location"
                                is ParsedResult.ProductBarcode -> "Product: ${parsed.format}"
                                is ParsedResult.PlainText -> "Plain Text"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("btn_close_scan_result")
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Content Box
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    when (parsed) {
                        is ParsedResult.Url -> {
                            Text("URL Link", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = parsed.url,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        is ParsedResult.Wifi -> {
                            Text("Network SSID", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(parsed.ssid, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Password: ${parsed.password.ifBlank { "(None / Open)" }}", fontSize = 14.sp)
                            Text("Security: ${parsed.security}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        is ParsedResult.Phone -> {
                            Text("Phone Number", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(parsed.number, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        is ParsedResult.Email -> {
                            Text("Recipient Email", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(parsed.address, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            if (!parsed.subject.isNullOrEmpty()) {
                                Text("Subject: ${parsed.subject}", fontSize = 13.sp)
                            }
                        }
                        is ParsedResult.Contact -> {
                            Text("Contact Name", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(parsed.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            parsed.phone?.let { Text("Phone: $it", fontSize = 14.sp) }
                            parsed.email?.let { Text("Email: $it", fontSize = 14.sp) }
                            parsed.org?.let { Text("Organization: $it", fontSize = 13.sp) }
                        }
                        is ParsedResult.Geo -> {
                            Text("Coordinates", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Lat: ${parsed.latitude}, Lng: ${parsed.longitude}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        is ParsedResult.ProductBarcode -> {
                            Text("Barcode Number (${parsed.format})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = parsed.code,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp
                            )
                        }
                        is ParsedResult.PlainText -> {
                            Text("Extracted Text", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = parsed.text, fontSize = 15.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary contextual action button
            when (parsed) {
                is ParsedResult.Url -> {
                    Button(
                        onClick = { QrBarcodeParser.openUrl(context, parsed.url) },
                        modifier = Modifier.fillMaxWidth().testTag("btn_action_open_url"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Website in Browser")
                    }
                }
                is ParsedResult.Wifi -> {
                    Button(
                        onClick = {
                            QrBarcodeParser.copyToClipboard(context, "Wi-Fi Password", parsed.password)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("btn_action_copy_wifi_pass"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Wifi, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy Wi-Fi Password")
                    }
                }
                is ParsedResult.Phone -> {
                    Button(
                        onClick = { QrBarcodeParser.dialPhone(context, parsed.number) },
                        modifier = Modifier.fillMaxWidth().testTag("btn_action_dial_phone"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Call Number")
                    }
                }
                is ParsedResult.Email -> {
                    Button(
                        onClick = { QrBarcodeParser.sendEmail(context, parsed.address, parsed.subject, parsed.body) },
                        modifier = Modifier.fillMaxWidth().testTag("btn_action_send_email"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Email")
                    }
                }
                is ParsedResult.ProductBarcode -> {
                    Button(
                        onClick = { QrBarcodeParser.searchProductOnline(context, parsed.code) },
                        modifier = Modifier.fillMaxWidth().testTag("btn_action_search_barcode"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Search Barcode Online")
                    }
                }
                is ParsedResult.Geo -> {
                    Button(
                        onClick = { QrBarcodeParser.openInMaps(context, parsed.latitude, parsed.longitude, parsed.query) },
                        modifier = Modifier.fillMaxWidth().testTag("btn_action_open_map"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open in Maps")
                    }
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary Quick Actions: Copy, Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val textToCopy = when (parsed) {
                            is ParsedResult.Url -> parsed.url
                            is ParsedResult.Wifi -> parsed.password
                            is ParsedResult.Phone -> parsed.number
                            is ParsedResult.Email -> parsed.address
                            is ParsedResult.ProductBarcode -> parsed.code
                            else -> rawValue
                        }
                        QrBarcodeParser.copyToClipboard(context, "Scan Result", textToCopy)
                    },
                    modifier = Modifier.weight(1f).testTag("btn_copy_scan_result"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy")
                }

                OutlinedButton(
                    onClick = {
                        QrBarcodeParser.shareText(context, rawValue, "Share Scanned Item")
                    },
                    modifier = Modifier.weight(1f).testTag("btn_share_scan_result"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Auto-saved confirmation badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = ScannerEmerald,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Automatically saved to Scan History",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
