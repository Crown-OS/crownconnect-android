package com.crownos.connect.privileged

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import com.crownos.connect.permissions.AppPermission
import com.crownos.connect.permissions.SpecialAccess
import com.crownos.connect.service.CrownNotificationListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class GrantReport(val granted: Int, val failed: List<String>)

@Singleton
class PrivilegedGrants @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shell: ShizukuShell,
) {
    private val packageName get() = context.packageName

    suspend fun grantEverything(): GrantReport = withContext(Dispatchers.IO) {
        val commands = buildList {
            AppPermission.entries.flatMap { it.permissions }.distinct().forEach { add(arrayOf("pm", "grant", packageName, it)) }
            add(arrayOf("appops", "set", packageName, "PROJECT_MEDIA", "allow"))
            add(arrayOf("appops", "set", packageName, "SYSTEM_ALERT_WINDOW", "allow"))
            add(arrayOf("appops", "set", packageName, "RUN_ANY_IN_BACKGROUND", "allow"))
            add(arrayOf("cmd", "notification", "allow_listener", ComponentName(context, CrownNotificationListener::class.java).flattenToString()))
            add(arrayOf("dumpsys", "deviceidle", "whitelist", "+$packageName"))
            enableAccessibilityCommand()?.let { addAll(it) }
        }
        val failed = commands.filterNot { shell.run(*it) }.map { it.joinToString(" ") }
        GrantReport(commands.size - failed.size, failed)
    }

    private fun enableAccessibilityCommand(): List<Array<String>>? {
        val component = SpecialAccess.accessibilityComponent(context).flattenToString()
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        if (enabled.split(':').any { it.equals(component, ignoreCase = true) }) return null
        val updated = listOf(enabled, component).filter { it.isNotBlank() }.joinToString(":")
        return listOf(
            arrayOf("settings", "put", "secure", Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, updated),
            arrayOf("settings", "put", "secure", Settings.Secure.ACCESSIBILITY_ENABLED, "1"),
        )
    }
}
