package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.UserPreferences
import com.example.database.AppDatabase
import com.example.model.ScanItem
import com.example.model.ScanType
import com.example.scanner.DocumentFilter
import com.example.scanner.DocumentProcessor
import com.example.scanner.OcrManager
import com.example.scanner.PdfManager
import com.example.scanner.QrBarcodeParser
import com.example.scanner.ScannedPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class AppTab {
    HOME,
    HISTORY,
    PREMIUM,
    SETTINGS
}

enum class ScannerMode {
    QR,
    BARCODE,
    DOCUMENT,
    ID_CARD,
    OCR,
    BATCH
}

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val scanDao = db.scanDao()
    val preferences = UserPreferences(application)

    // Navigation State
    private val _currentTab = MutableStateFlow(AppTab.HOME)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _scannerMode = MutableStateFlow(ScannerMode.QR)
    val scannerMode: StateFlow<ScannerMode> = _scannerMode.asStateFlow()

    private val _isScannerActive = MutableStateFlow(false)
    val isScannerActive: StateFlow<Boolean> = _isScannerActive.asStateFlow()

    private val _isDocumentEditorActive = MutableStateFlow(false)
    val isDocumentEditorActive: StateFlow<Boolean> = _isDocumentEditorActive.asStateFlow()

    private val _isOcrViewerActive = MutableStateFlow(false)
    val isOcrViewerActive: StateFlow<Boolean> = _isOcrViewerActive.asStateFlow()

    private val _isPdfToolsActive = MutableStateFlow(false)
    val isPdfToolsActive: StateFlow<Boolean> = _isPdfToolsActive.asStateFlow()

    private val _isQrGeneratorActive = MutableStateFlow(false)
    val isQrGeneratorActive: StateFlow<Boolean> = _isQrGeneratorActive.asStateFlow()

    private val _idCardFront = MutableStateFlow<Bitmap?>(null)
    val idCardFront: StateFlow<Bitmap?> = _idCardFront.asStateFlow()

    private val _selectedDetailScan = MutableStateFlow<ScanItem?>(null)
    val selectedDetailScan: StateFlow<ScanItem?> = _selectedDetailScan.asStateFlow()

    // Scanned results & Batch
    private val _lastDetectedCode = MutableStateFlow<Pair<String, String>?>(null) // raw, format
    val lastDetectedCode: StateFlow<Pair<String, String>?> = _lastDetectedCode.asStateFlow()

    private val _batchItems = MutableStateFlow<List<ScanItem>>(emptyList())
    val batchItems: StateFlow<List<ScanItem>> = _batchItems.asStateFlow()

    // Document Scanning pages
    private val _scannedPages = MutableStateFlow<List<ScannedPage>>(emptyList())
    val scannedPages: StateFlow<List<ScannedPage>> = _scannedPages.asStateFlow()

    // OCR result text
    private val _extractedOcrText = MutableStateFlow("")
    val extractedOcrText: StateFlow<String> = _extractedOcrText.asStateFlow()

    private val _isOcrLoading = MutableStateFlow(false)
    val isOcrLoading: StateFlow<Boolean> = _isOcrLoading.asStateFlow()

    // Security Gate State
    private val _isAppLocked = MutableStateFlow(preferences.isAppLockEnabled.value)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private val _isPrivateFolderUnlocked = MutableStateFlow(false)
    val isPrivateFolderUnlocked: StateFlow<Boolean> = _isPrivateFolderUnlocked.asStateFlow()

    // History filter and search query
    private val _historySearchQuery = MutableStateFlow("")
    val historySearchQuery: StateFlow<String> = _historySearchQuery.asStateFlow()

    private val _historyFilterType = MutableStateFlow<ScanType?>(null)
    val historyFilterType: StateFlow<ScanType?> = _historyFilterType.asStateFlow()

    private val _historySelectedFolder = MutableStateFlow<String?>(null)
    val historySelectedFolder: StateFlow<String?> = _historySelectedFolder.asStateFlow()

    private val _sortByNewest = MutableStateFlow(true)
    val sortByNewest: StateFlow<Boolean> = _sortByNewest.asStateFlow()

    // DB flows
    val allPublicScans: StateFlow<List<ScanItem>> = scanDao.getAllPublicScans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val privateScans: StateFlow<List<ScanItem>> = scanDao.getPrivateScans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<String>> = scanDao.getAllFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("General", "Work", "Receipts", "Personal"))

    // Filtered scans for UI
    val filteredScans: StateFlow<List<ScanItem>> = combine(
        allPublicScans,
        _historySearchQuery,
        _historyFilterType,
        _historySelectedFolder,
        _sortByNewest
    ) { scans, query, typeFilter, folderFilter, newest ->
        var list = scans
        if (typeFilter != null) {
            list = list.filter { it.type == typeFilter }
        }
        if (folderFilter != null) {
            list = list.filter { it.folder == folderFilter }
        }
        if (query.isNotBlank()) {
            list = list.filter {
                it.title.contains(query, ignoreCase = true) || it.content.contains(query, ignoreCase = true)
            }
        }
        if (newest) {
            list.sortedByDescending { it.timestamp }
        } else {
            list.sortedBy { it.timestamp }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var toneGen: ToneGenerator? = null

    init {
        try {
            toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun openScanner(mode: ScannerMode) {
        _scannerMode.value = mode
        _lastDetectedCode.value = null
        _isScannerActive.value = true
    }

    fun closeScanner() {
        _isScannerActive.value = false
        _idCardFront.value = null
    }

    fun openDocumentEditor() {
        _isDocumentEditorActive.value = true
    }

    fun closeDocumentEditor() {
        _isDocumentEditorActive.value = false
    }

    fun openOcrViewer(text: String = "") {
        if (text.isNotEmpty()) {
            _extractedOcrText.value = text
        }
        _isOcrViewerActive.value = true
    }

    fun closeOcrViewer() {
        _isOcrViewerActive.value = false
    }

    fun openPdfTools() {
        _isPdfToolsActive.value = true
    }

    fun closePdfTools() {
        _isPdfToolsActive.value = false
    }

    fun openQrGenerator() {
        _isQrGeneratorActive.value = true
    }

    fun closeQrGenerator() {
        _isQrGeneratorActive.value = false
    }

    fun onIdCardCaptured(bitmap: Bitmap) {
        if (_idCardFront.value == null) {
            _idCardFront.value = bitmap
        } else {
            val front = _idCardFront.value!!
            val merged = DocumentProcessor.mergeIdCard(front, bitmap)
            _idCardFront.value = null
            addScannedPage(merged)
            _isScannerActive.value = false
        }
    }

    fun resetIdCardScan() {
        _idCardFront.value = null
    }

    fun saveGeneratedQr(title: String, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val item = ScanItem(
                title = title,
                content = content,
                type = ScanType.QR_CODE,
                subType = "CREATED_QR",
                timestamp = System.currentTimeMillis()
            )
            scanDao.insertScan(item)
        }
    }

    fun selectDetailScan(item: ScanItem?) {
        _selectedDetailScan.value = item
    }

    fun setHistorySearch(query: String) {
        _historySearchQuery.value = query
    }

    fun setHistoryFilter(type: ScanType?) {
        _historyFilterType.value = type
    }

    fun setHistoryFolder(folder: String?) {
        _historySelectedFolder.value = folder
    }

    fun toggleSortOrder() {
        _sortByNewest.value = !_sortByNewest.value
    }

    // Sound and Haptics
    fun triggerScanFeedback() {
        val app = getApplication<Application>()
        if (preferences.vibrateOnScan.value) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    val v = app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    @Suppress("DEPRECATION")
                    v?.vibrate(50)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (preferences.beepOnScan.value) {
            try {
                toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // QR & Barcode Detection handler
    fun onCodeScanned(rawValue: String, format: String) {
        val currentPair = _lastDetectedCode.value
        if (currentPair?.first == rawValue && System.currentTimeMillis() % 1000 < 600) {
            return
        }

        triggerScanFeedback()

        if (preferences.autoCopy.value) {
            QrBarcodeParser.copyToClipboard(getApplication(), "Scanned Result", rawValue)
        }

        val type = if (format.contains("QR", ignoreCase = true)) ScanType.QR_CODE else ScanType.BARCODE
        val parsed = QrBarcodeParser.parse(rawValue, format)
        val subType = when (parsed) {
            is com.example.scanner.ParsedResult.Url -> "URL"
            is com.example.scanner.ParsedResult.Wifi -> "WIFI"
            is com.example.scanner.ParsedResult.Phone -> "PHONE"
            is com.example.scanner.ParsedResult.Email -> "EMAIL"
            is com.example.scanner.ParsedResult.Contact -> "CONTACT"
            is com.example.scanner.ParsedResult.Geo -> "MAP"
            is com.example.scanner.ParsedResult.ProductBarcode -> parsed.format
            is com.example.scanner.ParsedResult.PlainText -> "TEXT"
        }

        val title = when (parsed) {
            is com.example.scanner.ParsedResult.Url -> parsed.url.removePrefix("https://").removePrefix("http://").take(30)
            is com.example.scanner.ParsedResult.Wifi -> "Wi-Fi: ${parsed.ssid}"
            is com.example.scanner.ParsedResult.Phone -> "Phone: ${parsed.number}"
            is com.example.scanner.ParsedResult.Email -> "Email: ${parsed.address}"
            is com.example.scanner.ParsedResult.Contact -> "Contact: ${parsed.name}"
            is com.example.scanner.ParsedResult.Geo -> "Location: ${parsed.latitude}, ${parsed.longitude}"
            is com.example.scanner.ParsedResult.ProductBarcode -> "${parsed.format}: ${parsed.code}"
            is com.example.scanner.ParsedResult.PlainText -> rawValue.take(30)
        }

        val item = ScanItem(
            title = title,
            type = type,
            content = rawValue,
            subType = subType
        )

        if (_scannerMode.value == ScannerMode.BATCH) {
            // In batch mode, append to batch list
            _batchItems.value = _batchItems.value + item
        } else {
            _lastDetectedCode.value = Pair(rawValue, format)
            // Save to history automatically
            viewModelScope.launch(Dispatchers.IO) {
                scanDao.insertScan(item)
            }
        }
    }

    fun clearBatch() {
        _batchItems.value = emptyList()
    }

    fun saveAllBatchItems() {
        val items = _batchItems.value
        viewModelScope.launch(Dispatchers.IO) {
            items.forEach { scanDao.insertScan(it) }
        }
        _batchItems.value = emptyList()
    }

    fun exportBatchAsCsv(context: Context): File {
        val items = _batchItems.value
        val storageDir = File(context.filesDir, "exports").apply { if (!exists()) mkdirs() }
        val file = File(storageDir, "batch_export_${System.currentTimeMillis()}.csv")
        file.bufferedWriter().use { out ->
            out.write("Index,Type,Format,Content,Timestamp\n")
            items.forEachIndexed { i, item ->
                out.write("${i + 1},\"${item.type}\",\"${item.subType}\",\"${item.content.replace("\"", "\"\"")}\",${item.timestamp}\n")
            }
        }
        return file
    }

    fun exportBatchAsTxt(context: Context): File {
        val items = _batchItems.value
        val storageDir = File(context.filesDir, "exports").apply { if (!exists()) mkdirs() }
        val file = File(storageDir, "batch_export_${System.currentTimeMillis()}.txt")
        file.bufferedWriter().use { out ->
            out.write("SCANNER PRO - BATCH EXPORT\n")
            out.write("Total Items: ${items.size}\n\n")
            items.forEachIndexed { i, item ->
                out.write("[${i + 1}] (${item.subType}) ${item.title}\nContent: ${item.content}\n\n")
            }
        }
        return file
    }

    // Document Scanning Page Management
    fun addScannedPage(bitmap: Bitmap) {
        val processed = DocumentProcessor.applyFilterAndTransform(
            source = bitmap,
            filter = DocumentFilter.MAGIC_COLOR
        )
        val page = ScannedPage(
            originalBitmap = bitmap,
            processedBitmap = processed,
            filter = DocumentFilter.MAGIC_COLOR
        )
        _scannedPages.value = _scannedPages.value + page
        openDocumentEditor()
    }

    fun updatePageFilter(pageIndex: Int, filter: DocumentFilter) {
        val current = _scannedPages.value.toMutableList()
        if (pageIndex in current.indices) {
            val p = current[pageIndex]
            p.filter = filter
            p.processedBitmap = DocumentProcessor.applyFilterAndTransform(
                source = p.originalBitmap,
                filter = filter,
                rotationDegrees = p.rotation
            )
            _scannedPages.value = current
        }
    }

    fun rotatePage(pageIndex: Int) {
        val current = _scannedPages.value.toMutableList()
        if (pageIndex in current.indices) {
            val p = current[pageIndex]
            p.rotation = (p.rotation + 90) % 360
            p.processedBitmap = DocumentProcessor.applyFilterAndTransform(
                source = p.originalBitmap,
                filter = p.filter,
                rotationDegrees = p.rotation
            )
            _scannedPages.value = current
        }
    }

    fun deletePage(pageIndex: Int) {
        val current = _scannedPages.value.toMutableList()
        if (pageIndex in current.indices) {
            current.removeAt(pageIndex)
            _scannedPages.value = current
            if (current.isEmpty()) {
                closeDocumentEditor()
            }
        }
    }

    fun reorderPages(fromIndex: Int, toIndex: Int) {
        val current = _scannedPages.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            _scannedPages.value = current
        }
    }

    fun clearScannedPages() {
        _scannedPages.value = emptyList()
        closeDocumentEditor()
    }

    fun saveDocument(
        title: String,
        folder: String = "General",
        saveAsPdf: Boolean = true,
        isPrivate: Boolean = false,
        onComplete: (ScanItem) -> Unit
    ) {
        val pages = _scannedPages.value
        if (pages.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val bitmaps = pages.map { it.processedBitmap }

            // Save first page as thumbnail
            val thumbFile = DocumentProcessor.saveBitmapToFile(app, bitmaps.first(), "thumb")

            val (filePath, type, subType, sizeFormatted) = if (saveAsPdf) {
                val pdfFile = PdfManager.createPdfFromBitmaps(
                    context = app,
                    bitmaps = bitmaps,
                    filename = title,
                    pageSize = "A4"
                )
                Tuple4(pdfFile.absolutePath, ScanType.PDF_FILE, "PDF_PAGES", PdfManager.formatFileSize(pdfFile.length()))
            } else {
                val imgFile = DocumentProcessor.saveBitmapToFile(app, bitmaps.first(), "doc")
                Tuple4(imgFile.absolutePath, ScanType.DOCUMENT, "IMAGE_PAGES", PdfManager.formatFileSize(imgFile.length()))
            }

            val scanItem = ScanItem(
                title = title,
                type = type,
                content = "Scanned document with ${pages.size} page(s)",
                subType = subType,
                filePath = filePath,
                thumbnailPath = thumbFile.absolutePath,
                folder = folder,
                isPrivate = isPrivate,
                pageCount = pages.size,
                fileSizeFormatted = sizeFormatted
            )

            val newId = scanDao.insertScan(scanItem)
            val savedItem = scanItem.copy(id = newId)

            withContext(Dispatchers.Main) {
                clearScannedPages()
                onComplete(savedItem)
            }
        }
    }

    // OCR Execution
    fun runOcrOnBitmap(bitmap: Bitmap, onComplete: ((String) -> Unit)? = null) {
        _isOcrLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val extracted = OcrManager.extractTextFromBitmap(bitmap)
            withContext(Dispatchers.Main) {
                _extractedOcrText.value = extracted
                _isOcrLoading.value = false
                openOcrViewer(extracted)
                onComplete?.invoke(extracted)
            }
        }
    }

    fun saveOcrResult(title: String, text: String, folder: String = "General") {
        viewModelScope.launch(Dispatchers.IO) {
            val item = ScanItem(
                title = title.ifBlank { "OCR Note ${System.currentTimeMillis()}" },
                type = ScanType.OCR_TEXT,
                content = text,
                subType = "TEXT",
                folder = folder,
                fileSizeFormatted = "${text.length} chars"
            )
            scanDao.insertScan(item)
        }
    }

    // PDF Toolbox operations
    fun mergePdfs(pdfItems: List<ScanItem>, outputTitle: String, onComplete: (ScanItem) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val files = pdfItems.mapNotNull { it.filePath?.let { p -> File(p) } }.filter { it.exists() }
            if (files.isNotEmpty()) {
                val mergedFile = PdfManager.mergePdfs(app, files, outputTitle)
                val thumbBmps = PdfManager.renderPdfPagesToBitmaps(app, mergedFile)
                val thumbPath = if (thumbBmps.isNotEmpty()) {
                    DocumentProcessor.saveBitmapToFile(app, thumbBmps.first(), "thumb").absolutePath
                } else null

                val item = ScanItem(
                    title = outputTitle,
                    type = ScanType.PDF_FILE,
                    content = "Merged from ${files.size} documents",
                    subType = "MERGED_PDF",
                    filePath = mergedFile.absolutePath,
                    thumbnailPath = thumbPath,
                    pageCount = thumbBmps.size,
                    fileSizeFormatted = PdfManager.formatFileSize(mergedFile.length())
                )
                val id = scanDao.insertScan(item)
                withContext(Dispatchers.Main) {
                    onComplete(item.copy(id = id))
                }
            }
        }
    }

    // Security PIN Lock
    fun unlockApp(pin: String): Boolean {
        val stored = preferences.pinCode.value
        if (stored == null || stored == pin) {
            _isAppLocked.value = false
            return true
        }
        return false
    }

    fun unlockPrivateFolder(pin: String): Boolean {
        val stored = preferences.pinCode.value
        if (stored == null || stored == pin) {
            _isPrivateFolderUnlocked.value = true
            return true
        }
        return false
    }

    fun lockPrivateFolder() {
        _isPrivateFolderUnlocked.value = false
    }

    // Database modifications
    fun updateItemTitle(id: Long, newTitle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val item = scanDao.getScanById(id)
            if (item != null) {
                scanDao.updateScan(item.copy(title = newTitle))
            }
        }
    }

    fun moveItemFolder(id: Long, newFolder: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val item = scanDao.getScanById(id)
            if (item != null) {
                scanDao.updateScan(item.copy(folder = newFolder))
            }
        }
    }

    fun setItemPrivate(id: Long, isPrivate: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val item = scanDao.getScanById(id)
            if (item != null) {
                scanDao.updateScan(item.copy(isPrivate = isPrivate))
            }
        }
    }

    fun deleteScanItem(item: ScanItem) {
        viewModelScope.launch(Dispatchers.IO) {
            item.filePath?.let { File(it).delete() }
            item.thumbnailPath?.let { File(it).delete() }
            scanDao.deleteScan(item)
            if (_selectedDetailScan.value?.id == item.id) {
                withContext(Dispatchers.Main) {
                    _selectedDetailScan.value = null
                }
            }
        }
    }

    fun saveScanItemDirect(item: ScanItem) {
        viewModelScope.launch(Dispatchers.IO) {
            scanDao.insertScan(item)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            scanDao.clearAll()
        }
    }

    fun clearAppData() {
        viewModelScope.launch(Dispatchers.IO) {
            scanDao.clearAll()
            preferences.clearAllPreferences()
            getApplication<Application>().filesDir.deleteRecursively()
        }
    }
}

private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
