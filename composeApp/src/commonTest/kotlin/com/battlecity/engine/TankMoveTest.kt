package com.battlecity.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TankMoveTest {
    @Test
    fun lowSpeedAccumulatesAcrossFrames() {
        val t = Tank().apply {
            x = 100
            y = 100
            dir = Direction.UP
            speed = 0.5f
        }
        // Step 4 times: total 4 * 0.5 = 2 px, but only 2 of the 4 steps produce
        // an integer movement (steps 2 and 4 in this case).
        repeat(4) { t.moveStep() }
        val totalDrop = 100 - t.y
        assertEquals(2, totalDrop, "low speed 0.5 should produce 2 px over 4 frames")
        assertTrue(t.speedRem in 0f..0.99f, "rem should be in [0, 1)")
    }

    @Test
    fun changeDirectionLanesUp() {
        val t = Tank().apply { x = 100; y = 100; dir = Direction.UP }
        t.changeDirection(Direction.LEFT)
        assertEquals(Direction.LEFT, t.dir)
        // y should snap to one of {2, 18, 34} offset within a tile row.
        val offset = t.y % GameConfig.TILE_H
        assertTrue(offset in listOf(2, 18, 34), "y offset = $offset not in {2,18,34}")
    }

    @Test
    fun blockedAtLeftBoundary() {
        val t = Tank().apply {
            x = 0
            y = 100
            dir = Direction.LEFT
            speed = 2.0f
        }
        val moved = t.moveStep()
        assertEquals(0, t.x)
        assertEquals(false, moved)
    }

    @Test
    fun sweptHitTestIgnoresStationary() {
        val a = Tank().apply { x = 100; y = 100; dir = Direction.UP; active = true }
        val b = Tank().apply { x = 200; y = 200; active = true }
        assertEquals(false, a.hitTest(b, 100, 100))
    }
}
