package com.battlecity.engine

/**
 * Cardinal directions, matching the C enum in `Sprite.h` (UP=0, RIGHT=1, DOWN=2, LEFT=3).
 * The numeric values matter — they index into sprite sheets for the four direction rows.
 */
enum class Direction(val index: Int) {
    UP(0),
    RIGHT(1),
    DOWN(2),
    LEFT(3);

    companion object {
        fun fromIndex(i: Int): Direction = entries[i and 3]
    }
}
