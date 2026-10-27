package com.crownos.connect.media

import android.os.Build
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.crownos.connect.runtime.LltsRuntime
import com.crownos.connect.ui.kit.ButtonSize
import com.crownos.connect.ui.kit.ButtonVariant
import com.crownos.connect.ui.kit.CrownButton
import com.crownos.connect.ui.kit.CrownIcons
import com.crownos.connect.ui.kit.drawHairlineBorder
import com.crownos.connect.ui.theme.CrownTheme
import com.crownos.connect.ui.theme.CrownType

private const val TYPING_SENTINEL = " "

@Composable
fun RemoteDisplayView(stream: MediaStream, peerName: String, runtime: LltsRuntime, onClose: () -> Unit) {
    val sender = remember(stream.key) { RemoteInputSender(runtime, stream.peer) }
    val focus = remember { FocusRequester() }
    var toolbarOpen by remember { mutableStateOf(false) }
    var typing by remember { mutableStateOf(false) }
    val aspect = if (stream.width > 0 && stream.height > 0) stream.width.toFloat() / stream.height else 16f / 9f

    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .aspectRatio(aspect)
                .focusRequester(focus)
                .focusable()
                .onKeyEvent { key ->
                    val pressed = when (key.type) {
                        KeyEventType.KeyDown -> true
                        KeyEventType.KeyUp -> false
                        else -> return@onKeyEvent false
                    }
                    sender.onKey(key.key.nativeKeyCode, pressed)
                }
                .pointerInput(stream.key) {
                    awaitPointerEventScope {
                        while (true) sender.onPointer(awaitPointerEvent(), size)
                    }
                },
        ) {
            DecoderSurface(stream, runtime)
        }
        LaunchedEffect(Unit) { focus.requestFocus() }

        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(top = 8.dp), contentAlignment = Alignment.TopCenter) {
            DisplayToolbar(
                open = toolbarOpen,
                title = peerName,
                onToggle = { toolbarOpen = !toolbarOpen },
                onKeyboard = { typing = !typing },
                onClose = onClose,
            )
        }
        if (typing) KeyboardCapture(sender) { typing = false }
    }
}

@Composable
private fun DecoderSurface(stream: MediaStream, runtime: LltsRuntime) {
    AndroidView(
        factory = { context ->
            SurfaceView(context).apply {
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            holder.surface.setFrameRate(stream.fps.toFloat(), Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE)
                        }
                        runtime.attachDecoderSurface(stream.peer, stream.index, holder.surface)
                    }

                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

                    override fun surfaceDestroyed(holder: SurfaceHolder) = runtime.detachSurface(stream.peer, stream.index)
                })
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun DisplayToolbar(open: Boolean, title: String, onToggle: () -> Unit, onKeyboard: () -> Unit, onClose: () -> Unit) {
    val glass = CrownTheme.palette.glass
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .drawBehind {
                val radius = 16.dp.toPx()
                drawRoundRect(Brush.verticalGradient(listOf(glass.tintTop, glass.tintBottom)), cornerRadius = CornerRadius(radius))
                drawHairlineBorder(glass.border, radius)
            }
            .clickable(remember { MutableInteractionSource() }, null, onClick = onToggle)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!open) {
            Box(Modifier.size(width = 36.dp, height = 4.dp).background(glass.label.copy(alpha = 0.5f), shape))
            return@Row
        }
        BasicText(title, Modifier.padding(horizontal = 6.dp), style = CrownType.body.copy(color = glass.label), maxLines = 1)
        CrownButton(null, onKeyboard, variant = ButtonVariant.Ghost, size = ButtonSize.Small, icon = CrownIcons.Keyboard)
        CrownButton("Disconnect", onClose, variant = ButtonVariant.Destructive, size = ButtonSize.Small, icon = CrownIcons.X)
    }
}

@Composable
private fun KeyboardCapture(sender: RemoteInputSender, onDone: () -> Unit) {
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var value by remember { mutableStateOf(TextFieldValue(TYPING_SENTINEL)) }
    BasicTextField(
        value = value,
        onValueChange = { next ->
            val typed = next.text
            when {
                typed.length < TYPING_SENTINEL.length -> sender.backspace()
                typed.length > TYPING_SENTINEL.length -> sender.type(typed.drop(TYPING_SENTINEL.length))
            }
            value = TextFieldValue(TYPING_SENTINEL, TextRange(TYPING_SENTINEL.length))
        },
        modifier = Modifier.size(1.dp).alpha(0f).focusRequester(focus),
    )
    LaunchedEffect(Unit) {
        focus.requestFocus()
        keyboard?.show()
    }
    BackHandler(onBack = onDone)
}
