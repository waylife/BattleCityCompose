package com.battlecity.engine

import kotlin.jvm.JvmInline

/** 16-bit key bitset, matching UEFI's `INPUT_STATE` masks. */
@JvmInline
value class KeyBits(val raw: Int) {
    operator fun contains(bit: Int): Boolean = (raw and bit) != 0
    fun with(bit: Int): KeyBits = KeyBits(raw or bit)
    fun without(bit: Int): KeyBits = KeyBits(raw and bit.inv())
    fun isEmpty(): Boolean = raw == 0

    companion object {
        const val UP: Int = 0x0001
        const val RIGHT: Int = 0x0002
        const val DOWN: Int = 0x0004
        const val LEFT: Int = 0x0008
        const val DIR_MASK: Int = 0x000F
        const val FIRE: Int = 0x0010
        const val ENTER: Int = 0x0020
        const val ESC: Int = 0x0040

        val NONE = KeyBits(0)
    }
}

/** Per-player + system input bundle. */
data class InputState(
    val p1: KeyBits = KeyBits.NONE,
    val p2: KeyBits = KeyBits.NONE,
    val system: KeyBits = KeyBits.NONE,
)
