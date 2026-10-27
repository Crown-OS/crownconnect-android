package com.crownos.connect.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.crownos.connect.settings.AppPreferences
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var preferences: AppPreferences

    override fun onReceive(context: Context, intent: Intent) {
        val restart = intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        if (restart && preferences.onboarded.value) ConnectionService.start(context)
    }
}
