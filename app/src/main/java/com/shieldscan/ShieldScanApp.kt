package com.shieldscan

import android.app.Application
import androidx.room.Room
import com.shieldscan.data.db.ShieldDatabase
import com.shieldscan.workers.ScanScheduler

class ShieldScanApp : Application() {
    lateinit var db: ShieldDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        db = Room.databaseBuilder(this, ShieldDatabase::class.java, "shieldscan.db")
            .fallbackToDestructiveMigration()
            .build()
        ScanScheduler.ensureScheduled(this)
    }
}
