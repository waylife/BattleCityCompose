package com.battlecity.engine

import com.battlecity.clock.nowMs

/**
 * 13×13 map of [Tile]s. Mirrors UEFI's `PLANE` struct in `Plane.h`.
 *
 * The collision logic is the most algorithmically dense part of the game.
 * The 16-bit `Tile.mask` represents 4×4 sub-cells of 8×8 pixels each:
 *
 *   bit 15  14  13  12 | row 0
 *   bit 11  10   9   8 | row 1
 *   bit  7   6   5   4 | row 2
 *   bit  3   2   1   0 | row 3
 *
 * When a bullet crosses a tile, we compute a `Flag` (a 4-bit slice of the
 * mask that the bullet's centre falls in), report the tile kind, and clear
 * the slice if the tile is destructible.
 */
class Plane {
    val map: Array<Array<Tile>> = Array(GameConfig.PLANE_H) {
        Array(GameConfig.PLANE_W) { Tile() }
    }
    var riverFrame1: Boolean = true
    var riverAnimMs: Long = 0L
    var protected: Boolean = false
    var protectStartMs: Long = 0L

    /**
     * Bullet hit-test. Returns the tile kind hit, or `NULL` if nothing.
     * `destroyConcrete` is `true` for the upgraded player (type ≥ 3).
     */
    fun hitSurface(
        bx: Int, by: Int, bw: Int, bh: Int,
        dir: Direction, destroyConcrete: Boolean,
    ): TileKind {
        val x = bx + bw / 2
        val y = by + bh / 2
        val row = y / GameConfig.TILE_H
        val col = x / GameConfig.TILE_W
        if (row !in 0 until GameConfig.PLANE_H || col !in 0 until GameConfig.PLANE_W) return TileKind.NULL

        var hit = TileKind.NULL
        val shift: Int
        var flag: Int

        when (dir) {
            Direction.UP, Direction.DOWN -> {
                shift = if (!destroyConcrete) (3 - (y - row * GameConfig.TILE_H) / 8) * 4
                else (1 - (y - row * GameConfig.TILE_H) / 16) * 8
                if (x % GameConfig.TILE_W != 0) {
                    flag = if (!destroyConcrete) 0xF shl shift else 0xFF shl shift
                    if (map[row][col].mask and flag != 0) {
                        hit = map[row][col].kind
                        if (map[row][col].kind.isDestructible &&
                            (!destroyConcrete || map[row][col].kind == TileKind.BRICK || destroyConcrete)
                        ) {
                            map[row][col].mask = map[row][col].mask and flag.inv()
                        }
                    }
                } else {
                    flag = if (!destroyConcrete) 0xC shl shift else 0xCC shl shift
                    if (map[row][col].mask and flag != 0) {
                        hit = map[row][col].kind
                        if (map[row][col].kind.isDestructible &&
                            (!destroyConcrete || map[row][col].kind == TileKind.BRICK || destroyConcrete)
                        ) {
                            map[row][col].mask = map[row][col].mask and flag.inv()
                        }
                    }
                    flag = if (!destroyConcrete) 0x3 shl shift else 0x33 shl shift
                    if (col >= 1 && map[row][col - 1].mask and flag != 0) {
                        if (hit != TileKind.BRICK && hit != TileKind.CONCRETE) {
                            hit = map[row][col - 1].kind
                        }
                        if (map[row][col - 1].kind.isDestructible &&
                            (!destroyConcrete || map[row][col - 1].kind == TileKind.BRICK || destroyConcrete)
                        ) {
                            map[row][col - 1].mask = map[row][col - 1].mask and flag.inv()
                        }
                    }
                }
                if (col >= 1 && map[row][col - 1].mask == 0) {
                    map[row][col - 1].kind = TileKind.NULL
                }
            }
            Direction.LEFT, Direction.RIGHT -> {
                shift = if (!destroyConcrete) (x - col * GameConfig.TILE_W) / 8
                else (x - col * GameConfig.TILE_W) / 16
                if (y % GameConfig.TILE_H != 0) {
                    val mask = if (!destroyConcrete) {
                        when (shift) {
                            0 -> 0x8888
                            1 -> 0x4444
                            2 -> 0x2222
                            else -> 0x1111
                        }
                    } else {
                        if (shift == 0) 0xCCCC else 0x3333
                    }
                    if (map[row][col].mask and mask != 0) {
                        hit = map[row][col].kind
                        if (map[row][col].kind.isDestructible &&
                            (!destroyConcrete || map[row][col].kind == TileKind.BRICK || destroyConcrete)
                        ) {
                            map[row][col].mask = map[row][col].mask and mask.inv()
                        }
                    }
                } else {
                    val mask = if (!destroyConcrete) {
                        when (shift) {
                            0 -> 0x8800
                            1 -> 0x4400
                            2 -> 0x2200
                            else -> 0x1100
                        }
                    } else {
                        if (shift == 0) 0xCC00 else 0x3300
                    }
                    if (map[row][col].mask and mask != 0) {
                        hit = map[row][col].kind
                        if (map[row][col].kind.isDestructible &&
                            (!destroyConcrete || map[row][col].kind == TileKind.BRICK || destroyConcrete)
                        ) {
                            map[row][col].mask = map[row][col].mask and mask.inv()
                        }
                    }
                    val low = mask ushr 8
                    if (row >= 1 && map[row - 1][col].mask and low != 0) {
                        if (hit != TileKind.BRICK && hit != TileKind.CONCRETE) {
                            hit = map[row - 1][col].kind
                        }
                        if (map[row - 1][col].kind.isDestructible &&
                            (!destroyConcrete || map[row - 1][col].kind == TileKind.BRICK || destroyConcrete)
                        ) {
                            map[row - 1][col].mask = map[row - 1][col].mask and low.inv()
                        }
                    }
                }
                if (row >= 1 && map[row - 1][col].mask == 0) {
                    map[row - 1][col].kind = TileKind.NULL
                }
            }
        }

        if (map[row][col].mask == 0) {
            map[row][col].kind = TileKind.NULL
        }
        if (hit == TileKind.HAWK) {
            // The base was destroyed — turn it into stone (matches `Plane.c:179-181`).
            map[12][6].kind = TileKind.STONE
        }
        return hit
    }

    /**
     * Tank collision in the tank's facing direction. Returns the first
     * non-null, non-tree tile kind the tank's leading edge overlaps.
     * Matches `PlaneGetSurface` in `Plane.c:233-285`.
     */
    fun getSurface(
        tx: Int, ty: Int, tw: Int, th: Int, dir: Direction,
    ): TileKind {
        val x1 = tx; val y1 = ty
        val x2 = x1 + tw; val y2 = y1 + th
        val xc = x1 + tw / 2
        val yc = y1 + th / 2
        if (dir == Direction.UP || dir == Direction.DOWN) {
            val col = xc / GameConfig.TILE_W
            val row = if (dir == Direction.UP) y1 / GameConfig.TILE_H else y2 / GameConfig.TILE_H
            if (xc % GameConfig.TILE_W != 0) {
                if (getSurfaceAt(row, col, x1, y1, x2, y2, -1, -1)) {
                    return map[row][col].kind
                }
            } else {
                if (col >= 1 && getSurfaceAt(row, col - 1, x1, y1, x2, y2, 1, 3)) {
                    return map[row][col - 1].kind
                }
                if (getSurfaceAt(row, col, x1, y1, x2, y2, 0, 2)) {
                    return map[row][col].kind
                }
            }
        } else {
            val row = yc / GameConfig.TILE_H
            val col = if (dir == Direction.LEFT) x1 / GameConfig.TILE_W else x2 / GameConfig.TILE_W
            if (yc % GameConfig.TILE_H != 0) {
                if (getSurfaceAt(row, col, x1, y1, x2, y2, -1, -1)) {
                    return map[row][col].kind
                }
            } else {
                if (row >= 1 && getSurfaceAt(row - 1, col, x1, y1, x2, y2, 2, 3)) {
                    return map[row - 1][col].kind
                }
                if (getSurfaceAt(row, col, x1, y1, x2, y2, 0, 1)) {
                    return map[row][col].kind
                }
            }
        }
        return TileKind.NULL
    }

    private fun getSurfaceAt(
        row: Int, col: Int,
        al: Int, at: Int, ar: Int, ab: Int,
        quadA: Int, quadB: Int,
    ): Boolean {
        if (row !in 0 until GameConfig.PLANE_H || col !in 0 until GameConfig.PLANE_W) return false
        val t = map[row][col]
        if (t.kind == TileKind.NULL || t.kind == TileKind.TREE) return false
        for (i in 0 until 4) {
            if (i == quadA || i == quadB || (quadA == -1 && quadB == -1)) {
                val sl = col * GameConfig.TILE_W + (i % 2) * 16
                val sr = sl + 16
                val st = row * GameConfig.TILE_H + (i / 2) * 16
                val sb = st + 16
                val flag = when (i) {
                    0 -> GameConfig.MASK_QUAD_TL
                    1 -> GameConfig.MASK_QUAD_TR
                    2 -> GameConfig.MASK_QUAD_BL
                    else -> GameConfig.MASK_QUAD_BR
                }
                if ((t.mask and flag) != 0 &&
                    al < sr && ar > sl && at < sb && ab > st
                ) {
                    return true
                }
            }
        }
        return false
    }

    /** Convert the base's surrounding brick wall to concrete for 20s. */
    fun protect() {
        map[12][5].kind = TileKind.CONCRETE; map[12][5].mask = 0x3333
        map[11][5].kind = TileKind.CONCRETE; map[11][5].mask = 0x0033
        map[11][6].kind = TileKind.CONCRETE; map[11][6].mask = 0x00FF
        map[11][7].kind = TileKind.CONCRETE; map[11][7].mask = 0x00CC
        map[12][7].kind = TileKind.CONCRETE; map[12][7].mask = 0xCCCC
        protected = true
        protectStartMs = nowMs()
    }

    fun unprotect() {
        map[12][5].kind = TileKind.BRICK; map[12][5].mask = 0x3333
        map[11][5].kind = TileKind.BRICK; map[11][5].mask = 0x0033
        map[11][6].kind = TileKind.BRICK; map[11][6].mask = 0x00FF
        map[11][7].kind = TileKind.BRICK; map[11][7].mask = 0x00CC
        map[12][7].kind = TileKind.BRICK; map[12][7].mask = 0xCCCC
        protected = false
    }

    /** Clear the base's surrounding wall entirely (eaten by shovel bonus when hit by enemy). */
    fun bare() {
        for (r in 11..12) for (c in 5..7) {
            map[r][c].kind = TileKind.NULL
            map[r][c].mask = 0
        }
        protected = false
    }

    /** Per-tick: animate river, expire protect. */
    fun tickAnim(now: Long) {
        if (now - riverAnimMs > GameConfig.RIVER_ANIM_MS) {
            riverFrame1 = !riverFrame1
            riverAnimMs = now
        }
        if (protected && now - protectStartMs > GameConfig.PROTECT_DURATION_MS) {
            unprotect()
        }
    }
}
