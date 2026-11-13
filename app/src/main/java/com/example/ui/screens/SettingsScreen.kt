package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.OutlinedButton
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserPreferences
import com.example.ui.theme.GoldStar
import com.example.ui.theme.ScannerCyanAccent

@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    currentUser: FirebaseUser? = null,
    onSignOut: () -> Unit = {},
    onNavigatePremium: () -> Unit,
    onClearHistory: () -> Unit,
    onClearAllData: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val isPremium by preferences.isPremium.collectAsState()
    val vibrate by preferences.vibrateOnScan.collectAsState()
    val beep by preferences.beepOnScan.collectAsState()
    val autoCopy by preferences.autoCopy.collectAsState()
    val cloudBackup by preferences.cloudBackupEnabled.collectAsState()
    val isAppLock by preferences.isAppLockEnabled.collectAsState()
    val pageSize by preferences.pdfPageSize.collectAsState()

    // Dialogs
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Text(
            text = "Settings",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Signed-in Google Account Card
        if (currentUser != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_google_account_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (currentUser.photoUrl != null) {
                            AsyncImage(
                                model = currentUser.photoUrl,
                                contentDescription = currentUser.displayName ?: "Profile",
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.AccountCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = currentUser.displayName?.takeIf { it.isNotBlank() } ?: "Google User",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = currentUser.email ?: "Signed in with Google",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showSignOutDialog = true },
                        modifier = Modifier.testTag("btn_sign_out"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign Out",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sign Out", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // Premium Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onNavigatePremium)
                .testTag("settings_premium_card"),
            colors = CardDefaults.cardColors(
                containerColor = if (isPremium) GoldStar.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(if (isPremium) GoldStar else MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.WorkspacePremium,
                            contentDescription = null,
                            tint = if (isPremium) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = if (isPremium) "Alif Scanner Pro Active" else "Upgrade to Pro",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isPremium) "Manage subscription & benefits" else "Unlimited scans, OCR, batch mode & cloud",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Scanner Feedback & Actions
        SettingsSectionHeader("Scanner & Feedback")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                SettingsSwitchRow(
                    icon = Icons.Filled.Vibration,
                    title = "Vibrate on Scan",
                    subtitle = "Haptic pulse when barcode or QR is recognized",
                    checked = vibrate,
                    onCheckedChange = { preferences.setVibrateOnScan(it) }
                )
                SettingsSwitchRow(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    title = "Beep Sound on Scan",
                    subtitle = "Audio feedback confirmation",
                    checked = beep,
                    onCheckedChange = { preferences.setBeepOnScan(it) }
                )
                SettingsSwitchRow(
                    icon = Icons.Filled.QrCodeScanner,
                    title = "Auto-Copy to Clipboard",
                    subtitle = "Copy detected text or URL automatically",
                    checked = autoCopy,
                    onCheckedChange = { preferences.setAutoCopy(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // PDF & Export Preferences
        SettingsSectionHeader("PDF & Documents")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                SettingsActionRow(
                    icon = Icons.Filled.PictureAsPdf,
                    title = "Page Size Format",
                    value = pageSize,
                    onClick = {
                        val next = if (pageSize == "A4") "Letter" else "A4"
                        preferences.setPdfPageSize(next)
                        Toast.makeText(context, "Page size set to $next", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Cloud Backup & Sync
        SettingsSectionHeader("Cloud Backup & Sync")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                SettingsSwitchRow(
                    icon = Icons.Filled.Cloud,
                    title = "Cloud Backup (Drive / OneDrive)",
                    subtitle = if (cloudBackup) "Connected & Auto-sync enabled" else "Offline only. Scans stay local.",
                    checked = cloudBackup,
                    onCheckedChange = {
                        if (!isPremium && it) {
                            Toast.makeText(context, "Cloud backup is a Pro feature", Toast.LENGTH_SHORT).show()
                            onNavigatePremium()
                        } else {
                            preferences.setCloudBackupEnabled(it)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Security & Privacy
        SettingsSectionHeader("Security & Privacy")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                SettingsSwitchRow(
                    icon = Icons.Filled.Lock,
                    title = "App Lock PIN",
                    subtitle = if (isAppLock) "PIN protection enabled" else "Require 4-digit PIN on launch",
                    checked = isAppLock,
                    onCheckedChange = { enable ->
                        if (enable) {
                            showSetPinDialog = true
                        } else {
                            preferences.setAppLockEnabled(false)
                        }
                    }
                )

                SettingsActionRow(
                    icon = Icons.Filled.Fingerprint,
                    title = "Change PIN Code",
                    value = if (isAppLock) "Update" else "Disabled",
                    onClick = { showSetPinDialog = true }
                )

                SettingsActionRow(
                    icon = Icons.Filled.Delete,
                    title = "Clear Scan History",
                    value = "Clean",
                    onClick = { showClearHistoryDialog = true }
                )

                SettingsActionRow(
                    icon = Icons.Filled.Security,
                    title = "Reset All App Data",
                    value = "Reset",
                    onClick = { showClearDataDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // About & Legal
        SettingsSectionHeader("About & Legal")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                SettingsActionRow(
                    icon = Icons.Filled.Policy,
                    title = "Privacy Policy",
                    value = "View",
                    onClick = { showPrivacyDialog = true }
                )
                SettingsActionRow(
                    icon = Icons.Filled.Info,
                    title = "Terms of Service",
                    value = "View",
                    onClick = { showTermsDialog = true }
                )
                SettingsActionRow(
                    icon = Icons.Filled.Info,
                    title = "App Version",
                    value = "v1.0.0 (Production)",
                    onClick = {}
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))
    }

    // Set PIN Dialog
    if (showSetPinDialog) {
        var enteredPin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSetPinDialog = false },
            title = { Text("Set 4-Digit Security PIN") },
            text = {
                Column {
                    Text("Enter a 4-digit PIN to lock the app and the Private Vault:", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) enteredPin = it },
                        placeholder = { Text("e.g. 1234") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_settings_pin")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (enteredPin.length == 4) {
                            preferences.setPinCode(enteredPin)
                            preferences.setAppLockEnabled(true)
                            showSetPinDialog = false
                            Toast.makeText(context, "PIN code saved successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Please enter 4 digits", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Privacy Policy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Alif Scanner Pro values your privacy above all else.\n\n" +
                                "1. Local Processing: All image analysis, QR decoding, barcode reading, OCR, and PDF generation happen 100% locally on your device.\n\n" +
                                "2. No Unsolicited Uploads: We never upload your camera feed, photos, or scanned files to external servers without your explicit selection.\n\n" +
                                "3. Encrypted Vault: Items marked as private are stored securely with local device encryption.\n\n" +
                                "4. Zero Third-Party Trackers: Your scan history is yours alone.",
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("I Understand")
                }
            }
        )
    }

    // Terms of Service Dialog
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text("Terms of Service") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Alif Scanner Pro is provided as a professional document and barcode scanner application.\n\n" +
                                "Users are responsible for the documents and codes they scan and share. The app does not endorse third-party web content reached via scanned URLs.\n\n" +
                                "Subscription terms and billing are managed securely via Google Play.",
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTermsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Clear History Dialog
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear Scan History") },
            text = { Text("Delete all scan history entries? Scanned files will be removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearHistory()
                        showClearHistoryDialog = false
                        Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear All Data Dialog
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Reset All App Data") },
            text = { Text("Reset all preferences, scan history, private vault, and cached files? This will return the app to its initial state.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllData()
                        showClearDataDialog = false
                        Toast.makeText(context, "App data reset", Toast.LENGTH_SHORT).show()
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Sign Out Confirmation Dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out?") },
            text = { Text("You will need to sign in again with your Google (Gmail) account to continue using Alif Scanner Pro.") },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        onSignOut()
                    },
                    modifier = Modifier.testTag("confirm_sign_out_button"),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
}

@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(value, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
