package com.example.coldcat.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a time-based block schedule.
 * startMinute and endMinute are minutes from midnight (0..1439).
 * e.g. 14:00 = 840, 21:00 = 1260
 *
 * If endMinute < startMinute, the schedule crosses midnight.
 *
 * The 1-hour buffer is enforced in logic:
 * - If (endMinute - startMinute + 1440) % 1440 >= 1380 (23h or more),
 *   endMinute is auto-adjusted to startMinute - 60 (mod 1440).
 */
@Entity(tableName = "block_schedules")
data class BlockSchedule(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val label: String = "",
    val startMinute: Int,   // 0..1439
    val endMinute: Int,     // 0..1439
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)