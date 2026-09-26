package com.crownos.connect

import android.app.Application
import com.crownos.connect.service.AppVisibility
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CrownConnectApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppVisibility.install()
    }
}
