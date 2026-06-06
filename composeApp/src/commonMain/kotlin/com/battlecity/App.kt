package com.battlecity

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import com.battlecity.clock.nowMs
import com.battlecity.engine.GameConfig
import com.battlecity.engine.GameEngine
import com.battlecity.engine.GamePhase
import com.battlecity.engine.GameState
import com.battlecity.engine.InputState
import com.battlecity.engine.KeyBits
import com.battlecity.input.VirtualPad
import com.battlecity.input.bindKeyboard
import com.battlecity.render.SpriteAtlas
import com.battlecity.render.drawGame
import com.battlecity.render.drawSplash
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Top-level app composable.
 *
 * The engine + renderer are platform-agnostic. The only platform-specific
 * concerns are: how [InputState] is filled (desktop uses keyboard,
 * mobile uses [VirtualPad]), and how the actual PNG sprites are decoded.
 * For the moment we ship colour-coded placeholder rectangles (see
 * [SpriteAtlas]); the PNG loading path can be wired in later via
 * `org.jetbrains.compose.resources.imageResource`.
 */
@Composable
fun App() {
    val atlas = remember { SpriteAtlas() }
    val state = remember { GameState() }
    val input = remember { mutableStateOf(InputState()) }
    val engine = remember { GameEngine(levelLoader = { _ -> defaultMap() }) }

    LaunchedEffect(Unit) {
        engine.reset(state)
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(Color.DarkGray),
    ) {
        val isCompact = maxWidth < 600.dp

        LaunchedEffect(Unit) {
            while (isActive) {
                val now = nowMs()
                engine.update(state, input.value, now)
                val s = input.value
                if (s.system.raw != 0) {
                    if (KeyBits.ENTER in s.system && state.phase == GamePhase.SPLASH) {
                        engine.reset(state)
                    }
                    input.value = s.copy(system = KeyBits.NONE)
                }
                delay(33L)
            }
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .bindKeyboard(input, isP1 = true),
            ) {
                val sx = size.width / GameConfig.SCREEN_W
                val sy = size.height / GameConfig.SCREEN_H
                val sc = minOf(sx, sy)
                val tx = (size.width - GameConfig.SCREEN_W * sc) / 2f
                val ty = (size.height - GameConfig.SCREEN_H * sc) / 2f
                translate(tx, ty) {
                    scale(sc, sc, pivot = Offset(0f, 0f)) {
                        when (state.phase) {
                            GamePhase.SPLASH -> drawSplash(atlas)
                            else -> drawGame(state, atlas)
                        }
                    }
                }
            }

            if (isCompact) {
                VirtualPad(input)
            }
        }
    }
}

private fun defaultMap(): ByteArray = ByteArray(GameConfig.PLANE_H * GameConfig.PLANE_W) { 0 }
