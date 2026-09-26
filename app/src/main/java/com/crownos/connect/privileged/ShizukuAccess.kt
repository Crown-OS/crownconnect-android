package com.crownos.connect.privileged

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.crownos.connect.util.applicationInfoOrNull
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import javax.inject.Inject
import javax.inject.Singleton

private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
private const val PERMISSION_REQUEST_CODE = 4747
private const val ROOT_UID = 0

sealed interface ShizukuStatus {
    data object NotInstalled : ShizukuStatus
    data object NotRunning : ShizukuStatus
    data object NeedsPermission : ShizukuStatus
    data class Ready(val asRoot: Boolean) : ShizukuStatus
}

@Singleton
class ShizukuAccess @Inject constructor(@ApplicationContext private val context: Context) {
    private val statusState = MutableStateFlow(currentStatus())
    val status: StateFlow<ShizukuStatus> = statusState.asStateFlow()

    val isReady: Boolean get() = statusState.value is ShizukuStatus.Ready

    init {
        HiddenApiBypass.addHiddenApiExemptions("L")
        Shizuku.addBinderReceivedListenerSticky { refresh() }
        Shizuku.addBinderDeadListener { refresh() }
        Shizuku.addRequestPermissionResultListener { _, _ -> refresh() }
    }

    fun refresh() {
        statusState.value = currentStatus()
    }

    fun requestPermission() {
        if (Shizuku.pingBinder() && !Shizuku.isPreV11()) Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
    }

    fun launchManager() {
        context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ?.let(context::startActivity)
    }

    private fun currentStatus(): ShizukuStatus = when {
        !Shizuku.pingBinder() -> if (context.packageManager.applicationInfoOrNull(SHIZUKU_PACKAGE) == null) {
            ShizukuStatus.NotInstalled
        } else {
            ShizukuStatus.NotRunning
        }
        Shizuku.isPreV11() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED -> ShizukuStatus.NeedsPermission
        else -> ShizukuStatus.Ready(asRoot = Shizuku.getUid() == ROOT_UID)
    }
}
