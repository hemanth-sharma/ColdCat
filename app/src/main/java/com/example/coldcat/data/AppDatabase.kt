package com.example.coldcat.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

//
//private const val PREFS = "coldcat_prefs"
//private const val KEY_FIRST_LAUNCH = "first_launch"
//
//fun isFirstLaunch(context: Context): Boolean {
//    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
//    return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
//}
//
//fun setFirstLaunchDone(context: Context) {
//    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
//    prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
//}


@Database(
    entities = [BlockedApp::class, BlockedWebsite::class, BlockSchedule::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blockDao(): BlockDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "coldcat_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}