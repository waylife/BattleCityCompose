package com.battlecity.engine

/**
 * Tile kinds, matching UEFI's `TILE_OBJ` enum in `Plane.h`.
 * The numeric values matter: they index into the `tile.png` sprite sheet
 * (NULL is rendered as nothing, BRICK as index 0, etc.).
 */
enum class TileKind(val index: Int) {
    NULL(-1),
    BRICK(0),
    CONCRETE(1),
    TREE(2),
    RIVER(3),
    STONE(4),
    HAWK(5);

    val isDestructible: Boolean get() = this == BRICK || this == CONCRETE

    companion object {
        fun fromByte(b: Byte): TileKind {
            val v = (b.toInt() and 0xFF) - 1
            return entries.firstOrNull { it.index == v } ?: NULL
        }
    }
}

/**
 * A single map cell. `mask` is a 16-bit value representing the 4x4 sub-grid
 * of 8x8 pixel squares (0xFFFF = full, 0x0000 = cleared).
 *
 *   MSB 0x8000  0x4000  0x2000  0x1000    row 0
 *        0x0800  0x0400  0x0200  0x0100    row 1
 *        0x0080  0x0040  0x0020  0x0010    row 2
 *   LSB 0x0008  0x0004  0x0002  0x0001    row 3
 */
data class Tile(var kind: TileKind = TileKind.NULL, var mask: Int = 0) {
    fun isEmpty(): Boolean = kind == TileKind.NULL || mask == 0
}
