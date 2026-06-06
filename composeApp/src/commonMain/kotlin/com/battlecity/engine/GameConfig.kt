package com.battlecity.engine

/**
 * Compile-time game constants. Mirror the constants in UEFI's `Gfx.h`.
 * Kept centralised so the renderer and engine stay in lock-step.
 */
object GameConfig {
    // Logical game window (matches UEFI 640x480, but rendered scaled to fit canvas).
    const val SCREEN_W: Int = 640
    const val SCREEN_H: Int = 480

    // Tile grid.
    const val PLANE_W: Int = 13
    const val PLANE_H: Int = 13
    const val TILE_W: Int = 32
    const val TILE_H: Int = 32
    const val PLANE_PIXEL_W: Int = PLANE_W * TILE_W  // 416
    const val PLANE_PIXEL_H: Int = PLANE_H * TILE_H  // 416

    // Letterbox offsets: 640-416=224 → 112 px each side; 480-416=64 → 32 px each side.
    const val PLANE_OFFSET_X: Int = 112
    const val PLANE_OFFSET_Y: Int = 32

    // Tank / bullet / bonus.
    const val TANK_W: Int = 28
    const val TANK_H: Int = 28
    const val BULLET_W: Int = 8
    const val BULLET_H: Int = 8
    const val BONUS_W: Int = 30
    const val BONUS_H: Int = 28

    // Player / enemy slots (matches UEFI `Tank.h`).
    const val PLAYER_NUM: Int = 2
    const val ENEMY_NUM: Int = 6
    const val EXPLODE_NUM: Int = 20

    // Player starting positions (matches `Game.c` PLAYER1_STARTX/Y).
    const val PLAYER1_START_X: Int = 130
    const val PLAYER1_START_Y: Int = 386
    const val PLAYER2_START_X: Int = 258
    const val PLAYER2_START_Y: Int = 386

    // Animation / timing.
    const val TARGET_FPS: Int = 30
    const val FRAME_DELAY_MS: Int = 1000 / TARGET_FPS
    const val ENEMY_SPAWN_INTERVAL_MS: Int = 3000
    const val ENEMY_FIRE_COOLDOWN_MS: Int = 150
    const val PLAYER_FIRE_COOLDOWN_MS: Int = 200
    const val PLAYER_RESPAWN_SHIELD_MS: Int = 3000
    const val ENEMY_RESPAWN_SHIELD_MS: Int = 800
    const val HELMET_SHIELD_MS: Int = 10000
    const val SHIELD_FLICKER_MS: Int = 50
    const val LOCKED_DURATION_MS: Int = 10000  // bonus clock
    const val PLAYER_LOCKED_MS: Int = 5000    // enemy-bomb
    const val WIN_DELAY_MS: Int = 3000
    const val BONUS_DURATION_MS: Int = 10000
    const val BONUS_FLICKER_MS: Int = 200
    const val PROTECT_DURATION_MS: Int = 20000
    const val RIVER_ANIM_MS: Int = 500

    // Game state.
    const val ENEMIES_PER_LEVEL: Int = 20
    const val MAX_ENEMIES_SINGLE: Int = 4
    const val MAX_ENEMIES_TWO_PLAYER: Int = 6
    const val PLAYER_START_LIVES: Int = 3
    val ENEMIES_REMAINING_FOR_BONUS: IntArray = intArrayOf(4, 11, 18)

    // Brick mask quadrant constants from `Plane.c`.
    const val MASK_QUAD_TL: Int = 0xCC00
    const val MASK_QUAD_TR: Int = 0x3300
    const val MASK_QUAD_BL: Int = 0x00CC
    const val MASK_QUAD_BR: Int = 0x0033
    const val MASK_FULL_BRICK: Int = 0xFFFF
    const val MASK_CLEARED: Int = 0x0000

    // Enemy speed table (Float) — matches `EnemyReborn` in UEFI.
    val ENEMY_SPEED: FloatArray = floatArrayOf(0.7f, 1.2f, 0.5f)

    // Tank turn lane-snap offsets (matches `Tank.c:91-101`).
    const val LANE_SNAP_LOW: Int = 10
    const val LANE_SNAP_LOW2: Int = 6  // X % TILE_W < TILE_W - 6
    const val LANE_X_A: Int = 2
    const val LANE_X_B: Int = 18
    const val LANE_X_C: Int = 34
    const val LANE_Y_A: Int = 2
    const val LANE_Y_B: Int = 18
    const val LANE_Y_C: Int = 34
}
