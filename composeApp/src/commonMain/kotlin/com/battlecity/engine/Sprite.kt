package com.battlecity.engine

/**
 * Base sprite. Mirrors UEFI's `SPRITE` struct in `Sprite.h`.
 * Mutable: the engine mutates slot arrays in-place each tick.
 */
open class Sprite(
    w: Int,
    h: Int,
) {
    var w: Int = w
    var h: Int = h
    var active: Boolean = false
    var x: Int = 0
    var y: Int = 0

    fun reset(x: Int = 0, y: Int = 0) {
        this.active = false
        this.x = x
        this.y = y
    }
}

/** Standard AABB test, matching `SpriteHitTest` in `Sprite.c`. */
fun hitTest(a: Sprite, b: Sprite): Boolean {
    if (!a.active || !b.active) return false
    if (a.x + a.w <= b.x) return false
    if (b.x + b.w <= a.x) return false
    if (a.y + a.h <= b.y) return false
    if (b.y + b.h <= a.y) return false
    return true
}
