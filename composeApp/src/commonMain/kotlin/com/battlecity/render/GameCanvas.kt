package com.battlecity.render

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.battlecity.clock.nowMs
import com.battlecity.engine.Explode
import com.battlecity.engine.GameConfig
import com.battlecity.engine.GamePhase
import com.battlecity.engine.GameState
import com.battlecity.engine.Player
import com.battlecity.engine.Tank
import com.battlecity.engine.TileKind

/**
 * Main rendering pass. Mirrors the order in UEFI's `GameRender`:
 *  1. Clear background (gray outside playfield, black inside)
 *  2. Plane tiles (brick/concrete/river/stone/hawk)
 *  3. Enemies
 *  4. Players
 *  5. Tree overlay (drawn on top of tanks)
 *  6. Explodes
 *  7. Bonus
 *  8. HUD
 *  9. GameOver overlay
 */
fun DrawScope.drawGame(
    state: GameState,
    atlas: SpriteAtlas,
    screenScale: Float = 1f,
    drawHud: Boolean = true,
) {
    val now = nowMs()
    val ox = GameConfig.PLANE_OFFSET_X
    val oy = GameConfig.PLANE_OFFSET_Y

    // Clear outside the playfield with gray.
    drawRect(Color(0xFF808080))

    // Clear playfield with black.
    drawRect(
        Color.Black,
        topLeft = androidx.compose.ui.geometry.Offset(ox.toFloat(), oy.toFloat()),
        size = androidx.compose.ui.geometry.Size(
            GameConfig.PLANE_PIXEL_W.toFloat(),
            GameConfig.PLANE_PIXEL_H.toFloat()
        ),
    )

    // Plane tiles.
    drawPlane(state, atlas, ox, oy)

    // Enemies.
    for (i in 0 until state.maxEnemies) {
        val e = state.enemies[i]
        if (e.active) drawTank(state, atlas, e, ox, oy, now)
    }

    // Players.
    if (state.players[0].active) drawPlayer(state, atlas, state.players[0], ox, oy, now)
    if (state.players[1].active) drawPlayer(state, atlas, state.players[1], ox, oy, now)

    // Tree overlay (after tanks so trees hide them).
    drawTrees(state, atlas, ox, oy)

    // Explodes.
    state.explodes.slots.forEach { drawExplode(atlas, it, ox, oy, now) }

    // Bonus.
    if (state.bonus.active && state.bonus.tickAnim(now)) {
        atlas.draw(
            this, SpriteAsset.BONUS,
            srcX = state.bonus.type.index * GameConfig.BONUS_W, srcY = 0,
            srcW = GameConfig.BONUS_W, srcH = GameConfig.BONUS_H,
            dstX = state.bonus.x + ox, dstY = state.bonus.y + oy,
        )
    }

    if (drawHud) drawHud(state, atlas, ox, oy)

    if (state.phase == GamePhase.OVER) {
        val sw = 248; val sh = 160
        atlas.draw(
            this, SpriteAsset.GAMEOVER,
            0, 0, sw, sh,
            ((size.width - sw) / 2).toInt(), ((size.height - sh) / 2).toInt(),
        )
    }
}

private fun DrawScope.drawPlane(state: GameState, atlas: SpriteAtlas, ox: Int, oy: Int) {
    val plane = state.plane
    for (i in 0 until GameConfig.PLANE_H) {
        for (j in 0 until GameConfig.PLANE_W) {
            val tile = plane.map[i][j]
            if (tile.kind == TileKind.NULL || tile.kind == TileKind.TREE) continue
            val px = j * GameConfig.TILE_W + ox
            val py = i * GameConfig.TILE_H + oy
            if (tile.mask == GameConfig.MASK_FULL_BRICK) {
                val srcX = if (tile.kind == TileKind.RIVER && !plane.riverFrame1)
                    tile.kind.index * GameConfig.TILE_W + GameConfig.TILE_W
                else tile.kind.index * GameConfig.TILE_W
                atlas.draw(this, SpriteAsset.TILE, srcX, 0,
                    GameConfig.TILE_W, GameConfig.TILE_H, px, py)
            } else {
                var cur = 0x8000
                for (k in 0 until 16) {
                    if (tile.mask and cur != 0) {
                        val sx = tile.kind.index * GameConfig.TILE_W + (k % 4) * 8
                        val sy = (k / 4) * 8
                        atlas.draw(this, SpriteAsset.TILE, sx, sy, 8, 8,
                            px + (k % 4) * 8, py + (k / 4) * 8)
                    }
                    cur = cur shr 1
                }
            }
        }
    }
}

private fun DrawScope.drawTrees(state: GameState, atlas: SpriteAtlas, ox: Int, oy: Int) {
    val plane = state.plane
    for (i in 0 until GameConfig.PLANE_H) {
        for (j in 0 until GameConfig.PLANE_W) {
            if (plane.map[i][j].kind == TileKind.TREE) {
                val px = j * GameConfig.TILE_W + ox
                val py = i * GameConfig.TILE_H + oy
                atlas.draw(this, SpriteAsset.TILE, TileKind.TREE.index * GameConfig.TILE_W, 0,
                    GameConfig.TILE_W, GameConfig.TILE_H, px, py)
            }
        }
    }
}

private fun DrawScope.drawTank(
    state: GameState, atlas: SpriteAtlas, t: Tank,
    ox: Int, oy: Int, now: Long,
) {
    if (t.bore) {
        if (now - t.shieldStartMs > 800) t.bore = false
        else return
    }
    val asset = SpriteAsset.PLAYER1
    val spriteCol = t.type * 2 + t.frame
    val dirIdx = t.dir.index
    atlas.draw(this, asset, spriteCol * 28, dirIdx * 28, 28, 28, t.x + ox, t.y + oy)
    if (t.shield) {
        val shieldAsset = SpriteAsset.SHIELD
        val shieldY = if (t.shieldFrame) 32 else 0
        atlas.draw(this, shieldAsset, 0, shieldY, 32, 32, t.x - 2 + ox, t.y - 2 + oy)
        if (now - t.flickerStartMs > GameConfig.SHIELD_FLICKER_MS) {
            t.shieldFrame = !t.shieldFrame
            t.flickerStartMs = now
        }
        if (now >= t.shieldEndMs) t.shield = false
    }
    t.bullets.forEach { b -> if (b.active) drawBullet(atlas, b, ox, oy) }
}

private fun DrawScope.drawPlayer(
    state: GameState, atlas: SpriteAtlas, p: Player,
    ox: Int, oy: Int, now: Long,
) {
    if (p.bore) {
        if (now - p.shieldStartMs > 800) p.bore = false
        else return
    }
    if (p.locked) {
        if (now - p.flickerStartMs > 200) {
            p.show = !p.show
            p.flickerStartMs = now
        }
        if (now - p.lockStartMs > GameConfig.PLAYER_LOCKED_MS) p.locked = false
    }
    if (!p.locked || p.show) {
        val asset = if (p === state.players[1]) SpriteAsset.PLAYER2 else SpriteAsset.PLAYER1
        val spriteCol = p.type * 2 + p.frame
        val dirIdx = p.dir.index
        atlas.draw(this, asset, spriteCol * 28, dirIdx * 28, 28, 28, p.x + ox, p.y + oy)
    }
    if (p.shield) {
        val shieldAsset = SpriteAsset.SHIELD
        val shieldY = if (p.shieldFrame) 32 else 0
        atlas.draw(this, shieldAsset, 0, shieldY, 32, 32, p.x - 2 + ox, p.y - 2 + oy)
        if (now - p.flickerStartMs > GameConfig.SHIELD_FLICKER_MS) {
            p.shieldFrame = !p.shieldFrame
            p.flickerStartMs = now
        }
        if (now >= p.shieldEndMs) p.shield = false
    }
    p.bullets.forEach { b -> if (b.active) drawBullet(atlas, b, ox, oy) }
}

private fun DrawScope.drawBullet(atlas: SpriteAtlas, b: com.battlecity.engine.Bullet, ox: Int, oy: Int) {
    val dirIdx = b.dir.index
    atlas.draw(this, SpriteAsset.BULLET, dirIdx * 8, 0, 8, 8, b.x + ox, b.y + oy)
}

private fun DrawScope.drawExplode(atlas: SpriteAtlas, e: Explode, ox: Int, oy: Int, now: Long) {
    if (!e.active) return
    val delta = now - e.startMs
    if (e.big) {
        val dx = e.x + ox
        val dy = e.y + oy
        if (delta < 50 || delta > 150) {
            atlas.draw(this, SpriteAsset.EXPLODE1, 0, 0, 28, 28, dx, dy)
        } else {
            atlas.draw(this, SpriteAsset.EXPLODE2, 0, 0, 64, 64, dx, dy)
        }
        if (delta > 200) e.active = false
    } else {
        val dx = e.x - 20 + ox
        val dy = e.y - 20 + oy
        atlas.draw(this, SpriteAsset.EXPLODE1, 0, 0, 28, 28, dx, dy)
        if (delta > 70) e.active = false
    }
}

private fun DrawScope.drawHud(state: GameState, atlas: SpriteAtlas, ox: Int, oy: Int) {
    val hudR = ox + GameConfig.PLANE_PIXEL_W + 34
    val hudL = ox - 80

    for (i in 0 until state.enemiesLeft) {
        val x = hudR + (i % 2) * 15
        val y = oy + 20 + (i / 2) * 15
        atlas.draw(this, SpriteAsset.MISC, 0, 0, 14, 14, x, y)
    }
    atlas.draw(this, SpriteAsset.MISC, 28, 0, 28, 14, hudR, oy + 252)
    atlas.draw(this, SpriteAsset.MISC, 28, 0, 28, 14, hudL, oy - 28)
    drawNumber(atlas, hudL + 50, oy - 28, state.players[0].score)

    atlas.draw(this, SpriteAsset.MISC, 14, 0, 14, 14, hudR, oy + 269)
    drawNumber(atlas, hudR + 20, oy + 269, state.players[0].lives)

    if (!state.single) {
        atlas.draw(this, SpriteAsset.MISC, 56, 0, 28, 14, hudR, oy + 302)
        atlas.draw(this, SpriteAsset.MISC, 56, 0, 28, 14,
            ox + GameConfig.PLANE_PIXEL_W - 36, oy - 28)
        drawNumber(atlas, ox + GameConfig.PLANE_PIXEL_W + 14, oy - 28, state.players[1].score)
        atlas.draw(this, SpriteAsset.MISC, 14, 0, 14, 14, hudR, oy + 319)
        drawNumber(atlas, hudR + 20, oy + 319, state.players[1].lives)
    }

    atlas.draw(this, SpriteAsset.FLAG, 0, 0, 32, 32, hudR, oy + 352)
    drawNumber(atlas, hudR + 30, oy + 372, state.level)
}

private fun DrawScope.drawNumber(atlas: SpriteAtlas, x: Int, y: Int, n: Int) {
    var value = n
    if (value < 0) value = 0
    val digits = mutableListOf<Int>()
    if (value == 0) digits.add(0) else {
        while (value > 0) { digits.add(0, value % 10); value /= 10 }
    }
    var dx = x
    for (d in digits) {
        atlas.draw(this, SpriteAsset.NUM, d * 14, 0, 14, 14, dx, y)
        dx += 14
    }
}
