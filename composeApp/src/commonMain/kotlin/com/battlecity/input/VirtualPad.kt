package com.battlecity.input

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.battlecity.engine.InputState
import com.battlecity.engine.KeyBits

/**
 * Bottom-of-screen virtual d-pad + fire button. The d-pad is a single
 * `pointerInput` region that reads the local touch coordinates relative to
 * the pad centre to determine which direction (if any) is active. The fire
 * button is a separate press-and-hold area.
 */
@Composable
fun VirtualPad(input: MutableState<InputState>, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .alpha(0.65f),
    ) {
        val padSize = minOf(maxWidth, maxHeight) * 0.28f
        val fireSize = padSize * 0.8f
        val padOffsetX = 24.dp
        val padOffsetY = 24.dp
        val fireOffset = 24.dp

        // D-pad area (left bottom).
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = padOffsetX, bottom = padOffsetY)
                .size(padSize)
                .background(Color.DarkGray.copy(alpha = 0.5f), CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { off ->
                            updateDir(input, off, this.size.toFloatSize())
                        },
                        onDragEnd = { clearDir(input) },
                        onDragCancel = { clearDir(input) },
                        onDrag = { change, _ ->
                            updateDir(input, change.position, this.size.toFloatSize())
                        },
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            // On a quick tap we keep the last direction
                            // pressed (so tap-and-release still feels
                            // responsive but doesn't fire continuously).
                            val released = tryAwaitRelease()
                            if (released) clearDir(input)
                        },
                    )
                },
        )

        // Fire button (right bottom).
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = fireOffset, bottom = padOffsetY)
                .size(fireSize)
                .background(Color.Red.copy(alpha = 0.5f), CircleShape)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.any {
                                it.pressed && it.position.x in 0f..size.width.toFloat() &&
                                    it.position.y in 0f..size.height.toFloat()
                            }
                            setFire(input, pressed)
                        }
                    }
                },
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(fireSize * 0.5f)
                    .background(Color.White.copy(alpha = 0.7f), CircleShape),
            )
        }
    }
}

private fun androidx.compose.ui.unit.IntSize.toFloatSize(): Size = Size(width.toFloat(), height.toFloat())

private fun updateDir(
    input: MutableState<InputState>,
    touch: Offset,
    area: Size,
) {
    val cx = area.width / 2f
    val cy = area.height / 2f
    val dx = touch.x - cx
    val dy = touch.y - cy
    val r = (kotlin.math.sqrt(dx * dx + dy * dy)).coerceAtLeast(1f)
    val nx = dx / r
    val ny = dy / r
    val threshold = 0.4f
    val up = ny < -threshold
    val down = ny > threshold
    val left = nx < -threshold
    val right = nx > threshold
    val raw = (if (up) KeyBits.UP else 0) or
        (if (down) KeyBits.DOWN else 0) or
        (if (left) KeyBits.LEFT else 0) or
        (if (right) KeyBits.RIGHT else 0)
    val s = input.value
    input.value = s.copy(p1 = KeyBits(raw))
}

private fun clearDir(input: MutableState<InputState>) {
    val s = input.value
    if (s.p1.raw and KeyBits.DIR_MASK == 0) return
    input.value = s.copy(p1 = KeyBits(s.p1.raw and KeyBits.DIR_MASK.inv()))
}

private fun setFire(input: MutableState<InputState>, pressed: Boolean) {
    val s = input.value
    val newP1 = if (pressed) s.p1.with(KeyBits.FIRE) else s.p1.without(KeyBits.FIRE)
    if (newP1 == s.p1) return
    input.value = s.copy(p1 = newP1)
}
