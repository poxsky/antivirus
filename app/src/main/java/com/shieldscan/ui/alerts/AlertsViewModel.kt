package com.shieldscan.ui.alerts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shieldscan.ShieldScanApp
import com.shieldscan.data.db.AlertEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AlertsViewModel(app: Application) : AndroidViewModel(app) {
    private val db = (app as ShieldScanApp).db
    private val _alerts = MutableStateFlow<List<AlertEntity>>(emptyList())
    val alerts: StateFlow<List<AlertEntity>> = _alerts

    init {
        viewModelScope.launch {
            db.alertDao().observeAlerts().collect { _alerts.value = it }
        }
    }

    fun markAllRead() {
        viewModelScope.launch { db.alertDao().markAllRead() }
    }
}
