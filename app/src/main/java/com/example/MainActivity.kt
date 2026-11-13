package com.example

import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.GoogleAuthHelper
import com.example.auth.GoogleAuthHelper.authStateFlow
import com.example.data.UserProfileRepository
import com.example.model.ScanItem
import com.example.model.ScanType
import com.example.ui.screens.AuthScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import com.example.scanner.PdfManager
import com.example.scanner.QrBarcodeParser
import com.example.ui.components.PinLockScreen
import com.example.ui.components.ScannerBottomNavBar
import com.example.ui.screens.ActiveScannerScreen
import com.example.ui.screens.DocumentEditorScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.OcrScreen
import com.example.ui.screens.PdfToolboxScreen
import com.example.ui.screens.PremiumScreen
import com.example.ui.screens.QrGeneratorScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.ScannerProTheme
import com.example.viewmodel.AppTab
import com.example.viewmodel.ScannerMode
import com.example.viewmodel.ScannerViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ScannerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.example.ads.AdManager.initialize(this)

        setContent {
            ScannerProTheme {
                AppContent(viewModel = viewModel, activity = this)
            }
        }
    }
}

@Composable
fun AppContent(viewModel: ScannerViewModel, activity: ComponentActivity) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val profileRepository = remember(context) { UserProfileRepository(context) }
    val currentUser by Firebase.auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    var showAuthForPremium by remember { mutableStateOf(false) }
    var pendingPremiumPlan by remember { mutableStateOf<String?>(null) }

    // Sync user profile in Firestore whenever signed in
    LaunchedEffect(currentUser?.uid) {
        val signedInUser = currentUser
        if (signedInUser != null) {
            val isPrem = viewModel.preferences.isPremium.value
            profileRepository.syncUserProfileOnLogin(signedInUser, isPrem)
        }
    }

    ScannerApp(
        viewModel = viewModel,
        activity = activity,
        currentUser = currentUser,
        showAuthOverlay = showAuthForPremium,
        onRequirePremiumLogin = { plan ->
            pendingPremiumPlan = plan
            showAuthForPremium = true
        },
        onDismissAuthOverlay = {
            showAuthForPremium = false
            pendingPremiumPlan = null
        },
        onAuthSuccessForPremium = {
            showAuthForPremium = false
            val plan = pendingPremiumPlan
            pendingPremiumPlan = null
            if (plan != null) {
                viewModel.preferences.setPremium(true)
                viewModel.preferences.setPremiumPlan(plan)
                scope.launch {
                    val user = Firebase.auth.currentUser
                    if (user != null) {
                        profileRepository.syncUserProfileOnLogin(user, isPremium = true)
                    }
                }
                Toast.makeText(context, "Signed in & Alif Scanner Pro Premium activated!", Toast.LENGTH_SHORT).show()
            }
        },
        onPremiumPlanChanged = { isPrem ->
            if (Firebase.auth.currentUser != null) {
                scope.launch {
                    profileRepository.updatePremiumStatus(isPrem)
                }
            }
        },
        onSignOut = {
            GoogleAuthHelper.signOut(
                credentialManager = credentialManager,
                scope = scope
            )
        }
    )
}

@Composable
fun ScannerApp(
    viewModel: ScannerViewModel,
    activity: ComponentActivity,
    currentUser: FirebaseUser? = null,
    showAuthOverlay: Boolean = false,
    onRequirePremiumLogin: (String) -> Unit = {},
    onDismissAuthOverlay: () -> Unit = {},
    onAuthSuccessForPremium: () -> Unit = {},
    onPremiumPlanChanged: (Boolean) -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val isPremium by viewModel.preferences.isPremium.collectAsState()
    val premiumPlan by viewModel.preferences.premiumPlan.collectAsState()
    val isAppLocked by viewModel.isAppLocked.collectAsState()

    // Overlay screen states
    val isScannerActive by viewModel.isScannerActive.collectAsState()
    val scannerMode by viewModel.scannerMode.collectAsState()
    val batchItems by viewModel.batchItems.collectAsState()
    val lastDetectedCode by viewModel.lastDetectedCode.collectAsState()

    val isDocumentEditorActive by viewModel.isDocumentEditorActive.collectAsState()
    val scannedPages by viewModel.scannedPages.collectAsState()

    val isOcrViewerActive by viewModel.isOcrViewerActive.collectAsState()
    val extractedOcrText by viewModel.extractedOcrText.collectAsState()
    val isOcrLoading by viewModel.isOcrLoading.collectAsState()

    val isPdfToolsActive by viewModel.isPdfToolsActive.collectAsState()
    val isQrGeneratorActive by viewModel.isQrGeneratorActive.collectAsState()
    val idCardFront by viewModel.idCardFront.collectAsState()

    // History data
    val filteredScans by viewModel.filteredScans.collectAsState()
    val privateScans by viewModel.privateScans.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val searchQuery by viewModel.historySearchQuery.collectAsState()
    val filterType by viewModel.historyFilterType.collectAsState()
    val selectedFolder by viewModel.historySelectedFolder.collectAsState()
    val sortByNewest by viewModel.sortByNewest.collectAsState()
    val isPrivateUnlocked by viewModel.isPrivateFolderUnlocked.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (isAppLocked) {
            // Master App Lock Screen
            PinLockScreen(
                title = "Alif Scanner Pro Locked",
                subtitle = "Enter 4-digit PIN to unlock the app",
                onPinEntered = { pin ->
                    viewModel.unlockApp(pin)
                }
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (!isScannerActive && !isDocumentEditorActive && !isOcrViewerActive && !isPdfToolsActive && !isQrGeneratorActive && !showAuthOverlay) {
                            ScannerBottomNavBar(
                                currentTab = currentTab,
                                onTabSelected = { viewModel.setTab(it) },
                                isPremium = isPremium
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Main Tab Content
                        when (currentTab) {
                            AppTab.HOME -> {
                                HomeScreen(
                                    isPremium = isPremium,
                                    recentScans = filteredScans,
                                    onOpenScanner = { mode -> viewModel.openScanner(mode) },
                                    onNavigateTab = { viewModel.setTab(it) },
                                    onOpenPdfTools = { viewModel.openPdfTools() },
                                    onOpenQrGenerator = { viewModel.openQrGenerator() },
                                    onSelectScanItem = { viewModel.selectDetailScan(it) },
                                    onImportImage = { uri ->
                                        handleImportedImage(viewModel, uri)
                                    }
                                )
                            }
                            AppTab.HISTORY -> {
                                HistoryScreen(
                                    scans = filteredScans,
                                    privateScans = privateScans,
                                    folders = folders,
                                    searchQuery = searchQuery,
                                    selectedFilterType = filterType,
                                    selectedFolder = selectedFolder,
                                    sortByNewest = sortByNewest,
                                    isPrivateFolderUnlocked = isPrivateUnlocked,
                                    onSearchChanged = { viewModel.setHistorySearch(it) },
                                    onFilterTypeChanged = { viewModel.setHistoryFilter(it) },
                                    onFolderChanged = { viewModel.setHistoryFolder(it) },
                                    onToggleSortOrder = { viewModel.toggleSortOrder() },
                                    onUpdateTitle = { id, title -> viewModel.updateItemTitle(id, title) },
                                    onMoveFolder = { id, folder -> viewModel.moveItemFolder(id, folder) },
                                    onTogglePrivate = { id, isPriv -> viewModel.setItemPrivate(id, isPriv) },
                                    onDeleteScan = { viewModel.deleteScanItem(it) },
                                    onClearAll = { viewModel.clearAllHistory() },
                                    onUnlockPrivateFolder = { pin -> viewModel.unlockPrivateFolder(pin) },
                                    onLockPrivateFolder = { viewModel.lockPrivateFolder() }
                                )
                            }
                            AppTab.PREMIUM -> {
                                PremiumScreen(
                                    isPremium = isPremium,
                                    currentPlan = premiumPlan,
                                    isSignedIn = currentUser != null,
                                    currentUserEmail = currentUser?.email,
                                    onRequireLogin = onRequirePremiumLogin,
                                    onActivatePlan = { plan ->
                                        viewModel.preferences.setPremium(true)
                                        viewModel.preferences.setPremiumPlan(plan)
                                        onPremiumPlanChanged(true)
                                    },
                                    onDeactivatePlan = {
                                        viewModel.preferences.setPremium(false)
                                        viewModel.preferences.setPremiumPlan("free")
                                        onPremiumPlanChanged(false)
                                    }
                                )
                            }
                            AppTab.SETTINGS -> {
                                SettingsScreen(
                                    preferences = viewModel.preferences,
                                    currentUser = currentUser,
                                    onSignOut = onSignOut,
                                    onNavigatePremium = { viewModel.setTab(AppTab.PREMIUM) },
                                    onClearHistory = { viewModel.clearAllHistory() },
                                    onClearAllData = { viewModel.clearAppData() }
                                )
                            }
                        }

                        // Full Screen Scanner Overlay
                        AnimatedVisibility(
                            visible = isScannerActive,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            ActiveScannerScreen(
                                currentMode = scannerMode,
                                isPremium = isPremium,
                                batchItems = batchItems,
                                lastDetectedCode = lastDetectedCode,
                                onModeChanged = { viewModel.openScanner(it) },
                                onCodeScanned = { raw, format ->
                                    viewModel.onCodeScanned(raw, format)
                                },
                                onDocumentCaptured = { bmp ->
                                    viewModel.addScannedPage(bmp)
                                },
                                onIdCardCaptured = { bmp ->
                                    viewModel.onIdCardCaptured(bmp)
                                },
                                idCardFront = idCardFront,
                                onOcrCaptured = { bmp ->
                                    viewModel.runOcrOnBitmap(bmp)
                                },
                                onCloseScanner = { viewModel.closeScanner() },
                                onDismissScanResult = { viewModel.onCodeScanned("", "") },
                                onSaveBatch = {
                                    viewModel.saveAllBatchItems()
                                    Toast.makeText(context, "Batch saved to history!", Toast.LENGTH_SHORT).show()
                                },
                                onClearBatch = { viewModel.clearBatch() },
                                onExportBatchCsv = {
                                    val file = viewModel.exportBatchAsCsv(context)
                                    QrBarcodeParser.shareText(context, file.readText(), "Exported CSV Scans")
                                },
                                onExportBatchTxt = {
                                    val file = viewModel.exportBatchAsTxt(context)
                                    QrBarcodeParser.shareText(context, file.readText(), "Exported TXT Scans")
                                },
                                onOpenPremium = {
                                    viewModel.closeScanner()
                                    viewModel.setTab(AppTab.PREMIUM)
                                }
                            )
                        }

                        // Document Editor Overlay
                        AnimatedVisibility(
                            visible = isDocumentEditorActive,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            DocumentEditorScreen(
                                pages = scannedPages,
                                folders = folders,
                                onFilterChanged = { idx, filter -> viewModel.updatePageFilter(idx, filter) },
                                onRotatePage = { idx -> viewModel.rotatePage(idx) },
                                onDeletePage = { idx -> viewModel.deletePage(idx) },
                                onAddPageClicked = {
                                    viewModel.openScanner(ScannerMode.DOCUMENT)
                                },
                                onSaveDocument = { title, folder, saveAsPdf, isPriv ->
                                    viewModel.saveDocument(title, folder, saveAsPdf, isPriv) {
                                        viewModel.closeDocumentEditor()
                                        viewModel.closeScanner()
                                        if (!isPremium) {
                                            com.example.ads.AdManager.showInterstitialAd(activity) {}
                                        }
                                    }
                                },
                                onClose = { viewModel.closeDocumentEditor() }
                            )
                        }

                        // OCR Screen Overlay
                        AnimatedVisibility(
                            visible = isOcrViewerActive,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            OcrScreen(
                                initialText = extractedOcrText,
                                isLoading = isOcrLoading,
                                onSaveOcr = { title, text ->
                                    viewModel.saveOcrResult(title, text)
                                    viewModel.closeOcrViewer()
                                },
                                onClose = { viewModel.closeOcrViewer() }
                            )
                        }

                        // PDF Toolbox Screen Overlay
                        AnimatedVisibility(
                            visible = isPdfToolsActive,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            val pdfItems = filteredScans.filter { it.type == ScanType.PDF_FILE }
                            PdfToolboxScreen(
                                pdfItems = pdfItems,
                                onSaveNewPdf = { item ->
                                    viewModel.saveScanItemDirect(item)
                                },
                                onDeletePdf = { item -> viewModel.deleteScanItem(item) },
                                onClose = { viewModel.closePdfTools() }
                            )
                        }

                        // QR & Barcode Creator Overlay
                        AnimatedVisibility(
                            visible = isQrGeneratorActive,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            QrGeneratorScreen(
                                onClose = { viewModel.closeQrGenerator() },
                                onSaveToHistory = { title, content ->
                                    viewModel.saveGeneratedQr(title, content)
                                }
                            )
                        }
                    }
                }

                // Google Sign-In Overlay (shown only when purchasing/restoring Premium while signed out)
                AnimatedVisibility(
                    visible = showAuthOverlay,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    AuthScreen(
                        onAuthSuccess = onAuthSuccessForPremium,
                        onClose = onDismissAuthOverlay
                    )
                }
            }
        }
    }
}

private fun handleImportedImage(viewModel: ScannerViewModel, uri: Uri) {
    val app = viewModel.getApplication<android.app.Application>()
    try {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val src = ImageDecoder.createSource(app.contentResolver, uri)
            ImageDecoder.decodeBitmap(src) { d, _, _ -> d.isMutableRequired = true }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(app.contentResolver, uri)
        }
        viewModel.addScannedPage(bitmap)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
