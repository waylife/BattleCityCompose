package com.battlecity.engine

import com.battlecity.clock.nowMs

/** Bonus kinds, matching UEFI's `BONUS_TYPE` enum. */
enum class BonusType(val index: Int) {
    LIFE(0),
    CLOCK(1),
    SHOVEL(2),
    BOMB(3),
    STAR(4),
    HELMET(5);

    companion object {
        /** Match the probability table from UEFI `BonusSpawn`. */
        fun roll(r: Int): BonusType = when (r) {
            in 0..9 -> LIFE
            in 10..29 -> CLOCK
            in 30..49 -> SHOVEL
            in 50..64 -> BOMB
            in 65..84 -> STAR
            else -> HELMET
        }
    }
}

/**
 * Power-up entity, matching UEFI's `BONUS` struct.
 * 6 kinds, flickers for 10s, then despawns.
 */
class Bonus : Sprite(GameConfig.BONUS_W, GameConfig.BONUS_H) {
    var type: BonusType = BonusType.LIFE
    var lastMs: Long = 0L
    var flickerMs: Long = 0L
    var show: Boolean = true

    fun spawn(rng: GameRng) {
        type = BonusType.roll(rng.nextInt(100))
        x = rng.nextInt(GameConfig.PLANE_PIXEL_W - GameConfig.BONUS_W)
        y = rng.nextInt(GameConfig.PLANE_PIXEL_H - GameConfig.BONUS_H)
        active = true
        val now = nowMs()
        lastMs = now
        flickerMs = now
        show = true
    }

    fun tickAnim(now: Long): Boolean {
        if (!active) return false
        if (now - flickerMs > GameConfig.BONUS_FLICKER_MS) {
            flickerMs = now
            show = !show
        }
        if (now - lastMs > GameConfig.BONUS_DURATION_MS) {
            active = false
            return false
        }
        return show
    }
}
