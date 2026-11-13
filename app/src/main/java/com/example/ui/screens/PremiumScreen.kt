package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldGradientEnd
import com.example.ui.theme.GoldGradientStart
import com.example.ui.theme.GoldStar
import com.example.ui.theme.ScannerBlueLight
import com.example.ui.theme.ScannerBluePrimary
import com.example.ui.theme.ScannerCyanAccent
import com.example.ui.theme.ScannerEmerald

@Composable
fun PremiumScreen(
    isPremium: Boolean,
    currentPlan: String,
    isSignedIn: Boolean = false,
    currentUserEmail: String? = null,
    onRequireLogin: (pendingPlan: String) -> Unit = {},
    onActivatePlan: (plan: String) -> Unit,
    onDeactivatePlan: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var selectedTier by remember { mutableStateOf("yearly") } // "monthly", "yearly", "lifetime"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(bottom = 36.dp)
    ) {
        // Hero Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(ScannerBluePrimary, ScannerBlueLight.copy(alpha = 0.8f), MaterialTheme.colorScheme.background)
                    )
                )
                .padding(top = 40.dp, bottom = 28.dp, start = 24.dp, end = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = com.example.R.drawable.alif_scanner_logo),
                    contentDescription = "Alif Scanner Pro",
                    modifier = Modifier
                        .size(86.dp)
                        .clip(RoundedCornerShape(20.dp))
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Alif Scanner Pro Premium",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isPremium) "Your Premium membership is active" else "Unlock full productivity & unlimited scanning tools",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Active Status Card (if premium already)
        if (isPremium) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
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
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = ScannerEmerald, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Active Plan: ${currentPlan.replaceFirstChar { it.uppercase() }}", fontWeight = FontWeight.Bold)
                            Text("All Pro features unlocked", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }

                    TextButton(onClick = onDeactivatePlan) {
                        Text("Manage", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Feature Highlights
        Text(
            text = "Everything in Pro",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ProFeatureRow(icon = Icons.Filled.HighQuality, title = "Unlimited High-Resolution Scans", desc = "No daily limits or scan caps")
            ProFeatureRow(icon = Icons.Filled.ViewCarousel, title = "Batch Scanning Mode", desc = "Scan 50+ codes or documents continuously")
            ProFeatureRow(icon = Icons.Filled.TextFields, title = "Advanced OCR Recognition", desc = "Extract text from photos and export to TXT or PDF")
            ProFeatureRow(icon = Icons.Filled.PictureAsPdf, title = "Professional PDF Tools", desc = "Merge, split, compress, and reorder pages freely")
            ProFeatureRow(icon = Icons.Filled.Lock, title = "Password-Protected Vault", desc = "PIN and biometric lock for sensitive scans")
            ProFeatureRow(icon = Icons.Filled.CloudUpload, title = "Cloud Sync & Backup", desc = "Sync with Google Drive, Dropbox, and OneDrive")
            ProFeatureRow(icon = Icons.Filled.Shield, title = "100% Ad-Free Experience", desc = "Clean, distraction-free workflow")
            ProFeatureRow(icon = Icons.Filled.SupportAgent, title = "Priority Customer Support", desc = "Direct help from the engineering team")
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Pricing Plans (Monthly, Yearly, Lifetime)
        Text(
            text = "Select a Plan",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Yearly Plan (Best Value)
            PricingPlanCard(
                title = "Yearly Plan",
                price = "$19.99 / year",
                badge = "Best Value • $1.66 / mo",
                isSelected = selectedTier == "yearly",
                onClick = { selectedTier = "yearly" },
                testTag = "plan_yearly"
            )

            // Monthly Plan
            PricingPlanCard(
                title = "Monthly Plan",
                price = "$2.99 / month",
                badge = null,
                isSelected = selectedTier == "monthly",
                onClick = { selectedTier = "monthly" },
                testTag = "plan_monthly"
            )

            // Lifetime Plan
            PricingPlanCard(
                title = "Lifetime Access",
                price = "$39.99 one-time",
                badge = "Pay Once, Keep Forever",
                isSelected = selectedTier == "lifetime",
                onClick = { selectedTier = "lifetime" },
                testTag = "plan_lifetime"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isSignedIn && !currentUserEmail.isNullOrBlank()) {
            Text(
                text = "Signed in as: $currentUserEmail",
                fontSize = 12.sp,
                color = ScannerEmerald,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            )
        }

        // Subscribe / Activate Button
        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
            Button(
                onClick = {
                    if (isSignedIn) {
                        onActivatePlan(selectedTier)
                        Toast.makeText(context, "Welcome to Alif Scanner Pro Premium!", Toast.LENGTH_SHORT).show()
                    } else {
                        onRequireLogin(selectedTier)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("btn_subscribe_now"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ScannerBluePrimary
                )
            ) {
                Text(
                    text = if (isPremium) "Update Subscription Plan" else "Start Free 7-Day Trial",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Restore Purchases and Legal Links
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = {
                    if (isSignedIn) {
                        onActivatePlan("lifetime")
                        Toast.makeText(context, "Purchases restored successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        onRequireLogin("lifetime")
                    }
                },
                modifier = Modifier.testTag("btn_restore_purchases")
            ) {
                Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Restore Purchases", fontSize = 13.sp)
            }
        }

        Text(
            text = "Subscriptions auto-renew unless cancelled at least 24 hours prior to the end of the current period. Manage via Google Play.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
fun ProFeatureRow(icon: ImageVector, title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(ScannerCyanAccent.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = ScannerCyanAccent, modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
            Text(text = desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PricingPlanCard(
    title: String,
    price: String,
    badge: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) ScannerCyanAccent else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .testTag(testTag),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant
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
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    if (badge != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = GoldStar.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = badge,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldStar,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(price, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            }

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) ScannerCyanAccent else Color.Transparent)
                    .border(2.dp, if (isSelected) ScannerCyanAccent else MaterialTheme.colorScheme.outline, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
