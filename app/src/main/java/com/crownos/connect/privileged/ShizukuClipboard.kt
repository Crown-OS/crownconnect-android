package com.crownos.connect.privileged

import android.content.ClipData
import android.content.Context
import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import android.os.Process
import com.crownos.connect.state.ClipboardReader
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val SHELL_PACKAGE = "com.android.shell"
private const val DEFAULT_DEVICE_ID = 0
private const val PER_USER_RANGE = 100_000

@Singleton
class ShizukuClipboard @Inject constructor(@ApplicationContext private val context: Context) : ClipboardReader {
    private val userId = Process.myUid() / PER_USER_RANGE
    private var clipboard: Any? = null
    private var listenerProxy: Any? = null

    private fun service(): Any? = clipboard ?: HiddenBinders.systemInterface("clipboard", "android.content.IClipboard")?.also { clipboard = it }

    override fun readText(): String? {
        val clip = service()?.invokeHidden(
            "getPrimaryClip",
            arrayOf(SHELL_PACKAGE, null, userId, DEFAULT_DEVICE_ID),
            arrayOf(SHELL_PACKAGE, null, userId),
            arrayOf(SHELL_PACKAGE, userId),
        )?.getOrNull() as? ClipData ?: return null
        if (clip.itemCount == 0) return null
        return clip.getItemAt(0).coerceToText(context)?.toString()
    }

    fun watch(onChanged: () -> Unit): Boolean {
        val binder: IBinder = object : Binder() {
            override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
                if (code != FIRST_CALL_TRANSACTION) return super.onTransact(code, data, reply, flags)
                onChanged()
                return true
            }
        }
        val proxy = HiddenBinders.callbackProxy("android.content.IOnPrimaryClipChangedListener", binder) ?: return false
        val added = service()?.invokeHidden(
            "addPrimaryClipChangedListener",
            arrayOf(proxy, SHELL_PACKAGE, null, userId, DEFAULT_DEVICE_ID),
            arrayOf(proxy, SHELL_PACKAGE, null, userId),
            arrayOf(proxy, SHELL_PACKAGE, userId),
        )?.isSuccess == true
        if (added) listenerProxy = proxy
        return added
    }

    fun unwatch() {
        val proxy = listenerProxy ?: return
        listenerProxy = null
        service()?.invokeHidden(
            "removePrimaryClipChangedListener",
            arrayOf(proxy, SHELL_PACKAGE, null, userId, DEFAULT_DEVICE_ID),
            arrayOf(proxy, SHELL_PACKAGE, null, userId),
            arrayOf(proxy, SHELL_PACKAGE, userId),
        )
    }

    fun reset() {
        clipboard = null
        listenerProxy = null
    }
}
