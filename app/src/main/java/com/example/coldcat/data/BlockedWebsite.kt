package com.example.coldcat.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_websites")
data class BlockedWebsite(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val domain: String,       // e.g. "youtube.com"
    val addedAt: Long = System.currentTimeMillis()
)