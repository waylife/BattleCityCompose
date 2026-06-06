package com.battlecity.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class TileHitTestTest {
    @Test
    fun bulletDownOnBrickClearsMask() {
        val plane = MapLoader.parse(ByteArray(169))
        plane.map[5][5].kind = TileKind.BRICK
        plane.map[5][5].mask = 0xFFFF

        // Bullet centred in row 5, going DOWN, destroyConcrete=false.
        val bx = 5 * GameConfig.TILE_W + 16
        val by = 5 * GameConfig.TILE_H
        val kind = plane.hitSurface(bx, by, 8, 8, Direction.DOWN, destroyConcrete = false)
        assertEquals(TileKind.BRICK, kind)
        // After destruction the mask should be smaller than 0xFFFF.
        val after = plane.map[5][5].mask
        assertEquals(true, after < 0xFFFF, "mask should shrink on hit, was 0x${after.toString(16)}")
    }

    @Test
    fun bulletIntoRiverDoesNotDestroy() {
        val plane = MapLoader.parse(ByteArray(169))
        plane.map[5][5].kind = TileKind.RIVER
        plane.map[5][5].mask = 0xFFFF

        val bx = 5 * GameConfig.TILE_W + 16
        val by = 5 * GameConfig.TILE_H
        val kind = plane.hitSurface(bx, by, 8, 8, Direction.DOWN, destroyConcrete = false)
        assertEquals(TileKind.RIVER, kind)
        // River mask should not have been touched.
        assertEquals(0xFFFF, plane.map[5][5].mask)
    }

    @Test
    fun getSurfaceOnEmptyReturnsNull() {
        val plane = MapLoader.parse(ByteArray(169))
        val surface = plane.getSurface(50, 50, 28, 28, Direction.UP)
        assertEquals(TileKind.NULL, surface)
    }
}
