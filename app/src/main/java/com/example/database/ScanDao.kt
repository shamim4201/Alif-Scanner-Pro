package com.example.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.ScanItem
import com.example.model.ScanType
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Query("SELECT * FROM scan_items WHERE isPrivate = 0 ORDER BY timestamp DESC")
    fun getAllPublicScans(): Flow<List<ScanItem>>

    @Query("SELECT * FROM scan_items WHERE isPrivate = 1 ORDER BY timestamp DESC")
    fun getPrivateScans(): Flow<List<ScanItem>>

    @Query("SELECT * FROM scan_items WHERE isPrivate = 0 AND type = :type ORDER BY timestamp DESC")
    fun getScansByType(type: ScanType): Flow<List<ScanItem>>

    @Query("SELECT * FROM scan_items WHERE isPrivate = 0 AND folder = :folder ORDER BY timestamp DESC")
    fun getScansByFolder(folder: String): Flow<List<ScanItem>>

    @Query("SELECT DISTINCT folder FROM scan_items WHERE isPrivate = 0")
    fun getAllFolders(): Flow<List<String>>

    @Query("SELECT * FROM scan_items WHERE isPrivate = 0 AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') ORDER BY timestamp DESC")
    fun searchScans(query: String): Flow<List<ScanItem>>

    @Query("SELECT * FROM scan_items WHERE id = :id LIMIT 1")
    suspend fun getScanById(id: Long): ScanItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(item: ScanItem): Long

    @Update
    suspend fun updateScan(item: ScanItem)

    @Delete
    suspend fun deleteScan(item: ScanItem)

    @Query("DELETE FROM scan_items WHERE id = :id")
    suspend fun deleteScanById(id: Long)

    @Query("DELETE FROM scan_items WHERE isPrivate = 0")
    suspend fun clearPublicHistory()

    @Query("DELETE FROM scan_items")
    suspend fun clearAll()
}
