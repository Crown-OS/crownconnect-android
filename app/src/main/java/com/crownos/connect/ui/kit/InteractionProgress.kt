package com.crownos.connect.ui.kit

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.Stable
import com.crownos.connect.ui.theme.CrownMotion

@Stable
class InteractionProgress(
    private val hoverState: State<Float>,
    private val pressState: State<Float>,
    private val focusState: State<Float>,
) {
    val hover: Float get() = hoverState.value
    val press: Float get() = pressState.value
    val focus: Float get() = focusState.value
}

@Composable
fun rememberInteractionProgress(
    source: InteractionSource,
    enabled: Boolean = true,
    hoverSpec: AnimationSpec<Float> = CrownMotion.Snappy,
    pressSpec: AnimationSpec<Float> = CrownMotion.Crisp,
    focusSpec: AnimationSpec<Float> = CrownMotion.Snappy,
): InteractionProgress {
    val hovered = source.collectIsHoveredAsState().value && enabled
    val pressed = source.collectIsPressedAsState().value && enabled
    val focused = source.collectIsFocusedAsState().value && enabled
    return InteractionProgress(
        animateFloatAsState(if (hovered) 1f else 0f, hoverSpec, label = "hover"),
        animateFloatAsState(if (pressed) 1f else 0f, pressSpec, label = "press"),
        animateFloatAsState(if (focused) 1f else 0f, focusSpec, label = "focus"),
    )
}

@Composable
fun animateSnappy(target: Float): State<Float> = animateFloatAsState(target, CrownMotion.Snappy, label = "snappy")

@Composable
fun animateCrisp(target: Float): State<Float> = animateFloatAsState(target, CrownMotion.Crisp, label = "crisp")

fun Boolean.toProgress(): Float = if (this) 1f else 0f
