package com.crownos.connect.ui.glass

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.crownos.connect.ui.theme.CrownTheme
import com.crownos.connect.util.scaleAlpha
import kotlin.math.ceil
import androidx.compose.ui.Modifier.Node as ModifierNode

enum class GlassStyle { Chrome, Sheet, Clear }

enum class GlassBorder { Outline, Bottom }

@Immutable
internal data class GlassPaint(
    val tintTop: Color,
    val tintBottom: Color,
    val highlight: Color,
    val border: Color,
    val opaqueFallback: Color,
)

private val BLUR_RADIUS = 30.dp
private val RIM_WIDTH = 2.dp

@Composable
fun Modifier.glass(
    backdrop: GlassBackdrop,
    cornerRadius: Dp,
    style: GlassStyle = GlassStyle.Chrome,
    border: GlassBorder = GlassBorder.Outline,
): Modifier {
    val palette = CrownTheme.palette
    val paint = when (style) {
        GlassStyle.Chrome -> GlassPaint(
            palette.glass.tintTop, palette.glass.tintBottom, palette.glass.highlight, palette.glass.border,
            palette.popover.background,
        )
        GlassStyle.Sheet -> GlassPaint(
            palette.popover.background.scaleAlpha(0.72f), palette.popover.background.scaleAlpha(0.78f),
            palette.glass.highlight, palette.popover.border, palette.popover.background,
        )
        GlassStyle.Clear -> GlassPaint(
            Color.Transparent, Color.Transparent, palette.glass.highlight, palette.glass.border,
            palette.popover.background.scaleAlpha(0.6f),
        )
    }
    val density = LocalDensity.current
    val metrics = with(density) {
        GlassMetrics(cornerRadius.toPx(), RIM_WIDTH.toPx(), BLUR_RADIUS.toPx(), 1.dp.toPx())
    }
    return this then GlassElement(backdrop, paint, metrics, border)
}

internal data class GlassMetrics(val cornerRadius: Float, val rim: Float, val blurRadius: Float, val hairline: Float)

private data class GlassElement(
    val backdrop: GlassBackdrop,
    val paint: GlassPaint,
    val metrics: GlassMetrics,
    val border: GlassBorder,
) : ModifierNodeElement<GlassNode>() {
    override fun create() = GlassNode(backdrop, paint, metrics, border)
    override fun update(node: GlassNode) {
        node.backdrop = backdrop
        node.paint = paint
        node.metrics = metrics
        node.border = border
        node.invalidateDraw()
    }
}

private class GlassNode(
    var backdrop: GlassBackdrop,
    var paint: GlassPaint,
    var metrics: GlassMetrics,
    var border: GlassBorder,
) : ModifierNode(), DrawModifierNode, LayoutAwareModifierNode {

    private var blurLayer: GraphicsLayer? = null
    private var appliedGeometry: GlassGeometry? = null
    private var positionInRoot = Offset.Zero
    private val shapePath = Path()

    override fun onAttach() {
        if (GlassEffect.supportsBlur) blurLayer = requireGraphicsContext().createGraphicsLayer()
    }

    override fun onDetach() {
        blurLayer?.let(requireGraphicsContext()::releaseGraphicsLayer)
        blurLayer = null
        appliedGeometry = null
    }

    override fun onPlaced(coordinates: LayoutCoordinates) {
        val position = coordinates.positionInRoot()
        if (position != positionInRoot) {
            positionInRoot = position
            invalidateDraw()
        }
    }

    override fun ContentDrawScope.draw() {
        val radius = metrics.cornerRadius.coerceAtMost(size.minDimension / 2)
        shapePath.reset()
        shapePath.addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(radius)))

        val source = backdrop.layer
        val blurred = blurLayer
        if (source != null && blurred != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            drawBlurredBackdrop(source, blurred, radius)
        } else {
            drawPath(shapePath, paint.opaqueFallback)
        }
        drawPath(shapePath, Brush.verticalGradient(listOf(paint.tintTop, paint.tintBottom)))
        drawHighlightAndBorder(radius)
        drawContent()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun ContentDrawScope.drawBlurredBackdrop(source: GraphicsLayer, blurred: GraphicsLayer, radius: Float) {
        val padding = ceil(metrics.blurRadius)
        val geometry = GlassGeometry(size.width, size.height, padding, radius, metrics.rim, metrics.blurRadius)
        if (geometry != appliedGeometry) {
            blurred.renderEffect = GlassEffect.create(geometry).asComposeRenderEffect()
            appliedGeometry = geometry
        }
        val offset = positionInRoot - backdrop.positionInRoot
        val recordSize = IntSize(ceil(size.width + padding * 2).toInt(), ceil(size.height + padding * 2).toInt())
        blurred.record(recordSize) {
            translate(padding - offset.x, padding - offset.y) { drawLayer(source) }
        }
        clipPath(shapePath) {
            translate(-padding, -padding) { drawLayer(blurred) }
        }
    }

    private fun ContentDrawScope.drawHighlightAndBorder(radius: Float) {
        val hairline = metrics.hairline
        val inset = hairline / 2
        if (border == GlassBorder.Bottom) {
            drawLine(paint.border, Offset(0f, size.height - inset), Offset(size.width, size.height - inset), strokeWidth = hairline)
            return
        }
        clipPath(shapePath) {
            drawLine(
                paint.highlight,
                Offset(radius, inset),
                Offset(size.width - radius, inset),
                strokeWidth = hairline,
            )
        }
        drawRoundRect(
            color = paint.border,
            topLeft = Offset(inset, inset),
            size = Size(size.width - hairline, size.height - hairline),
            cornerRadius = CornerRadius((radius - inset).coerceAtLeast(0f)),
            style = Stroke(hairline),
        )
    }
}
