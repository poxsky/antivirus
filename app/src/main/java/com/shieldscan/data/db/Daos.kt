package com.shieldscan.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM apps ORDER BY riskScore DESC")
    fun observeAll(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps ORDER BY riskScore DESC")
    suspend fun getAll(): List<AppEntity>

    @Query("SELECT * FROM apps WHERE packageName = :pkg LIMIT 1")
    suspend fun getByPackage(pkg: String): AppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(apps: List<AppEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(app: AppEntity)

    @Query("DELETE FROM apps WHERE packageName NOT IN (:currentPkgs)")
    suspend fun pruneMissing(currentPkgs: List<String>)

    @Query("SELECT COUNT(*) FROM apps")
    suspend fun count(): Int
}

@Dao
interface ScanHistoryDao {
    @Query("SELECT * FROM scan_history ORDER BY scannedAt DESC LIMIT 50")
    fun observeHistory(): Flow<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scan_history ORDER BY scannedAt DESC LIMIT 1")
    suspend fun getLast(): ScanHistoryEntity?

    @Insert
    suspend fun insert(entity: ScanHistoryEntity)
}

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts ORDER BY createdAt DESC")
    fun observeAlerts(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts ORDER BY createdAt DESC")
    suspend fun getAll(): List<AlertEntity>

    @Insert
    suspend fun insert(alert: AlertEntity)

    @Query("UPDATE alerts SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: Long)

    @Query("UPDATE alerts SET isRead = 1")
    suspend fun markAllRead()

    @Query("DELETE FROM alerts")
    suspend fun clearAll()
}
