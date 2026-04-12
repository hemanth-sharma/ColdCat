package com.example.coldcat.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class InstalledApp(
    val packageName: String,
    val appName: String,
    val icon: Drawable?
)

object AppUtils {

    /** Returns all user-installed apps (excludes system apps), sorted by name. */
    fun getInstalledUserApps(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { appInfo ->
                // Keep only user-installed apps, exclude ColdCat itself
                (appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0) &&
                        appInfo.packageName != context.packageName
            }
            .map { appInfo ->
                InstalledApp(
                    packageName = appInfo.packageName,
                    appName = pm.getApplicationLabel(appInfo).toString(),
                    icon = try { pm.getApplicationIcon(appInfo.packageName) } catch (e: Exception) { null }
                )
            }
            .sortedBy { it.appName }
    }

    fun getAppName(context: Context, packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    fun getAppIcon(context: Context, packageName: String): Drawable? {
        return try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null
        }
    }
}