package com.battlecity.engine

/** Top-level state machine phases. Mirrors UEFI's `GAMESTATE` enum. */
enum class GamePhase { SPLASH, ACTIVE, WIN, OVER }

/**
 * Snapshot of the entire game. Mirrors UEFI's `GAME` struct in `Game.h:19-38`.
 *
 * Mutable: the engine mutates slots in place each tick. Render reads them
 * via the `mutableStateOf` wrapper at the top level.
 */
class GameState {
    val plane: Plane = Plane()
    val players: Array<Player> = arrayOf(
        Player(GameConfig.PLAYER1_START_X, GameConfig.PLAYER1_START_Y),
        Player(GameConfig.PLAYER2_START_X, GameConfig.PLAYER2_START_Y),
    )
    val enemies: Array<Enemy> = Array(GameConfig.ENEMY_NUM) { Enemy() }
    val explodes: ExplodePool = ExplodePool()
    val bonus: Bonus = Bonus()

    var phase: GamePhase = GamePhase.SPLASH
    var level: Int = 1
    var enemiesTotal: Int = GameConfig.ENEMIES_PER_LEVEL
    var enemiesLeft: Int = GameConfig.ENEMIES_PER_LEVEL
    var maxEnemies: Int = GameConfig.MAX_ENEMIES_SINGLE
    var single: Boolean = true
    var enemyLocked: Boolean = false
    var lockStartMs: Long = 0L
    var winStartMs: Long = 0L
    var bored: Boolean = false
    var boreStartMs: Long = 0L
    var enemySpawnSelector: Int = 0
    var lastEnemySpawnMs: Long = 0L

    init {
        players[1].active = false
    }
}
