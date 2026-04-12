package com.example.coldcat.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.coldcat.ColdCatApplication
import com.example.coldcat.MainActivity
import com.example.coldcat.R
import com.example.coldcat.data.AppDatabase
import com.example.coldcat.util.PrefsManager
import com.example.coldcat.util.TimeUtils
import kotlinx.coroutines.*

/**
 * Persistent foreground service that:
 * 1. Runs continuously in background
 * 2. Checks schedule every 30 seconds
 * 3. Updates PrefsManager.blockActive state
 * 4. Starts/stops VPN service based on schedule
 * 5. Updates notification with current state
 */
class BlockEnforcementService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        startForeground(ColdCatApplication.NOTIFICATION_ID, buildNotification(false))
        startMonitoringLoop()
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
                    val isActive = TimeUtils.isAnyScheduleActive(schedules)

                    // Update shared state
                    PrefsManager.setBlockActive(applicationContext, isActive)

                    // Update notification
                    val notification = buildNotification(isActive)
                    val manager = getSystemService(NotificationManager::class.java)
                    manager.notify(ColdCatApplication.NOTIFICATION_ID, notification)

                    // Start/stop VPN based on schedule
                    if (isActive) {
                        startVpnIfNeeded()
                    } else {
                        stopVpnIfRunning()
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }

                delay(30_000L) // Check every 30 seconds
            }
        }
    }

    private fun startVpnIfNeeded() {
        if (!VpnBlockerService.isRunning) {
            val intent = Intent(applicationContext, VpnBlockerService::class.java)
                .apply { action = VpnBlockerService.ACTION_START }
            startService(intent)
        }
    }

    private fun stopVpnIfRunning() {
        if (VpnBlockerService.isRunning) {
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
        // Restart self
        val restartIntent = Intent(applicationContext, BlockEnforcementService::class.java)
        startService(restartIntent)
    }
}