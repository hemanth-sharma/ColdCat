package com.example.coldcat.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.coldcat.data.AppDatabase
import com.example.coldcat.ui.screen.BlockedOverlayActivity
import com.example.coldcat.util.TimeUtils
import kotlinx.coroutines.*

class AccessibilityBlockerService : AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var blockedPackages = setOf<String>()
    private var schedules = listOf<com.example.coldcat.data.BlockSchedule>()

    companion object {
        var isRunning = false
    }

    override fun onServiceConnected() {
        isRunning = true
        // Load blocked data and keep it updated
        scope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            db.blockDao().getAllBlockedApps().collect { apps ->
                blockedPackages = apps.map { it.packageName }.toSet()
            }
        }
        scope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            db.blockDao().getAllSchedules().collect { list ->
                schedules = list
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkg = event.packageName?.toString() ?: return

        // Don't block our own app or system UI
        if (pkg == applicationContext.packageName) return
        if (pkg == "com.android.systemui") return
        if (pkg == BlockedOverlayActivity.PACKAGE_MARKER) return

        // Check if a block is active
        if (!TimeUtils.isAnyScheduleActive(schedules)) return

        // Check if this package is blocked
        if (pkg in blockedPackages) {
            showBlockingOverlay(pkg)
        }
    }

    private fun showBlockingOverlay(blockedPackage: String) {
        val intent = Intent(applicationContext, BlockedOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(BlockedOverlayActivity.EXTRA_BLOCKED_PACKAGE, blockedPackage)
        }
        applicationContext.startActivity(intent)
    }

    override fun onInterrupt() {
        isRunning = false
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        scope.cancel()
    }
}