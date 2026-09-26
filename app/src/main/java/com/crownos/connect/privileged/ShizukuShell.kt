package com.crownos.connect.privileged

import android.util.Log
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuRemoteProcess
import java.lang.reflect.Method
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ShizukuShell"
private const val COMMAND_TIMEOUT_SECONDS = 10L

@Singleton
class ShizukuShell @Inject constructor(private val access: ShizukuAccess) {
    private val newProcess: Method? by lazy {
        runCatching {
            Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java,
            ).apply { isAccessible = true }
        }.onFailure { Log.w(TAG, "Shizuku cannot start processes", it) }.getOrNull()
    }

    fun run(vararg command: String): Boolean {
        if (!access.isReady) return false
        val process = runCatching { newProcess?.invoke(null, command, null, null) as? ShizukuRemoteProcess }
            .onFailure { Log.w(TAG, "starting ${command.firstOrNull()} failed", it) }
            .getOrNull() ?: return false
        return try {
            val finished = process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            finished && process.exitValue() == 0
        } finally {
            process.destroy()
        }
    }
}
