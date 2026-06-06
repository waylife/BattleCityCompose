package com.battlecity.engine

import com.battlecity.clock.nowMs

/**
 * The game engine: state machine + per-tick update.
 * Mirrors `GameUpdate` / `ProcessPlayers` / `ProcessBullets` / `ProcessEnemies`
 * in UEFI's `Game.c`.
 *
 * The level loader is a non-suspend callback so this engine stays in the
 * synchronous update path. The caller is responsible for pre-loading level
 * data into whatever backing store the callback reads from.
 */
class GameEngine(
    private val rng: GameRng = GameRng(),
    private val levelLoader: (Int) -> ByteArray? = { null },
) {
    /** Reset back to the splash screen and seed starting values. */
    fun reset(state: GameState) {
        state.players[0].lives = GameConfig.PLAYER_START_LIVES
        state.players[0].score = 0
        state.players[0].type = 0
        state.players[0].speed = 2.0f
        state.players[0].active = true
        if (!state.single) {
            state.players[1].lives = GameConfig.PLAYER_START_LIVES
            state.players[1].score = 0
            state.players[1].type = 0
            state.players[1].speed = 2.0f
            state.players[1].active = true
            state.maxEnemies = GameConfig.MAX_ENEMIES_TWO_PLAYER
        } else {
            state.players[1].active = false
            state.maxEnemies = GameConfig.MAX_ENEMIES_SINGLE
        }
        state.phase = GamePhase.ACTIVE
        state.level = 1
        initLevel(state, 1)
    }

    /**
     * Initialise a level: load the map, reset slots, fill in player positions.
     * Falls back to level 1 if [level] cannot be loaded.
     */
    fun initLevel(state: GameState, level: Int) {
        state.level = level
        val data = levelLoader(level) ?: levelLoader(1) ?: return
        val parsed = MapLoader.parse(data)
        copyInto(parsed, state.plane)

        if (state.players[0].lives > 0) resetPlayer(state.players[0],
            GameConfig.PLAYER1_START_X, GameConfig.PLAYER1_START_Y)
        if (state.single) {
            state.players[1].active = false
        } else if (state.players[1].lives > 0) {
            resetPlayer(state.players[1], GameConfig.PLAYER2_START_X, GameConfig.PLAYER2_START_Y)
        }

        for (k in 0 until 2) {
            state.players[k].bullets.forEach { it.active = false }
        }
        for (i in 0 until GameConfig.ENEMY_NUM) {
            state.enemies[i].active = false
            state.enemies[i].bullets[0].active = false
        }
        state.explodes.slots.forEach { it.active = false }
        state.bonus.active = false
        state.enemyLocked = false
        state.enemiesTotal = GameConfig.ENEMIES_PER_LEVEL
        state.enemiesLeft = GameConfig.ENEMIES_PER_LEVEL
    }

    private fun copyInto(src: Plane, dst: Plane) {
        for (i in 0 until GameConfig.PLANE_H) {
            for (j in 0 until GameConfig.PLANE_W) {
                dst.map[i][j].kind = src.map[i][j].kind
                dst.map[i][j].mask = src.map[i][j].mask
            }
        }
        dst.riverFrame1 = src.riverFrame1
        dst.protected = src.protected
        dst.protectStartMs = src.protectStartMs
    }

    private fun resetPlayer(p: Player, x: Int, y: Int) {
        p.x = x
        p.y = y
        p.dir = Direction.UP
        p.type = 0
        p.speed = 2.0f
        p.reborn()
    }

    /** Per-tick update. Drives state machine and entity simulation. */
    fun update(state: GameState, input: InputState, now: Long = nowMs()) {
        handleSystemInput(state, input)
        when (state.phase) {
            GamePhase.ACTIVE -> {
                processPlayers(state, input, now)
                processBullets(state, now)
                processEnemies(state, now)
            }
            GamePhase.WIN -> {
                if (now - state.winStartMs > GameConfig.WIN_DELAY_MS) {
                    // The actual `initLevel` call happens in the caller's
                    // coroutine because it needs the suspend `levelLoader`.
                    // Here we just flip the phase and bump the level.
                    state.level++
                    state.phase = GamePhase.ACTIVE
                    state.enemiesTotal = GameConfig.ENEMIES_PER_LEVEL
                    state.enemiesLeft = GameConfig.ENEMIES_PER_LEVEL
                }
            }
            GamePhase.SPLASH, GamePhase.OVER -> { /* static */ }
        }
    }

    private fun handleSystemInput(state: GameState, input: InputState) {
        if (KeyBits.ESC in input.system) {
            state.phase = when (state.phase) {
                GamePhase.ACTIVE -> GamePhase.SPLASH
                GamePhase.SPLASH -> { state.bored = true; GamePhase.SPLASH }
                GamePhase.OVER -> GamePhase.SPLASH
                GamePhase.WIN -> state.phase
            }
        }
    }

    private fun processPlayers(state: GameState, input: InputState, now: Long) {
        val inputs = arrayOf(input.p1, input.p2)
        for (k in 0 until 2) {
            val p = state.players[k]
            if (!p.active) continue
            val oldX = p.x; val oldY = p.y; val oldDir = p.dir

            if (!p.bore && state.phase != GamePhase.OVER) {
                p.processInput(inputs[k], now)
            }
            val surface = state.plane.getSurface(p.x, p.y, p.w, p.h, p.dir)
            if (surface != TileKind.NULL && surface != TileKind.TREE) {
                p.x = oldX; p.y = oldY; p.dir = oldDir
            }
            for (i in 0 until state.maxEnemies) {
                val e = state.enemies[i]
                if (e.active && !e.bore && p.hitTest(e, oldX, oldY)) {
                    p.x = oldX; p.y = oldY; p.dir = oldDir
                    break
                }
            }
            if (state.bonus.active && hitTest(p, state.bonus)) {
                eatBonusPlayer(state, p)
            }
        }
    }

    private fun processBullets(state: GameState, now: Long) {
        for (k in 0 until 2) {
            val p = state.players[k]
            for (j in p.bullets.indices) {
                val b = p.bullets[j]
                if (!b.active) continue
                if (!b.move()) {
                    b.active = false
                    continue
                }
                val destroyConcrete = p.type >= 3
                val surface = state.plane.hitSurface(b.x, b.y, b.w, b.h, b.dir, destroyConcrete)
                if (surface == TileKind.BRICK || surface == TileKind.CONCRETE) {
                    b.active = false
                    spawnExplode(state, b.x + b.w / 2, b.y + b.h / 2, big = false, now)
                } else if (surface == TileKind.HAWK) {
                    b.active = false
                    spawnExplode(state, b.x + b.w / 2, b.y + b.h / 2, big = true, now)
                    state.phase = GamePhase.OVER
                }
                for (i in 0 until state.maxEnemies) {
                    val e = state.enemies[i]
                    if (!e.bore && !e.shield && hitTest(b, e)) {
                        b.active = false
                        spawnExplode(state, b.x + b.w / 2, b.y + b.h / 2, big = true, now)
                        state.enemiesTotal--
                        if (e.hasBonus) {
                            state.bonus.spawn(rng)
                            e.hasBonus = false
                        }
                        if (e.type == 2) {
                            if (--e.level < 0) e.active = false
                        } else {
                            e.active = false
                        }
                        p.score += (e.type + 1) * 100
                        break
                    }
                }
            }
        }
    }

    private fun processEnemies(state: GameState, now: Long) {
        if (now - state.lastEnemySpawnMs > GameConfig.ENEMY_SPAWN_INTERVAL_MS) {
            state.lastEnemySpawnMs = now
            spawnEnemy(state)
        }
        if (state.enemyLocked && now - state.lockStartMs > GameConfig.LOCKED_DURATION_MS) {
            state.enemyLocked = false
        }
        for (i in 0 until state.maxEnemies) {
            val e = state.enemies[i]
            if (!e.active || e.bore) continue
            if (state.enemyLocked) continue
            val oldX = e.x; val oldY = e.y; val oldDir = e.dir

            if ((now xor i.toLong() * 31) % 200L == 0L || !e.moveStep()) {
                e.changeDirection(rng)
            }
            var surface = state.plane.getSurface(e.x, e.y, e.w, e.h, e.dir)
            if (surface == TileKind.BRICK) {
                if ((now xor i.toLong() * 17) % 100L < 30) {
                    e.changeDirection(rng)
                    surface = state.plane.getSurface(e.x, e.y, e.w, e.h, e.dir)
                } else {
                    e.fire(now)
                }
            } else if (surface == TileKind.CONCRETE || surface == TileKind.RIVER) {
                e.changeDirection(rng)
                surface = state.plane.getSurface(e.x, e.y, e.w, e.h, e.dir)
            }
            for (k in 0 until 2) {
                val pl = state.players[k]
                if (pl.active && e.hitTest(pl, oldX, oldY)) {
                    e.x = oldX; e.y = oldY; e.dir = oldDir
                    e.fire(now)
                }
            }
            if (surface != TileKind.NULL && surface != TileKind.TREE) {
                e.x = oldX; e.y = oldY; e.dir = oldDir
            }
            for (j in 0 until state.maxEnemies) {
                if (i != j && e.hitTest(state.enemies[j], oldX, oldY)) {
                    e.changeDirection(rng)
                    e.x = oldX; e.y = oldY; e.dir = oldDir
                    break
                }
            }
            if (state.bonus.active && hitTest(e, state.bonus)) {
                eatBonusEnemy(state, e)
            }
            if ((now xor i.toLong() * 11) % 100L == 0L) {
                e.fire(now)
            }

            val b = e.bullets[0]
            if (b.active) {
                if (!b.move()) b.active = false
                val bs = state.plane.hitSurface(b.x, b.y, b.w, b.h, b.dir, destroyConcrete = false)
                if (bs == TileKind.BRICK || bs == TileKind.CONCRETE) {
                    b.active = false
                    spawnExplode(state, b.x + b.w / 2, b.y + b.h / 2, big = false, now)
                } else if (bs == TileKind.HAWK) {
                    b.active = false
                    spawnExplode(state, b.x + b.w / 2, b.y + b.h / 2, big = true, now)
                    state.phase = GamePhase.OVER
                }
                for (k in 0 until 2) {
                    val pl = state.players[k]
                    if (pl.active && hitTest(b, pl)) {
                        b.active = false
                        if (!pl.shield) {
                            spawnExplode(state, b.x + b.w / 2, b.y + b.h / 2, big = true, now)
                            if (pl.lives > 0) pl.lives--
                            if (pl.lives <= 0) {
                                pl.active = false
                            } else {
                                resetPlayer(pl,
                                    if (k == 0) GameConfig.PLAYER1_START_X else GameConfig.PLAYER2_START_X,
                                    if (k == 0) GameConfig.PLAYER1_START_Y else GameConfig.PLAYER2_START_Y)
                            }
                        }
                    }
                }
            }
        }

        if (state.enemiesTotal <= 0 && state.phase == GamePhase.ACTIVE) {
            state.phase = GamePhase.WIN
            state.winStartMs = now
        }
        if (!state.players[0].active && !state.players[1].active) {
            state.phase = GamePhase.OVER
        }
    }

    private fun spawnEnemy(state: GameState) {
        if (state.enemiesLeft <= 0) return
        for (i in 0 until state.maxEnemies) {
            val slot = state.enemies[i]
            if (!slot.active) {
                val (x, y) = when (state.enemySpawnSelector and 3) {
                    0 -> 2 to 2
                    1 -> 194 to 2
                    2 -> 386 to 2
                    else -> 2 to 160
                }
                state.enemySpawnSelector++
                val t = rng.nextInt(100)
                val type = when {
                    t < 50 -> 0
                    t < 80 -> 1
                    else -> 2
                }
                slot.reborn(x, y, type)
                slot.hasBonus = state.enemiesLeft == 4 ||
                    state.enemiesLeft == 11 ||
                    state.enemiesLeft == 18
                state.enemiesLeft--
                return
            }
        }
    }

    private fun spawnExplode(state: GameState, cx: Int, cy: Int, big: Boolean, now: Long) {
        state.explodes.spawn(cx, cy, big, now)
    }

    private fun eatBonusPlayer(state: GameState, p: Player) {
        when (state.bonus.type) {
            BonusType.LIFE -> p.lives++
            BonusType.CLOCK -> { state.enemyLocked = true; state.lockStartMs = nowMs() }
            BonusType.SHOVEL -> state.plane.protect()
            BonusType.STAR -> if (p.type < 3) p.type++
            BonusType.HELMET -> grantShield(p)
            BonusType.BOMB -> {
                for (i in 0 until state.maxEnemies) {
                    val e = state.enemies[i]
                    if (e.active && !e.bore) {
                        spawnExplode(state, e.x + e.w / 2, e.y + e.h / 2, big = true, nowMs())
                        e.active = false
                        state.enemiesTotal--
                    }
                }
            }
        }
        state.bonus.active = false
    }

    private fun eatBonusEnemy(state: GameState, e: Enemy) {
        when (state.bonus.type) {
            BonusType.LIFE -> { state.enemiesTotal += 5; state.enemiesLeft += 5 }
            BonusType.CLOCK -> { state.players[0].locked = true; state.players[0].lockStartMs = nowMs() }
            BonusType.SHOVEL -> state.plane.bare()
            BonusType.BOMB -> {
                for (k in 0 until 2) {
                    val p = state.players[k]
                    if (p.active && p.lives > 0) {
                        spawnExplode(state, p.x + p.w / 2, p.y + p.h / 2, big = true, nowMs())
                        p.lives--
                        if (p.lives <= 0) p.active = false
                        else resetPlayer(p,
                            if (k == 0) GameConfig.PLAYER1_START_X else GameConfig.PLAYER2_START_X,
                            if (k == 0) GameConfig.PLAYER1_START_Y else GameConfig.PLAYER2_START_Y)
                    }
                }
            }
            BonusType.STAR -> { e.type = 2; e.level = 2 }
            BonusType.HELMET -> grantShield(e)
        }
        state.bonus.active = false
    }

    private fun grantShield(t: Tank) {
        val now = nowMs()
        t.shield = true
        t.shieldFrame = true
        t.shieldStartMs = now
        t.shieldEndMs = now + GameConfig.HELMET_SHIELD_MS
        t.flickerStartMs = now
    }
}
