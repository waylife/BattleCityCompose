package com.battlecity.clock

actual fun nowMs(): Long = android.os.SystemClock.elapsedRealtime()
