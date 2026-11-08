package com.example.database

import androidx.room.TypeConverter
import com.example.model.ScanType

class Converters {
    @TypeConverter
    fun fromScanType(type: ScanType): String {
        return type.name
    }

    @TypeConverter
    fun toScanType(value: String): ScanType {
        return try {
            ScanType.valueOf(value)
        } catch (e: Exception) {
            ScanType.QR_CODE
        }
    }
}
