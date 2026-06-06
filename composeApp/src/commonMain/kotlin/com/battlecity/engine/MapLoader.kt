package com.battlecity.engine

/**
 * Parses a level's `.map` file into a [Plane].
 * Format (from `PlaneCreateFromData` in `Plane.c:7-44`):
 *  - For each of the 13×13 cells:
 *    - 1 byte: tile kind index (0-based; UEFI stores 1-based then subtracts 1)
 *    - If kind is BRICK or CONCRETE: 1 extra byte for the sub-cell layout
 *      (0=top half, 1=bottom half, 2=horizontal half, 3=vertical half — see mask mapping below)
 */
object MapLoader {
    fun parse(bytes: ByteArray): Plane {
        val plane = Plane()
        val expected = GameConfig.PLANE_H * GameConfig.PLANE_W
        require(bytes.size >= expected) { "Map too small: ${bytes.size} < $expected" }
        var idx = 0
        for (i in 0 until GameConfig.PLANE_H) {
            for (j in 0 until GameConfig.PLANE_W) {
                if (idx >= bytes.size) error("Truncated map data at row=$i col=$j")
                val rawKind = bytes[idx].toInt() and 0xFF
                idx++
                val kind = TileKind.fromByte(rawKind.toByte())
                plane.map[i][j].kind = kind
                plane.map[i][j].mask = if (kind == TileKind.NULL) 0 else 0xFFFF
                if (kind == TileKind.BRICK || kind == TileKind.CONCRETE) {
                    if (idx >= bytes.size) error("Truncated mask at row=$i col=$j")
                    val maskCode = bytes[idx].toInt() and 0xFF
                    idx++
                    plane.map[i][j].mask = when (maskCode) {
                        0 -> 0xFF00
                        1 -> 0x00FF
                        2 -> 0xCCCC
                        3 -> 0x3333
                        else -> 0xFFFF
                    }
                }
            }
        }
        plane.riverFrame1 = true
        plane.protected = false
        plane.unprotect()
        return plane
    }
}
