package com.battlecity

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.battlecity.engine.InputState
import com.battlecity.engine.KeyBits

@OptIn(ExperimentalComposeUiApi::class)
fun main() = application {
    val windowState = rememberWindowState(size = DpSize(640.dp, 480.dp))
    val input = remember { mutableStateOf(InputState()) }
    var inputState by input

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Battle City",
        onPreviewKeyEvent = { event ->
            val bit = decodeDesktop(event)
            if (bit == 0) {
                false
            } else {
                val s = inputState
                val newP1 = if (bit in P1_BITS) applyTo(s.p1, bit, event.type == KeyEventType.KeyDown) else s.p1
                val newP2 = if (bit in P2_BITS) applyTo(s.p2, bit, event.type == KeyEventType.KeyDown) else s.p2
                val newSys = if (bit in SYS_BITS) applyTo(s.system, bit, event.type == KeyEventType.KeyDown) else s.system
                inputState = s.copy(p1 = newP1, p2 = newP2, system = newSys)
                false
            }
        },
    ) {
        App(input)
    }
}

private val P1_BITS = setOf(
    KeyBits.UP, KeyBits.DOWN, KeyBits.LEFT, KeyBits.RIGHT, KeyBits.FIRE,
)
private val P2_BITS = setOf(
    KeyBits.UP, KeyBits.DOWN, KeyBits.LEFT, KeyBits.RIGHT, KeyBits.FIRE,
)
private val SYS_BITS = setOf(KeyBits.ENTER, KeyBits.ESC)

private fun applyTo(kb: KeyBits, bit: Int, pressed: Boolean): KeyBits =
    if (pressed) kb.with(bit) else kb.without(bit)

private fun decodeDesktop(event: androidx.compose.ui.input.key.KeyEvent): Int {
    val key = event.key
    return when {
        key == Key.Enter || key == Key.NumPadEnter -> KeyBits.ENTER
        key == Key.Escape -> KeyBits.ESC
        key == Key.DirectionUp -> KeyBits.UP
        key == Key.DirectionDown -> KeyBits.DOWN
        key == Key.DirectionLeft -> KeyBits.LEFT
        key == Key.DirectionRight -> KeyBits.RIGHT
        key == Key.Spacebar -> KeyBits.FIRE
        else -> 0
    }
}
