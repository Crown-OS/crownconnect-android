package com.crownos.connect.media

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import java.util.concurrent.atomic.AtomicBoolean

private const val PAIRING_URI_PREFIX = "llts1:"

class QrCodeAnalyzer(private val onPairingUri: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val reader = QRCodeReader()
    private val hints = mapOf(
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        DecodeHintType.TRY_HARDER to true,
    )
    private val delivered = AtomicBoolean(false)
    private var luminance = ByteArray(0)

    override fun analyze(image: ImageProxy) {
        image.use { frame ->
            if (delivered.get()) return
            val plane = frame.planes.firstOrNull() ?: return
            val width = frame.width
            val height = frame.height
            val rowStride = plane.rowStride
            val needed = rowStride * height
            if (luminance.size < needed) luminance = ByteArray(needed)
            val buffer = plane.buffer
            buffer.rewind()
            buffer.get(luminance, 0, minOf(needed, buffer.remaining()))
            val source = PlanarYUVLuminanceSource(luminance, rowStride, height, 0, 0, width, height, false)
            val text = try {
                reader.decode(BinaryBitmap(HybridBinarizer(source)), hints).text
            } catch (_: NotFoundException) {
                null
            } catch (_: ReaderException) {
                null
            } finally {
                reader.reset()
            }
            if (text != null && text.startsWith(PAIRING_URI_PREFIX, ignoreCase = true) && delivered.compareAndSet(false, true)) {
                onPairingUri(text)
            }
        }
    }

    fun rearm() = delivered.set(false)
}
