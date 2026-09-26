package com.crownos.connect.ui.kit

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.crownos.connect.ui.theme.CrownTheme

const val ICON_STROKE = 1.8f

@Composable
fun LucideIcon(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    strokeWidth: Float = ICON_STROKE,
    color: Color = CrownTheme.palette.text.icon,
    contentDescription: String? = null,
) {
    val source = ImageVector.vectorResource(icon)
    val restroked = remember(source, strokeWidth) { source.withStrokeWidth(strokeWidth) }
    Image(
        painter = rememberVectorPainter(restroked),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        colorFilter = ColorFilter.tint(color),
    )
}

private fun ImageVector.withStrokeWidth(strokeWidth: Float): ImageVector =
    ImageVector.Builder(name, defaultWidth, defaultHeight, viewportWidth, viewportHeight, tintColor, tintBlendMode)
        .apply { copyGroup(root, strokeWidth) }
        .build()

private fun ImageVector.Builder.copyGroup(group: VectorGroup, strokeWidth: Float) {
    group.forEach { node ->
        when (node) {
            is VectorPath -> addPath(
                pathData = node.pathData,
                pathFillType = node.pathFillType,
                name = node.name,
                fill = node.fill,
                fillAlpha = node.fillAlpha,
                stroke = node.stroke,
                strokeAlpha = node.strokeAlpha,
                strokeLineWidth = if (node.stroke != null) strokeWidth else node.strokeLineWidth,
                strokeLineCap = node.strokeLineCap,
                strokeLineJoin = node.strokeLineJoin,
                strokeLineMiter = node.strokeLineMiter,
            )
            is VectorGroup -> {
                addGroup(
                    node.name, node.rotation, node.pivotX, node.pivotY, node.scaleX, node.scaleY,
                    node.translationX, node.translationY, node.clipPathData,
                )
                copyGroup(node, strokeWidth)
                clearGroup()
            }
        }
    }
}
