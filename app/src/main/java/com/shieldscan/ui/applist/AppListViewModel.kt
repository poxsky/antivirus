package com.shieldscan.ui.applist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shieldscan.ShieldScanApp
import com.shieldscan.data.db.AppEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AppListState(val apps: List<AppEntity> = emptyList(), val filtered: List<AppEntity> = emptyList(), val isLoading: Boolean = true)

class AppListViewModel(app: Application) : AndroidViewModel(app) {
    private val db = (app as ShieldScanApp).db
    private val _state = MutableStateFlow(AppListState())
    val state: StateFlow<AppListState> = _state

    private var all: List<AppEntity> = emptyList()
    private var currentQuery = ""
    private var currentFilter = Filter.ALL

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            // Observe DB
            db.appDao().observeAll().collect { list ->
                all = list.sortedByDescending { it.riskScore }
                applyFilter()
            }
        }
    }

    fun filter(query: String, filter: Filter) {
        currentQuery = query; currentFilter = filter
        applyFilter()
    }

    private fun applyFilter() {
        var filtered = all
        filtered = when(currentFilter) {
            Filter.ALL -> filtered
            Filter.RISKY -> filtered.filter { it.riskLevel in setOf("RISKY","CRITICAL") }
            Filter.SIDELOADED -> filtered.filter { it.isSideloaded }
            Filter.HIDDEN -> filtered.filter { it.isHidden }
            Filter.SYSTEM -> filtered.filter { it.isSystemApp }
        }
        if (currentQuery.isNotBlank()) {
            val q = currentQuery.lowercase()
            filtered = filtered.filter { it.appName.lowercase().contains(q) || it.packageName.lowercase().contains(q) }
        }
        _state.value = AppListState(apps = all, filtered = filtered, isLoading = false)
    }
}
