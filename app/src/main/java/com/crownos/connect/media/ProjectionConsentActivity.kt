package com.crownos.connect.media

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.crownos.connect.service.ForegroundLauncher
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProjectionConsentActivity : ComponentActivity() {
    @Inject lateinit var capture: ScreenCapture
    @Inject lateinit var launcher: ForegroundLauncher

    private val consent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        capture.onConsent(result.resultCode, result.data)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launcher.dismissPrompt()
        if (savedInstanceState == null) {
            consent.launch(getSystemService(MediaProjectionManager::class.java).createScreenCaptureIntent())
        }
    }

    companion object {
        fun intent(context: Context) = Intent(context, ProjectionConsentActivity::class.java)
    }
}
