package com.crownos.connect.privileged

import android.os.Binder
import android.os.Build
import android.os.Parcel
import android.os.ResultReceiver
import android.util.Log
import com.crownos.connect.state.HotspotController
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ShizukuHotspot"
private const val SHELL_PACKAGE = "com.android.shell"
private const val TETHERING_WIFI = 0
private const val CONNECTIVITY_SCOPE_GLOBAL = 1

@Singleton
class ShizukuHotspot @Inject constructor() : HotspotController {
    override fun setEnabled(enabled: Boolean): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) tetheringModule(enabled) else connectivityService(enabled)

    override fun ssid(): String? {
        val wifi = HiddenBinders.systemInterface("wifi", "android.net.wifi.IWifiManager") ?: return null
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val configuration = wifi.invokeHidden("getSoftApConfiguration", emptyArray()).getOrNull() ?: return null
                configuration.javaClass.getMethod("getSsid").invoke(configuration) as? String
            } else {
                val configuration = wifi.invokeHidden("getWifiApConfiguration", emptyArray()).getOrNull() ?: return null
                configuration.javaClass.getField("SSID").get(configuration) as? String
            }
        }.getOrNull()?.removeSurrounding("\"")
    }

    private fun tetheringModule(enabled: Boolean): Boolean {
        val connector = HiddenBinders.systemInterface("tethering", "android.net.ITetheringConnector") ?: return false
        val listener = HiddenBinders.callbackProxy("android.net.IIntResultListener", resultBinder()) ?: return false
        val outcome = if (enabled) {
            val request = tetheringRequest() ?: return false
            connector.invokeHidden(
                "startTethering",
                arrayOf(request, SHELL_PACKAGE, null, listener),
                arrayOf(request, SHELL_PACKAGE, listener),
            )
        } else {
            connector.invokeHidden(
                "stopTethering",
                arrayOf(TETHERING_WIFI, SHELL_PACKAGE, null, listener),
                arrayOf(TETHERING_WIFI, SHELL_PACKAGE, listener),
            )
        }
        return outcome.isSuccess
    }

    private fun connectivityService(enabled: Boolean): Boolean {
        val connectivity = HiddenBinders.systemInterface("connectivity", "android.net.IConnectivityManager") ?: return false
        val outcome = if (enabled) {
            connectivity.invokeHidden("startTethering", arrayOf(TETHERING_WIFI, ResultReceiver(null), false, SHELL_PACKAGE))
        } else {
            connectivity.invokeHidden("stopTethering", arrayOf(TETHERING_WIFI, SHELL_PACKAGE))
        }
        return outcome.isSuccess
    }

    private fun tetheringRequest(): Any? = runCatching {
        val type = Class.forName("android.net.TetheringRequestParcel")
        type.getDeclaredConstructor().newInstance().also { request ->
            type.getField("tetheringType").setInt(request, TETHERING_WIFI)
            type.getField("exemptFromEntitlementCheck").setBoolean(request, true)
            type.getField("showProvisioningUi").setBoolean(request, false)
            runCatching { type.getField("connectivityScope").setInt(request, CONNECTIVITY_SCOPE_GLOBAL) }
        }
    }.onFailure { Log.w(TAG, "the tethering request cannot be built", it) }.getOrNull()

    private fun resultBinder() = object : Binder() {
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            if (code != FIRST_CALL_TRANSACTION) return super.onTransact(code, data, reply, flags)
            Log.i(TAG, "tethering answered")
            return true
        }
    }
}
