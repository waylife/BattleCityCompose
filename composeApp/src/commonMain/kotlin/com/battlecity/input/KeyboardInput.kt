package com.battlecity.input

import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import com.battlecity.engine.InputState
import com.battlecity.engine.KeyBits

/**
 * Modifier that maps Compose [KeyEvent]s to the engine's [InputState].
 *
 * When [isP1] is `true` (default) the modifier binds P1's bit-set: arrow
 * keys for direction, space for fire, and shares Enter/Esc on the system
 * channel. Set [isP1] = `false` to bind P2 (WASD + Enter) instead.
 */
fun Modifier.bindKeyboard(
    input: MutableState<InputState>,
    isP1: Boolean = true,
): Modifier = this.onKeyEvent { event ->
    val bit = decode(event, p1 = isP1)
    if (bit == 0) {
        false
    } else {
        applyBit(input, bit, pressed = event.type == KeyEventType.KeyDown)
        false
    }
}

private fun applyBit(input: MutableState<InputState>, bit: Int, pressed: Boolean) {
    val s = input.value
    val newP1 = if (bit in P1_BITS) applyTo(s.p1, bit, pressed) else s.p1
    val newP2 = if (bit in P2_BITS) applyTo(s.p2, bit, pressed) else s.p2
    val newSys = if (bit in SYS_BITS) applyTo(s.system, bit, pressed) else s.system
    input.value = s.copy(p1 = newP1, p2 = newP2, system = newSys)
}

private fun applyTo(kb: KeyBits, bit: Int, pressed: Boolean): KeyBits =
    if (pressed) kb.with(bit) else kb.without(bit)

private val P1_BITS = setOf(
    KeyBits.UP, KeyBits.DOWN, KeyBits.LEFT, KeyBits.RIGHT, KeyBits.FIRE,
)
private val P2_BITS = setOf(
    KeyBits.UP, KeyBits.DOWN, KeyBits.LEFT, KeyBits.RIGHT, KeyBits.FIRE,
)
private val SYS_BITS = setOf(KeyBits.ENTER, KeyBits.ESC)

private fun decode(event: KeyEvent, p1: Boolean): Int {
    val key = event.key
    return when {
        // System keys — shared.
        key == Key.Enter || key == Key.NumPadEnter -> KeyBits.ENTER
        key == Key.Escape -> KeyBits.ESC
        // P1: arrow keys + space.
        p1 && key == Key.DirectionUp -> KeyBits.UP
        p1 && key == Key.DirectionDown -> KeyBits.DOWN
        p1 && key == Key.DirectionLeft -> KeyBits.LEFT
        p1 && key == Key.DirectionRight -> KeyBits.RIGHT
        // P2: WASD + space.
        !p1 && key == Key.W -> KeyBits.UP
        !p1 && key == Key.S -> KeyBits.DOWN
        !p1 && key == Key.A -> KeyBits.LEFT
        !p1 && key == Key.D -> KeyBits.RIGHT
        // Fire: space for P1, right-shift for P2 (UEFI used right-shift).
        key == Key.Spacebar -> KeyBits.FIRE
        else -> 0
    }
}
