package com.crownos.connect.media

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import com.crownos.connect.protocol.Feature
import com.crownos.connect.runtime.LltsRuntime
import com.crownos.connect.service.ConnectionService
import com.crownos.connect.service.ForegroundLauncher
import com.crownos.connect.util.ScreenGeometry
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ScreenCapture"
private const val DISPLAY_NAME = "CrownConnect"

@Singleton
class ScreenCapture @Inject constructor(
    @ApplicationContext private val context: Context,
    private val runtime: LltsRuntime,
    private val screen: ScreenGeometry,
    private val launcher: ForegroundLauncher,
) {
    private class Session(val stream: MediaStream, var projection: MediaProjection? = null, var display: VirtualDisplay? = null)

    private val projectionManager = context.getSystemService(MediaProjectionManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var waiting: Session? = null
    private var active: Session? = null
    private var consent: Pair<Int, Intent>? = null

    val isCapturing: Boolean get() = active?.projection != null

    val needsForegroundType: Boolean get() = consent != null || isCapturing

    fun start(stream: MediaStream) {
        stop()
        waiting = Session(stream)
        launcher.launch(
            ProjectionConsentActivity.intent(context),
            "Share this screen with your computer?",
            "Tap to allow screen sharing.",
        )
    }

    fun onConsent(resultCode: Int, data: Intent?) {
        val session = waiting
        if (session == null || resultCode != Activity.RESULT_OK || data == null) {
            session?.let { runtime.stopFeature(it.stream.peer, Feature.Mirror) }
            waiting = null
            return
        }
        consent = resultCode to data
        ConnectionService.promoteForProjection(context)
    }

    fun onForegroundReady() = mainHandler.post {
        val session = waiting ?: return@post
        val (resultCode, data) = consent ?: return@post
        consent = null
        waiting = null
        val projection = runCatching { projectionManager.getMediaProjection(resultCode, data) }
            .onFailure { Log.w(TAG, "the projection token was refused", it) }
            .getOrNull()
        val surface = runtime.createEncoderSurface(session.stream.peer, session.stream.index)
        if (projection == null || surface == null) {
            projection?.stop()
            runtime.stopFeature(session.stream.peer, Feature.Mirror)
            return@post
        }
        projection.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                if (active === session) release(session, stopFeature = true)
            }
        }, mainHandler)
        session.projection = projection
        session.display = projection.createVirtualDisplay(
            DISPLAY_NAME,
            session.stream.width,
            session.stream.height,
            (screen.size.density * DisplayMetrics.DENSITY_DEFAULT).toInt(),
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            surface,
            null,
            mainHandler,
        )
        active = session
    }

    fun stop(peer: String? = null) {
        waiting?.takeIf { peer == null || it.stream.peer == peer }?.let { waiting = null }
        active?.takeIf { peer == null || it.stream.peer == peer }?.let { release(it, stopFeature = false) }
    }

    private fun release(session: Session, stopFeature: Boolean) {
        active = null
        session.display?.release()
        session.projection?.stop()
        if (stopFeature) runtime.stopFeature(session.stream.peer, Feature.Mirror)
        runtime.detachSurface(session.stream.peer, session.stream.index)
        ConnectionService.refreshTypesOnly(context)
    }
}
