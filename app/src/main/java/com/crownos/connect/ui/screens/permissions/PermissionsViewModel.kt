package com.crownos.connect.ui.screens.permissions

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crownos.connect.input.InputInjectors
import com.crownos.connect.privileged.GrantReport
import com.crownos.connect.privileged.PrivilegedGrants
import com.crownos.connect.privileged.ShizukuAccess
import com.crownos.connect.discovery.DiscoveryCoordinator
import com.crownos.connect.permissions.AppPermission
import com.crownos.connect.permissions.SpecialAccess
import com.crownos.connect.service.ConnectionService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SHIZUKU_DOWNLOAD = "https://shizuku.rikka.app/download/"

@HiltViewModel
class PermissionsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val discovery: DiscoveryCoordinator,
    private val shizuku: ShizukuAccess,
    private val grants: PrivilegedGrants,
    private val injectors: InputInjectors,
) : ViewModel() {
    val shizukuStatus = shizuku.status

    private val grantReportState = MutableStateFlow<GrantReport?>(null)
    val grantReport: StateFlow<GrantReport?> = grantReportState.asStateFlow()

    private val grantingState = MutableStateFlow(false)
    val granting: StateFlow<Boolean> = grantingState.asStateFlow()

    val remoteControl: String
        get() = when {
            shizuku.isReady -> "Shizuku"
            injectors.canInject -> "Accessibility"
            else -> "Unavailable"
        }

    fun requestShizuku() = shizuku.requestPermission()

    fun openShizuku() = shizuku.launchManager()

    fun installShizuku() = context.startActivity(
        Intent(Intent.ACTION_VIEW, SHIZUKU_DOWNLOAD.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )

    fun grantEverything() {
        if (grantingState.value) return
        grantingState.value = true
        viewModelScope.launch {
            grantReportState.value = grants.grantEverything()
            grantingState.value = false
            refresh()
        }
    }

    private val grantedState = MutableStateFlow(snapshot())
    val granted: StateFlow<Set<AppPermission>> = grantedState.asStateFlow()

    private val specialState = MutableStateFlow(specialSnapshot())
    val special: StateFlow<Set<SpecialAccess>> = specialState.asStateFlow()

    fun refresh() {
        shizuku.refresh()
        specialState.value = specialSnapshot()
        val current = snapshot()
        if (current == grantedState.value) return
        grantedState.value = current
        discovery.onPermissionsChanged()
        ConnectionService.refreshForegroundTypes(context)
    }

    fun open(access: SpecialAccess) = context.startActivity(access.settingsIntent(context))

    private fun snapshot(): Set<AppPermission> = AppPermission.entries.filter { it.applies && it.isGranted(context) }.toSet()

    private fun specialSnapshot(): Set<SpecialAccess> = SpecialAccess.entries.filter { it.isGranted(context) }.toSet()
}
