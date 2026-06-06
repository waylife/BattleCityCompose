package com.battlecity.engine

import com.battlecity.clock.nowMs

/**
 * Player tank with score, lives, lock state. Mirrors UEFI's `PLAYER` struct
 * in `Tank.h:40-47` plus the `PlayerInit`/`PlayerFire`/`PlayerProcessInput`
 * functions.
 */
class Player(startX: Int, startY: Int) : Tank(bulletSlots = 2) {
    var lives: Int = GameConfig.PLAYER_START_LIVES
    var score: Int = 0
    var locked: Boolean = false
    var show: Boolean = true
    var lockStartMs: Long = 0L

    init {
        x = startX
        y = startY
        type = 0
        speed = 2.0f
        active = true
    }

    /** Rebirth: re-enable tank, grant spawn shield, reset bullets. */
    fun reborn() {
        active = true
        bore = true
        shield = true
        shieldFrame = true
        val now = nowMs()
        shieldStartMs = now
        shieldEndMs = now + GameConfig.PLAYER_RESPAWN_SHIELD_MS
        flickerStartMs = now
        dir = Direction.UP
        speed = 2.0f
        locked = false
        bullets.forEach { it.active = false }
    }

    /**
     * Translate the player's keybits into tank movement. Returns `true` if a
     * bullet was fired this tick.
     */
    fun processInput(input: KeyBits, now: Long): Boolean {
        if (!locked) {
            val newDir = when {
                KeyBits.UP in input -> Direction.UP
                KeyBits.DOWN in input -> Direction.DOWN
                KeyBits.LEFT in input -> Direction.LEFT
                KeyBits.RIGHT in input -> Direction.RIGHT
                else -> dir
            }
            if (KeyBits.DIR_MASK in input) {
                if (dir == newDir) {
                    moveStep()
                } else {
                    changeDirection(newDir)
                }
            }
        }
        if (KeyBits.FIRE in input) {
            return fire(now)
        }
        return false
    }

    /**
     * Fire a bullet. Speed depends on tank type (0→3.0, 1→4.0, ≥2→5.0).
     * Two bullet slots unlock at type ≥ 2. Cooldown is 200 ms.
     */
    fun fire(now: Long): Boolean {
        val speed = when (type) {
            0 -> 3.0f
            1 -> 4.0f
            else -> 5.0f
        }
        if (now - lastFireMs < GameConfig.PLAYER_FIRE_COOLDOWN_MS) return false
        val slot = bullets.indexOfFirst { !it.active }
        if (slot < 0 || (slot != 0 && type < 2)) return false

        lastFireMs = now
        val b = bullets[slot]
        b.active = true
        b.speed = speed
        b.dir = dir
        when (dir) {
            Direction.UP -> { b.x = x + w / 2 - 4; b.y = y + 4 }
            Direction.DOWN -> { b.x = x + w / 2 - 4; b.y = y + h - 12 }
            Direction.LEFT -> { b.x = x + 4; b.y = y + h / 2 - 4 }
            Direction.RIGHT -> { b.x = x + w - 12; b.y = y + h / 2 - 4 }
        }
        return true
    }
}
