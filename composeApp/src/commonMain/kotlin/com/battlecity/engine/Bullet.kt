package com.battlecity.engine

/**
 * Bullet entity, matching UEFI's `BULLET` struct in `Bullet.h`.
 * Bullets are owned by tanks; each tank has up to 2 bullet slots.
 */
class Bullet : Sprite(GameConfig.BULLET_W, GameConfig.BULLET_H) {
    var dir: Direction = Direction.UP
    var speed: Float = 3.0f

    fun move(): Boolean {
        if (!active) return false
        when (dir) {
            Direction.UP -> y -= speed.toInt()
            Direction.DOWN -> y += speed.toInt()
            Direction.LEFT -> x -= speed.toInt()
            Direction.RIGHT -> x += speed.toInt()
        }
        return if (x in 0..(GameConfig.PLANE_PIXEL_W - w) &&
            y in 0..(GameConfig.PLANE_PIXEL_H - h)
        ) {
            true
        } else {
            // Clamp to bounds and report out-of-bounds.
            x = x.coerceIn(0, GameConfig.PLANE_PIXEL_W - w)
            y = y.coerceIn(0, GameConfig.PLANE_PIXEL_H - h)
            false
        }
    }
}
