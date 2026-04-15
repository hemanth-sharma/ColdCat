package com.example.coldcat.ui

import android.app.Application
import android.content.Context
import android.net.VpnService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.coldcat.data.*
import com.example.coldcat.util.AppUtils
import com.example.coldcat.util.InstalledApp
import com.example.coldcat.util.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db  = AppDatabase.getInstance(application)
    private val dao = db.blockDao()

    val blockedApps: StateFlow<List<BlockedApp>> = dao.getAllBlockedApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blockedWebsites: StateFlow<List<BlockedWebsite>> = dao.getAllBlockedWebsites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val schedules: StateFlow<List<BlockSchedule>> = dao.getAllSchedules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * FIXED: Only active when:
     *  1. There is at least one schedule in the DB
     *  2. At least one enabled schedule covers the current time
     */
    val isBlockActive: StateFlow<Boolean> = schedules.map { list ->
        if (list.isEmpty()) false
        else TimeUtils.isAnyScheduleActive(list)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Installed apps
    private val _installedApps  = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    fun loadInstalledApps(context: Context) {
        viewModelScope.launch {
            _isLoadingApps.value = true
            _installedApps.value = withContext(Dispatchers.IO) {
                AppUtils.getInstalledUserApps(context)
            }
            _isLoadingApps.value = false
        }
    }

    fun addBlockedApp(app: BlockedApp) {
        viewModelScope.launch {
            if (!isBlockActive.value) dao.insertBlockedApp(app)
        }
    }

    fun removeBlockedApp(app: BlockedApp) {
        viewModelScope.launch {
            if (!isBlockActive.value) dao.deleteBlockedApp(app)
        }
    }

    fun addBlockedWebsite(domain: String) {
        viewModelScope.launch {
            if (!isBlockActive.value) {
                val normalized = normalizeDomain(domain)
                if (normalized.isNotBlank() && dao.isWebsiteBlocked(normalized) == 0) {
                    dao.insertBlockedWebsite(BlockedWebsite(domain = normalized))
                }
            }
        }
    }

    fun removeBlockedWebsite(site: BlockedWebsite) {
        viewModelScope.launch {
            if (!isBlockActive.value) dao.deleteBlockedWebsite(site)
        }
    }

    fun addSchedule(startMinute: Int, endMinute: Int, label: String = "") {
        viewModelScope.launch {
            if (!isBlockActive.value) {
                val adjustedEnd = TimeUtils.applyBufferRule(startMinute, endMinute)
                dao.insertSchedule(BlockSchedule(startMinute = startMinute, endMinute = adjustedEnd, label = label))
            }
        }
    }

    fun deleteSchedule(schedule: BlockSchedule) {
        viewModelScope.launch {
            if (!isBlockActive.value) dao.deleteSchedule(schedule)
        }
    }

    fun toggleSchedule(schedule: BlockSchedule) {
        viewModelScope.launch {
            // Allow toggling even during active block so user can disable a schedule
            dao.updateSchedule(schedule.copy(isEnabled = !schedule.isEnabled))
        }
    }

    private fun normalizeDomain(domain: String): String =
        domain.lowercase()
            .removePrefix("https://").removePrefix("http://").removePrefix("www.")
            .trimEnd('/').split("/")[0].trim()
}