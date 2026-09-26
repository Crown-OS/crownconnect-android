package com.crownos.connect.ui.screens.permissions

import android.content.Context
import androidx.lifecycle.ViewModel
import com.crownos.connect.discovery.DiscoveryCoordinator
import com.crownos.connect.permissions.AppPermission
import com.crownos.connect.service.ConnectionService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PermissionsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val discovery: DiscoveryCoordinator,
) : ViewModel() {
    private val grantedState = MutableStateFlow(snapshot())
    val granted: StateFlow<Set<AppPermission>> = grantedState.asStateFlow()

    fun refresh() {
        val current = snapshot()
        if (current == grantedState.value) return
        grantedState.value = current
        discovery.onPermissionsChanged()
        ConnectionService.refreshForegroundTypes(context)
    }

    private fun snapshot(): Set<AppPermission> = AppPermission.entries.filter { it.applies && it.isGranted(context) }.toSet()
}
