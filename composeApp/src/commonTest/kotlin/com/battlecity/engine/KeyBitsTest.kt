package com.battlecity.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KeyBitsTest {
    @Test
    fun contains() {
        val kb = KeyBits(KeyBits.UP or KeyBits.FIRE)
        assertTrue(KeyBits.UP in kb)
        assertTrue(KeyBits.FIRE in kb)
        assertFalse(KeyBits.DOWN in kb)
    }

    @Test
    fun withAndWithout() {
        val kb = KeyBits.NONE.with(KeyBits.UP).with(KeyBits.FIRE)
        assertEquals(KeyBits.UP or KeyBits.FIRE, kb.raw)
        val down = kb.without(KeyBits.UP)
        assertEquals(KeyBits.FIRE, down.raw)
    }

    @Test
    fun directionMaskCoversAllFour() {
        // Sanity check the constants.
        assertEquals(0x000F, KeyBits.DIR_MASK)
        val all = KeyBits.UP or KeyBits.DOWN or KeyBits.LEFT or KeyBits.RIGHT
        assertEquals(0x000F, all and KeyBits.DIR_MASK)
    }
}
