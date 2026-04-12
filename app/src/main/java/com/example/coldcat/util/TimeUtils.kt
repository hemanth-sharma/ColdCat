package com.example.coldcat.util

import com.example.coldcat.data.BlockSchedule
import java.util.Calendar

object TimeUtils {

    /** Returns current time as minutes from midnight (0..1439) */
    fun currentMinuteOfDay(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    /**
     * Checks whether [currentMinute] falls within the block schedule.
     * Handles overnight ranges (e.g. 22:00 -> 06:00).
     */
    fun isWithinSchedule(schedule: BlockSchedule, currentMinute: Int = currentMinuteOfDay()): Boolean {
        if (!schedule.isEnabled) return false
        val s = schedule.startMinute
        val e = schedule.endMinute
        return if (s <= e) {
            currentMinute in s..e
        } else {
            // Crosses midnight
            currentMinute >= s || currentMinute <= e
        }
    }

    /**
     * Returns true if ANY enabled schedule is currently active.
     */
    fun isAnyScheduleActive(schedules: List<BlockSchedule>): Boolean {
        val now = currentMinuteOfDay()
        return schedules.any { isWithinSchedule(it, now) }
    }

    /**
     * Applies the 1-hour buffer rule:
     * If the requested duration is >= 23 hours (1380 minutes),
     * the end time is adjusted to startMinute - 60 (mod 1440).
     * Returns the (possibly adjusted) endMinute.
     */
    fun applyBufferRule(startMinute: Int, requestedEndMinute: Int): Int {
        val duration = (requestedEndMinute - startMinute + 1440) % 1440
        return if (duration >= 23 * 60) {
            // Cap at 23 hours: end = start - 60
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

    /** Parses "HH:MM" to minutes from midnight */
    fun timeStringToMinute(time: String): Int {
        val parts = time.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    /**
     * Returns a human-readable schedule label e.g. "14:00 → 21:00"
     */
    fun formatSchedule(schedule: BlockSchedule): String {
        return "${minuteToTimeString(schedule.startMinute)} → ${minuteToTimeString(schedule.endMinute)}"
    }

    /**
     * Returns how many minutes remain until the current active schedule ends.
     * Returns -1 if no active schedule.
     */
    fun minutesUntilBlockEnds(schedules: List<BlockSchedule>): Int {
        val now = currentMinuteOfDay()
        val active = schedules.firstOrNull { isWithinSchedule(it, now) } ?: return -1
        val end = active.endMinute
        return if (end >= now) end - now else (1440 - now) + end
    }
}