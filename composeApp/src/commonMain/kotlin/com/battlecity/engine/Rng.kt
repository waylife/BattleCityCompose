package com.battlecity.engine

import kotlin.random.Random

/** Injectable RNG. The engine accepts one so tests can be deterministic. */
class GameRng(val random: Random = Random.Default) {
    fun nextInt(bound: Int): Int = random.nextInt(bound)
    fun nextFloat(): Float = random.nextFloat()
}
