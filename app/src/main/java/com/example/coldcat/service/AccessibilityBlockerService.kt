package com.example.coldcat.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.coldcat.data.AppDatabase
import com.example.coldcat.data.BlockSchedule
import com.example.coldcat.ui.screen.BlockedOverlayActivity
import com.example.coldcat.util.BlockOverlayManager
import com.example.coldcat.util.TimeUtils
import kotlinx.coroutines.*

class AccessibilityBlockerService : AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastBlockedPkg: String? = null
    private var lastBlockTime = 0L
    val now = System.currentTimeMillis()

    @Volatile private var blockedPackages = setOf<String>()
    @Volatile private var schedules = listOf<BlockSchedule>()

    companion object {
        var isRunning = false
        private const val TAG = "ColdCat_Accessibility"
    }

    override fun onServiceConnected() {
        isRunning = true
        Log.d(TAG, "Accessibility service connected")

        // Observe blocked apps
        scope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            db.blockDao().getAllBlockedApps().collect { apps ->
                blockedPackages = apps.map { it.packageName }.toSet()
                Log.d(TAG, "Blocked packages updated: $blockedPackages")
            }
        }

        // Observe schedules
        scope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            db.blockDao().getAllSchedules().collect { list ->
                schedules = list
                Log.d(TAG, "Schedules updated: ${list.size} schedules")
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkg = event.packageName?.toString() ?: return

        // Never block our own app, system UI, or the overlay itself
        val ownPackage = applicationContext.packageName
        if (pkg == ownPackage) return
        if (pkg == "com.android.systemui") return
        if (pkg == "com.android.launcher3") return
        if (pkg.startsWith("com.miui")) return          // MIUI system UI
        if (pkg.startsWith("com.android.settings")) return

        if (pkg !in blockedPackages) return
        val isActive = TimeUtils.isAnyScheduleActive(schedules)
        Log.d(TAG, "Window changed: $pkg | schedules=${schedules.size} | blockActive=$isActive | blocked=${pkg in blockedPackages}")

        if (!isActive) return
        Log.d(TAG, "BLOCKING: $pkg")

        // 1. INSTANT ESCAPE from app
        performGlobalAction(GLOBAL_ACTION_HOME)

        val now = System.currentTimeMillis()

        if (pkg == lastBlockedPkg && now - lastBlockTime < 3000) return

        lastBlockedPkg = pkg
        lastBlockTime = now

//        showBlockingOverlay(pkg)
        BlockOverlayManager.show(applicationContext, pkg)

    }

    private fun showBlockingOverlay(blockedPackage: String) {
        val intent = Intent(applicationContext, BlockedOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(BlockedOverlayActivity.EXTRA_BLOCKED_PACKAGE, blockedPackage)
        }
        try {
            applicationContext.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start overlay: ${e.message}")
        }
    }

    override fun onInterrupt() {
        isRunning = false
        Log.d(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        scope.cancel()
        Log.d(TAG, "Accessibility service destroyed")
    }
}