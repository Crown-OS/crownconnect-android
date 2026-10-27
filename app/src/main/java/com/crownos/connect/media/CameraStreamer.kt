package com.crownos.connect.media

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.MediaCodec
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Surface
import com.crownos.connect.protocol.CameraLens
import com.crownos.connect.protocol.Feature
import com.crownos.connect.protocol.FeatureRequest
import com.crownos.connect.runtime.LltsRuntime
import com.crownos.connect.util.CaptureSizes
import com.crownos.connect.util.Permissions
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

private const val TAG = "CameraStreamer"

@Singleton
class CameraStreamer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val runtime: LltsRuntime,
) {
    private val cameras = context.getSystemService(CameraManager::class.java)
    private val thread = HandlerThread("crownconnect-camera").apply { start() }
    private val handler = Handler(thread.looper)
    private val executor = Executor { handler.post(it) }
    private var stream: MediaStream? = null
    private var device: CameraDevice? = null
    private var session: CameraCaptureSession? = null
    private var scaler: SurfaceScaler? = null

    val isStreaming: Boolean get() = device != null

    @SuppressLint("MissingPermission")
    fun start(target: MediaStream) = handler.post {
        closeCamera()
        if (!Permissions.granted(context, Manifest.permission.CAMERA)) return@post fail(target, "camera permission is missing")
        val lens = (target.request as? FeatureRequest.Camera)?.lens ?: CameraLens.Back
        val cameraId = cameraFor(lens) ?: return@post fail(target, "no $lens camera")
        val encoderSurface = runtime.createEncoderSurface(target.peer, target.index) ?: return@post fail(target, "no encoder surface")
        stream = target
        val surface = cameraOutput(cameraId, encoderSurface, Size(target.width, target.height))
            ?: return@post fail(target, "the camera cannot be scaled to ${target.width}x${target.height}")
        runCatching {
            cameras.openCamera(cameraId, executor, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    device = camera
                    configure(camera, cameraId, surface, target)
                }

                override fun onDisconnected(camera: CameraDevice) = stopFrom(camera, target)

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.w(TAG, "the camera failed: $error")
                    stopFrom(camera, target)
                }
            })
        }.onFailure { fail(target, "opening the camera failed: ${it.message}") }
    }

    fun stop(peer: String? = null) = handler.post {
        if (peer == null || stream?.peer == peer) closeCamera()
    }

    private fun configure(camera: CameraDevice, cameraId: String, surface: Surface, target: MediaStream) {
        val output = OutputConfiguration(surface)
        val configuration = SessionConfiguration(
            SessionConfiguration.SESSION_REGULAR,
            listOf(output),
            executor,
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(configured: CameraCaptureSession) {
                    session = configured
                    val request = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                        addTarget(surface)
                        fpsRange(cameraId, target.fps)?.let { set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, it) }
                        set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF)
                        set(CaptureRequest.NOISE_REDUCTION_MODE, CameraMetadata.NOISE_REDUCTION_MODE_FAST)
                        set(CaptureRequest.EDGE_MODE, CameraMetadata.EDGE_MODE_FAST)
                    }.build()
                    runCatching { configured.setRepeatingRequest(request, null, handler) }
                        .onFailure { fail(target, "the camera refused the capture request") }
                }

                override fun onConfigureFailed(failed: CameraCaptureSession) = fail(target, "the camera refused its output surface")
            },
        )
        runCatching { camera.createCaptureSession(configuration) }.onFailure { fail(target, "the capture session failed") }
    }

    private fun cameraOutput(cameraId: String, encoderSurface: Surface, wanted: Size): Surface? {
        val sizes = cameras.getCameraCharacteristics(cameraId).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            ?: return encoderSurface
        if (wanted in sizes.getOutputSizes(MediaCodec::class.java).orEmpty()) return encoderSurface
        val source = CaptureSizes.closest(sizes.getOutputSizes(SurfaceTexture::class.java).orEmpty().asList(), wanted) ?: return null
        val created = SurfaceScaler(source, wanted).also { scaler = it }
        return runCatching { created.start(encoderSurface, handler) }
            .onFailure { Log.w(TAG, "scaling the camera on the GPU failed", it) }
            .getOrNull()
    }

    private fun cameraFor(lens: CameraLens): String? {
        val facing = if (lens == CameraLens.Front) CameraCharacteristics.LENS_FACING_FRONT else CameraCharacteristics.LENS_FACING_BACK
        return cameras.cameraIdList.firstOrNull { cameras.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) == facing }
    }

    private fun fpsRange(cameraId: String, fps: Int): Range<Int>? =
        cameras.getCameraCharacteristics(cameraId)
            .get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
            ?.minWithOrNull(compareBy<Range<Int>>({ abs(it.upper - fps) }, { it.upper - it.lower }).thenByDescending { it.lower })

    private fun stopFrom(camera: CameraDevice, target: MediaStream) {
        camera.close()
        if (device === camera) {
            device = null
            session = null
            runtime.stopFeature(target.peer, Feature.Camera)
        }
    }

    private fun fail(target: MediaStream, reason: String) {
        Log.w(TAG, reason)
        closeCamera()
        runtime.stopFeature(target.peer, Feature.Camera)
    }

    private fun closeCamera() {
        session?.close()
        session = null
        device?.close()
        device = null
        scaler?.release()
        scaler = null
        stream?.let { runtime.detachSurface(it.peer, it.index) }
        stream = null
    }
}
