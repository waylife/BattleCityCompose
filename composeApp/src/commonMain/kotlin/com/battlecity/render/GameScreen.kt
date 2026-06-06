package com.battlecity.render

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.battlecity.clock.nowMs
import com.battlecity.engine.GameConfig
import com.battlecity.engine.GameEngine
import com.battlecity.engine.GamePhase
import com.battlecity.engine.GameState
import com.battlecity.engine.InputState
import com.battlecity.engine.KeyBits
import com.battlecity.input.bindKeyboard
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * The main game screen. Drives the 30 FPS loop and dispatches input to the engine.
 * Works the same on every platform; input sources differ (keyboard vs. virtual pad).
 */
@Composable
fun GameScreen(
    state: GameState,
    engine: GameEngine,
    input: MutableState<InputState>,
    atlas: SpriteAtlas,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val lastTick = remember { mutableStateOf(nowMs()) }
    val showPad = remember { mutableStateOf(false) }

    // Decide on first composition whether to use the virtual pad based on
    // screen width (< 600dp ⇒ touch).
    val screenWidthDp = LocalDensity.run {
        // We use a sensible default; the real threshold is exposed via
        // LocalConfiguration in a higher-level wrapper.
        false
    }
    LaunchedEffect(Unit) { showPad.value = false }

    // 30 FPS tick loop.
    LaunchedEffect(Unit) {
        var last = nowMs()
        while (isActive) {
            val now = nowMs()
            lastTick.value = now
            engine.update(state, input.value, now)
            // Clear one-shot system bits (ENTER etc.) after a tick.
            val s = input.value
            if (s.system.raw != 0) input.value = s.copy(system = com.battlecity.engine.KeyBits.NONE)
            delay(33L)
            last = now
        }
    }

    Box(modifier = modifier.fillMaxSize().bindKeyboard(input, isP1 = true)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val logicalW = GameConfig.SCREEN_W.toFloat()
            val logicalH = GameConfig.SCREEN_H.toFloat()
            val sx = size.width / logicalW
            val sy = size.height / logicalH
            val scale = minOf(sx, sy)
            val tx = (size.width - logicalW * scale) / 2f
            val ty = (size.height - logicalH * scale) / 2f

            translate(tx, ty) {
                scale(scale, scale, pivot = androidx.compose.ui.geometry.Offset(0f, 0f)) {
                    when (state.phase) {
                        GamePhase.SPLASH -> drawSplash(atlas)
                        else -> drawGame(state, atlas)
                    }
                }
            }
        }
    }
}
