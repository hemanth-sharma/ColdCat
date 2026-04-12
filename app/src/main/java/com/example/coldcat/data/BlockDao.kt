package com.example.coldcat.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockDao {

    // --- Blocked Apps ---
    @Query("SELECT * FROM blocked_apps ORDER BY appName ASC")
    fun getAllBlockedApps(): Flow<List<BlockedApp>>

    @Query("SELECT * FROM blocked_apps ORDER BY appName ASC")
    suspend fun getAllBlockedAppsOnce(): List<BlockedApp>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedApp(app: BlockedApp)

    @Delete
    suspend fun deleteBlockedApp(app: BlockedApp)

    @Query("SELECT COUNT(*) FROM blocked_apps WHERE packageName = :pkg")
    suspend fun isAppBlocked(pkg: String): Int

    // --- Blocked Websites ---
    @Query("SELECT * FROM blocked_websites ORDER BY domain ASC")
    fun getAllBlockedWebsites(): Flow<List<BlockedWebsite>>

    @Query("SELECT * FROM blocked_websites ORDER BY domain ASC")
    suspend fun getAllBlockedWebsitesOnce(): List<BlockedWebsite>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedWebsite(site: BlockedWebsite)

    @Delete
    suspend fun deleteBlockedWebsite(site: BlockedWebsite)

    @Query("SELECT COUNT(*) FROM blocked_websites WHERE domain = :domain")
    suspend fun isWebsiteBlocked(domain: String): Int

    // --- Block Schedules ---
    @Query("SELECT * FROM block_schedules ORDER BY startMinute ASC")
    fun getAllSchedules(): Flow<List<BlockSchedule>>

    @Query("SELECT * FROM block_schedules ORDER BY startMinute ASC")
    suspend fun getAllSchedulesOnce(): List<BlockSchedule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: BlockSchedule)

    @Update
    suspend fun updateSchedule(schedule: BlockSchedule)

    @Delete
    suspend fun deleteSchedule(schedule: BlockSchedule)
}