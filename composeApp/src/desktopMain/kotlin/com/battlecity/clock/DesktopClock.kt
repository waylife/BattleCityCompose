package com.battlecity.clock

actual fun nowMs(): Long = System.nanoTime() / 1_000_000L
