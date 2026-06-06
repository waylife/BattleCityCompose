package com.battlecity.engine

/**
 * Tank (player or enemy). Mirrors UEFI's `TANK` struct in `Tank.h`.
 *
 * Speed is a `Float`; the integer part is extracted as the per-step distance
 * and the remainder is kept in [speedRem] for the next step. This matches
 * the UEFI `SpeedRem` workaround for low-speed enemies (0.5f etc.).
 */
open class Tank(
    val bulletSlots: Int = 2,
) : Sprite(GameConfig.TANK_W, GameConfig.TANK_H) {
    var dir: Direction = Direction.UP
    var speed: Float = 2.0f
    var speedRem: Float = 0.0f
    var frame: Int = 0           // animation toggle (0/1)
    var type: Int = 0            // 0..3 for player, 0..2 for enemy
    var bore: Boolean = false    // true during the post-spawn "hidden" window
    var shield: Boolean = false
    var shieldFrame: Boolean = true   // alternates every 50 ms for flicker
    var shieldStartMs: Long = 0
    var shieldEndMs: Long = 0
    var flickerStartMs: Long = 0
    var lastFireMs: Long = 0
    val bullets: Array<Bullet> = Array(bulletSlots) { Bullet() }

    /** Toggle the animation frame and step the tank by [speed] pixels. */
    fun moveStep(): Boolean {
        frame = frame xor 1
        val total = speed + speedRem
        val step = total.toInt()
        speedRem = total - step
        when (dir) {
            Direction.UP -> y -= step
            Direction.DOWN -> y += step
            Direction.LEFT -> x -= step
            Direction.RIGHT -> x += step
        }
        var blocked = false
        if (x < 0) { x = 0; blocked = true }
        if (y < 0) { y = 0; blocked = true }
        if (x + w > GameConfig.PLANE_PIXEL_W) { x = GameConfig.PLANE_PIXEL_W - w; blocked = true }
        if (y + h > GameConfig.PLANE_PIXEL_H) { y = GameConfig.PLANE_PIXEL_H - h; blocked = true }
        return !blocked
    }

    /**
     * Direction change with lane-snap. Matches `TankChangeDirection` in `Tank.c:80-103`.
     * The 2/18/34 offsets line the tank up cleanly on the 32×32 tile grid.
     */
    fun changeDirection(newDir: Direction) {
        if (dir == newDir) return
        dir = newDir
        val col = x / GameConfig.TILE_W
        val row = y / GameConfig.TILE_H
        val xOff = x % GameConfig.TILE_W
        val yOff = y % GameConfig.TILE_H
        x = col * GameConfig.TILE_W + when {
            xOff <= GameConfig.LANE_SNAP_LOW -> GameConfig.LANE_X_A
            xOff < GameConfig.TILE_W - GameConfig.LANE_SNAP_LOW2 -> GameConfig.LANE_X_B
            else -> GameConfig.LANE_X_C
        }
        y = row * GameConfig.TILE_H + when {
            yOff <= GameConfig.LANE_SNAP_LOW -> GameConfig.LANE_Y_A
            yOff < GameConfig.TILE_H - GameConfig.LANE_SNAP_LOW2 -> GameConfig.LANE_Y_B
            else -> GameConfig.LANE_Y_C
        }
    }

    /**
     * Tank-vs-tank swept AABB in the current direction. Mirrors `TankHitTest` in `Tank.c:153-181`.
     * Old position is used so we know which face of the bounding box was crossed.
     */
    fun hitTest(other: Tank, oldX: Int, oldY: Int): Boolean {
        if (!active || !other.active || bore || other.bore) return false
        val nx = x; val ny = y
        val ox1 = other.x; val oy1 = other.y
        val ox2 = ox1 + other.w; val oy2 = oy1 + other.h
        return when (dir) {
            Direction.UP ->
                nx <= ox2 && nx + w >= ox1 && oldY >= oy2 && ny <= oy2
            Direction.DOWN ->
                nx <= ox2 && nx + w >= ox1 && oldY + h <= oy1 && ny + h >= oy1
            Direction.RIGHT ->
                ny <= oy2 && ny + h >= oy1 && oldX + w <= ox1 && nx + w >= ox1
            Direction.LEFT ->
                ny <= oy2 && ny + h >= oy1 && oldX >= ox2 && nx <= ox2
        }
    }
}
