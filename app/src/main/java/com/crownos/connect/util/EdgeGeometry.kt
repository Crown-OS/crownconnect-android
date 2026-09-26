package com.crownos.connect.util

import com.crownos.connect.protocol.EDGE_POSITION_MAX
import com.crownos.connect.protocol.Edge
import kotlin.math.roundToInt

data class ScreenPoint(val x: Float, val y: Float)

object EdgeGeometry {
    private const val ENTRY_INSET_FRACTION = 0.005f

    fun fractionOf(position: Int): Float = position.coerceIn(0, EDGE_POSITION_MAX).toFloat() / EDGE_POSITION_MAX

    fun positionOf(fraction: Float): Int = (fraction.coerceIn(0f, 1f) * EDGE_POSITION_MAX).roundToInt()

    fun entryPoint(edge: Edge, fraction: Float, width: Int, height: Int): ScreenPoint {
        val insetX = width * ENTRY_INSET_FRACTION
        val insetY = height * ENTRY_INSET_FRACTION
        return when (edge) {
            Edge.Left -> ScreenPoint(insetX, fraction * (height - 1))
            Edge.Right -> ScreenPoint(width - 1 - insetX, fraction * (height - 1))
            Edge.Top -> ScreenPoint(fraction * (width - 1), insetY)
            Edge.Bottom -> ScreenPoint(fraction * (width - 1), height - 1 - insetY)
        }
    }

    fun crossed(edge: Edge, point: ScreenPoint, width: Int, height: Int): Boolean = when (edge) {
        Edge.Left -> point.x < 0f
        Edge.Right -> point.x > width - 1f
        Edge.Top -> point.y < 0f
        Edge.Bottom -> point.y > height - 1f
    }

    fun fractionAlong(edge: Edge, point: ScreenPoint, width: Int, height: Int): Float = when (edge) {
        Edge.Left, Edge.Right -> point.y / (height - 1).coerceAtLeast(1)
        Edge.Top, Edge.Bottom -> point.x / (width - 1).coerceAtLeast(1)
    }.coerceIn(0f, 1f)

    fun clamp(point: ScreenPoint, width: Int, height: Int) =
        ScreenPoint(point.x.coerceIn(0f, width - 1f), point.y.coerceIn(0f, height - 1f))
}

data class Letterbox(val left: Float, val top: Float, val width: Float, val height: Float) {
    fun toScreen(normalizedX: Float, normalizedY: Float, frameWidth: Int, frameHeight: Int): ScreenPoint {
        val frameX = normalizedX.coerceIn(0f, 1f) * frameWidth
        val frameY = normalizedY.coerceIn(0f, 1f) * frameHeight
        return ScreenPoint(((frameX - left) / width).coerceIn(0f, 1f), ((frameY - top) / height).coerceIn(0f, 1f))
    }

    companion object {
        fun fit(contentWidth: Int, contentHeight: Int, frameWidth: Int, frameHeight: Int): Letterbox {
            val scale = minOf(frameWidth.toFloat() / contentWidth, frameHeight.toFloat() / contentHeight)
            val width = contentWidth * scale
            val height = contentHeight * scale
            return Letterbox((frameWidth - width) / 2, (frameHeight - height) / 2, width, height)
        }
    }
}
