package com.battlecity.engine

import com.battlecity.clock.nowMs

/**
 * Enemy tank. Mirrors UEFI's `ENEMY` struct (`Tank.h:49-55`) plus the
 * `EnemyInit` / `EnemyReborn` / `EnemyFire` / `EnemyChangeDirection` functions.
 */
class Enemy : Tank(bulletSlots = 1) {
    var hasBonus: Boolean = false
    var showRed: Boolean = false
    var redStartMs: Long = 0L
    /** Hit-points for the armoured (Type 2) variant. */
    var level: Int = 0

    init {
        active = false
    }

    fun reborn(x: Int, y: Int, type: Int) {
        this.x = x
        this.y = y
        dir = Direction.DOWN
        this.type = type
        frame = 0
        speed = GameConfig.ENEMY_SPEED.getOrElse(type) { 0.7f }
        bore = true
        shield = false
        shieldFrame = true
        val now = nowMs()
        shieldStartMs = now
        shieldEndMs = now + GameConfig.ENEMY_RESPAWN_SHIELD_MS
        flickerStartMs = now
        active = true
        if (type == 2) level = 2
    }

    /** Single bullet slot; cooldown 150 ms. */
    fun fire(now: Long): Boolean {
        if (bullets[0].active) return false
        if (now - lastFireMs < GameConfig.ENEMY_FIRE_COOLDOWN_MS) return false
        lastFireMs = now
        val b = bullets[0]
        b.active = true
        b.speed = 3.0f
        b.dir = dir
        when (dir) {
            Direction.UP -> { b.x = x + w / 2 - 4; b.y = y }
            Direction.DOWN -> { b.x = x + w / 2 - 4; b.y = y + h - 8 }
            Direction.LEFT -> { b.x = x; b.y = y + h / 2 - 4 }
            Direction.RIGHT -> { b.x = x + w - 8; b.y = y + h / 2 - 4 }
        }
        return true
    }

    fun changeDirection(rng: GameRng) {
        val d = Direction.fromIndex(rng.nextInt(4))
        changeDirection(d)
    }
}
