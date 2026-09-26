package com.crownos.connect.permissions

import android.Manifest
import android.content.Context
import android.os.Build
import com.crownos.connect.ui.kit.CrownIcons
import com.crownos.connect.util.Permissions

enum class AppPermission(val title: String, val description: String, val icon: Int, val permissions: List<String>) {
    NearbyDevices(
        "Nearby devices",
        "Find your computers over Bluetooth and stay reachable",
        CrownIcons.Bluetooth,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        },
    ),
    Notifications(
        "Notifications",
        "Show pairing codes and requests from your computers",
        CrownIcons.Bell,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) listOf(Manifest.permission.POST_NOTIFICATIONS) else emptyList(),
    ),
    Camera("Camera", "Scan pairing codes and work as a webcam", CrownIcons.Webcam, listOf(Manifest.permission.CAMERA)),
    Microphone("Microphone", "Work as a microphone for your computer", CrownIcons.Mic, listOf(Manifest.permission.RECORD_AUDIO)),
    Phone(
        "Calls and contacts",
        "Answer, decline and place calls from your computer",
        CrownIcons.Phone,
        listOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS,
        ),
    );

    val applies: Boolean get() = permissions.isNotEmpty()

    fun isGranted(context: Context): Boolean = permissions.all { Permissions.granted(context, it) }

    companion object {
        val essential = listOf(NearbyDevices, Notifications, Camera)
    }
}
