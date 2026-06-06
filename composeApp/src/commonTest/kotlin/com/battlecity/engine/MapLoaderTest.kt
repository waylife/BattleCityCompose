package com.battlecity.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapLoaderTest {
    /**
     * 13×13 = 169 cells. Each NULL cell = 1 byte, each BRICK/CONCRETE cell
     * = 2 bytes (kind + mask sub-cell code). After parsing, the surrounding
     * base tiles are pre-populated with BRICK by `PlaneUnprotect`, so we
     * exclude those cells from the NULL assertion.
     */
    @Test
    fun parsesNullCells() {
        val bytes = ByteArray(169)
        val plane = MapLoader.parse(bytes)
        val baseCells = setOf(
            11 to 5, 11 to 6, 11 to 7, 12 to 5, 12 to 7,
        )
        for (row in 0 until GameConfig.PLANE_H) {
            for (col in 0 until GameConfig.PLANE_W) {
                if (row to col in baseCells) continue
                assertEquals(TileKind.NULL, plane.map[row][col].kind,
                    "expected NULL at ($row,$col)")
            }
        }
    }

    @Test
    fun parsesBrickWithMask() {
        // 1 BRICK cell (2 bytes) + 168 NULL cells (168 bytes) = 170 bytes.
        val bytes = ByteArray(170)
        bytes[0] = 1  // BRICK (1-based; internal index = 0)
        bytes[1] = 3.toByte()  // mask code 3 → 0x3333
        val plane = MapLoader.parse(bytes)
        assertEquals(TileKind.BRICK, plane.map[0][0].kind)
        assertEquals(0x3333, plane.map[0][0].mask)
    }

    @Test
    fun parsesConcreteWithFullMask() {
        val bytes = ByteArray(170)
        bytes[0] = 2  // CONCRETE
        bytes[1] = 0xFF.toByte()  // invalid mask code → falls back to 0xFFFF
        val plane = MapLoader.parse(bytes)
        assertEquals(TileKind.CONCRETE, plane.map[0][0].kind)
        assertEquals(0xFFFF, plane.map[0][0].mask)
    }

    @Test
    fun parsesAllMaskCodes() {
        val codes = intArrayOf(0, 1, 2, 3, 99)
        val expected = intArrayOf(0xFF00, 0x00FF, 0xCCCC, 0x3333, 0xFFFF)
        codes.forEachIndexed { i, code ->
            val bytes = ByteArray(170)
            bytes[0] = 1
            bytes[1] = code.toByte()
            val plane = MapLoader.parse(bytes)
            assertEquals(expected[i], plane.map[0][0].mask,
                "mask code $code should map to 0x${expected[i].toString(16)}")
        }
    }

    @Test
    fun unprotectInitialisesBaseToBricks() {
        val plane = MapLoader.parse(ByteArray(169))
        // PlaneUnprotect sets the surrounding wall (cells around the base at
        // [12][6]) to BRICK with a 4-quadrant mask. The base cell itself
        // ([12][6]) is not touched — it remains whatever the map data says.
        assertEquals(TileKind.BRICK, plane.map[12][5].kind)
        assertEquals(0x3333, plane.map[12][5].mask)
        assertEquals(TileKind.BRICK, plane.map[11][5].kind)
    }

    @Test
    fun protectTurnsBaseToConcrete() {
        val plane = MapLoader.parse(ByteArray(169))
        plane.protect()
        assertEquals(TileKind.CONCRETE, plane.map[12][5].kind)
        assertEquals(TileKind.CONCRETE, plane.map[12][7].kind)
        assertTrue(plane.protected)
    }
}
