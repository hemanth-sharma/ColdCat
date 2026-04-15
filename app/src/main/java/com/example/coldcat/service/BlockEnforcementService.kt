package com.example.coldcat.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.coldcat.ColdCatApplication
import com.example.coldcat.MainActivity
import com.example.coldcat.data.AppDatabase
import com.example.coldcat.util.PrefsManager
import com.example.coldcat.util.TimeUtils
import kotlinx.coroutines.*

class BlockEnforcementService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "ColdCat_Enforcement"
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(ColdCatApplication.NOTIFICATION_ID, buildNotification(false))
        startMonitoringLoop()
        Log.d(TAG, "BlockEnforcementService started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startMonitoringLoop() {
        scope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            while (isActive) {
                try {
                    val schedules = db.blockDao().getAllSchedulesOnce()
                    // FIXED: Only active if schedules exist AND time matches
                    val isActive = schedules.isNotEmpty() && TimeUtils.isAnyScheduleActive(schedules)

                    PrefsManager.setBlockActive(applicationContext, isActive)
                    Log.d(TAG, "Schedule check: ${schedules.size} schedules, blockActive=$isActive, time=${TimeUtils.currentMinuteOfDay()}")

                    val notification = buildNotification(isActive)
                    val manager = getSystemService(NotificationManager::class.java)
                    manager.notify(ColdCatApplication.NOTIFICATION_ID, notification)

                    // Manage VPN service
                    if (isActive) {
                        startVpnIfNeeded()
                    } else {
                        stopVpnIfRunning()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Monitor loop error: ${e.message}")
                }
                delay(30_000L)
            }
        }
    }

    private fun startVpnIfNeeded() {
        if (!VpnBlockerService.isRunning) {
            Log.d(TAG, "Starting VPN service")
            val intent = Intent(applicationContext, VpnBlockerService::class.java)
                .apply { action = VpnBlockerService.ACTION_START }
            startService(intent)
        }
    }

    private fun stopVpnIfRunning() {
        if (VpnBlockerService.isRunning) {
            Log.d(TAG, "Stopping VPN service")
            val intent = Intent(applicationContext, VpnBlockerService::class.java)
                .apply { action = VpnBlockerService.ACTION_STOP }
            startService(intent)
        }
    }

    private fun buildNotification(blockActive: Boolean): Notification {
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0,
            Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val statusText = if (blockActive) "🔒 Block active — stay focused!" else "✅ Monitoring schedule..."
        return NotificationCompat.Builder(applicationContext, ColdCatApplication.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("ColdCat")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        Log.d(TAG, "BlockEnforcementService destroyed — restarting")
        startService(Intent(applicationContext, BlockEnforcementService::class.java))
    }
}