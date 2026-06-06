package com.battlecity.engine

import com.battlecity.clock.nowMs

/**
 * Explosion slot, matching UEFI's `EXPLODE` struct in `Explode.h`.
 * Pool size = [GameConfig.EXPLODE_NUM] (20 in UEFI).
 */
class Explode : Sprite(28, 28) {
    var big: Boolean = false
    var startMs: Long = 0L

    fun spawn(centerX: Int, centerY: Int, isBig: Boolean, now: Long) {
        big = isBig
        if (isBig) { w = 64; h = 64 } else { w = 28; h = 28 }
        x = centerX - w / 2
        y = centerY - h / 2
        active = true
        startMs = now
    }
}

/** Pre-allocated pool of 20 explode slots, matching UEFI's `EXPLODE[20]`. */
class ExplodePool(size: Int = GameConfig.EXPLODE_NUM) {
    val slots: Array<Explode> = Array(size) { Explode() }

    fun spawn(centerX: Int, centerY: Int, big: Boolean, now: Long) {
        for (slot in slots) {
            if (!slot.active) {
                slot.spawn(centerX, centerY, big, now)
                return
            }
        }
    }

    fun tick(now: Long) {
        for (e in slots) {
            if (!e.active) continue
            val delta = now - e.startMs
            if (e.big) {
                if (delta > 200) e.active = false
            } else {
                if (delta > 70) e.active = false
            }
        }
    }
}
