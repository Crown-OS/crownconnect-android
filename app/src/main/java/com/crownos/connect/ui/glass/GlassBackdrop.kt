package com.crownos.connect.ui.glass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.Modifier.Node as ModifierNode

@Stable
class GlassBackdrop {
    internal var layer: GraphicsLayer? by mutableStateOf(null)
    internal var positionInRoot: Offset by mutableStateOf(Offset.Zero)
}

@Composable
fun rememberGlassBackdrop(): GlassBackdrop = remember { GlassBackdrop() }

fun Modifier.glassSource(backdrop: GlassBackdrop): Modifier = this then GlassSourceElement(backdrop)

private data class GlassSourceElement(val backdrop: GlassBackdrop) : ModifierNodeElement<GlassSourceNode>() {
    override fun create() = GlassSourceNode(backdrop)
    override fun update(node: GlassSourceNode) {
        node.backdrop = backdrop
    }
}

private class GlassSourceNode(var backdrop: GlassBackdrop) :
    ModifierNode(), DrawModifierNode, LayoutAwareModifierNode {

    private var layer: GraphicsLayer? = null

    override fun onAttach() {
        layer = requireGraphicsContext().createGraphicsLayer().also { backdrop.layer = it }
    }

    override fun onDetach() {
        layer?.let(requireGraphicsContext()::releaseGraphicsLayer)
        if (backdrop.layer === layer) backdrop.layer = null
        layer = null
    }

    override fun onPlaced(coordinates: LayoutCoordinates) {
        backdrop.positionInRoot = coordinates.positionInRoot()
    }

    override fun ContentDrawScope.draw() {
        val recorded = layer ?: return drawContent()
        recorded.record { this@draw.drawContent() }
        drawLayer(recorded)
    }
}

val LocalRootBackdrop = staticCompositionLocalOf<GlassBackdrop?> { null }
