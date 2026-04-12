package com.example.coldcat

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import com.example.coldcat.data.AppDatabase
import com.example.coldcat.service.BlockEnforcementService

class ColdCatApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // Start the background enforcement service
        val intent = Intent(this, BlockEnforcementService::class.java)
        startForegroundService(intent)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "ColdCat Active",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shown while ColdCat is monitoring your block schedule"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "coldcat_service_channel"
        const val NOTIFICATION_ID = 1001
    }
}