package com.github.kr328.clash.common.util

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import com.github.kr328.clash.common.constants.Intents

open class PackageUtil {
    @Suppress("DEPRECATION", "QueryPermissionsNeeded")
    companion object {
        fun getPackageInfo(packageManager: PackageManager, flags: Int = 0): PackageInfo {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
            else packageManager.getPackageInfo(packageName, flags)
        }

        fun getInstalledPackages(packageManager: PackageManager, flags: Int = PackageManager.GET_META_DATA): List<PackageInfo> {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                packageManager.getInstalledPackages(flags)
            }
        }

        fun queryIntentActivities(packageManager: PackageManager, flags: Int = 0): List<ResolveInfo> {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(
                    Intent(Intents.ACTION_PROVIDE_URL),
                    PackageManager.ResolveInfoFlags.of(flags.toLong()))
            } else {
                packageManager.queryIntentActivities(Intent(Intents.ACTION_PROVIDE_URL), flags)
            }
        }

        fun getApplicationInfo(packageManager: PackageManager, flags: Int = PackageManager.GET_META_DATA): ApplicationInfo {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(flags.toLong()))
            } else {
                packageManager.getApplicationInfo(packageName, flags)
            }
        }
    }
}