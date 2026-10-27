package com.shieldscan.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [AppEntity::class, ScanHistoryEntity::class, AlertEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ShieldDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun historyDao(): ScanHistoryDao
    abstract fun alertDao(): AlertDao
}
