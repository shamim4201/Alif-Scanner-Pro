package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ScanType {
    QR_CODE,
    BARCODE,
    DOCUMENT,
    OCR_TEXT,
    PDF_FILE
}

@Entity(tableName = "scan_items")
data class ScanItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val type: ScanType,
    val content: String,
    val subType: String = "", // e.g. "URL", "WIFI", "PHONE", "EMAIL", "EAN_13", "UPC_A", "DOC_PAGES"
    val metadataJson: String = "{}",
    val filePath: String? = null,
    val thumbnailPath: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val folder: String = "General",
    val isPrivate: Boolean = false,
    val isFavorite: Boolean = false,
    val pageCount: Int = 1,
    val fileSizeFormatted: String = ""
)
