package com.example.coldcat.util

import android.util.Log
import com.example.coldcat.data.BlockSchedule
import java.util.Calendar

object TimeUtils {

    private const val TAG = "ColdCat_TimeUtils"

    /** Returns current time as minutes from midnight (0..1439) */
    fun currentMinuteOfDay(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    /**
     * Returns true if [currentMinute] falls inside the schedule's active window.
     * Handles overnight ranges (e.g. 22:00 → 06:00).
     */
    fun isWithinSchedule(schedule: BlockSchedule, currentMinute: Int = currentMinuteOfDay()): Boolean {
        if (!schedule.isEnabled) return false
        val s = schedule.startMinute
        val e = schedule.endMinute

        val result = if (s == e) {
            // Zero-duration schedule — never active
            false
        } else if (s < e) {
            // Normal range: e.g. 09:00 (540) → 17:00 (1020)
            currentMinute in s until e
        } else {
            // Overnight range: e.g. 22:00 (1320) → 06:00 (360)
            currentMinute >= s || currentMinute < e
        }

        Log.d(TAG, "isWithinSchedule: start=$s end=$e now=$currentMinute → $result")
        return result
    }

    /**
     * Returns true if ANY enabled schedule covers the current time.
     * IMPORTANT: Returns false if the list is empty.
     */
    fun isAnyScheduleActive(schedules: List<BlockSchedule>): Boolean {
        if (schedules.isEmpty()) return false
        val now = currentMinuteOfDay()
        return schedules.any { isWithinSchedule(it, now) }
    }

    /**
     * 1-hour buffer rule:
     * If the requested duration is >= 23 hours, cap end at (start - 60 mod 1440).
     */
    fun applyBufferRule(startMinute: Int, requestedEndMinute: Int): Int {
        val duration = (requestedEndMinute - startMinute + 1440) % 1440
        return if (duration >= 23 * 60) {
            (startMinute - 60 + 1440) % 1440
        } else {
            requestedEndMinute
        }
    }

    /** Formats minutes-from-midnight as "HH:MM" */
    fun minuteToTimeString(minute: Int): String {
        val h = minute / 60
        val m = minute % 60
        return "%02d:%02d".format(h, m)
    }

    /** "HH:MM" → minutes from midnight */
    fun timeStringToMinute(time: String): Int {
        val parts = time.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    /** Human-readable schedule: "14:00 → 21:00" */
    fun formatSchedule(schedule: BlockSchedule): String =
        "${minuteToTimeString(schedule.startMinute)} → ${minuteToTimeString(schedule.endMinute)}"

    /**
     * Minutes remaining until the active schedule ends.
     * Returns -1 if no schedule is currently active.
     */
    fun minutesUntilBlockEnds(schedules: List<BlockSchedule>): Int {
        if (schedules.isEmpty()) return -1
        val now = currentMinuteOfDay()
        val active = schedules.firstOrNull { isWithinSchedule(it, now) } ?: return -1
        val end = active.endMinute
        return if (end > now) end - now else (1440 - now) + end
    }
}