package com.battlecity.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameStateMachineTest {
    @Test
    fun resetStartsActive() {
        val state = GameState()
        val engine = GameEngine(levelLoader = { _ -> ByteArray(169) })
        engine.reset(state)
        assertEquals(GamePhase.ACTIVE, state.phase)
        assertEquals(1, state.level)
        assertEquals(GameConfig.PLAYER_START_LIVES, state.players[0].lives)
    }

    @Test
    fun escFromActiveGoesBackToSplash() {
        val state = GameState()
        val engine = GameEngine(levelLoader = { _ -> ByteArray(169) })
        engine.reset(state)
        engine.update(state, InputState(system = KeyBits(KeyBits.ESC)))
        assertEquals(GamePhase.SPLASH, state.phase)
    }

    @Test
    fun bulletMovesOutOfBounds() {
        val state = GameState()
        val engine = GameEngine(levelLoader = { _ -> ByteArray(169) })
        engine.reset(state)
        val p = state.players[0]
        // Manually spawn a bullet moving UP at the top of the plane.
        p.bullets[0].apply {
            active = true
            x = 100
            y = 0
            dir = Direction.UP
            speed = 3.0f
        }
        // Run the engine for a few frames; bullet should clear out.
        repeat(120) {
            engine.update(state, InputState(), 0L)
        }
        // After 120 frames, the bullet should have either deactivated or be clamped.
        assertEquals(false, p.bullets[0].active && p.bullets[0].y < 0)
    }
}
