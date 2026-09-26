package com.crownos.connect.util

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

fun PackageManager.applicationInfoOrNull(packageName: String): ApplicationInfo? = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        getApplicationInfo(packageName, 0)
    }
}.getOrNull()

fun PackageManager.labelOf(packageName: String): String =
    applicationInfoOrNull(packageName)?.let { getApplicationLabel(it).toString() } ?: packageName
