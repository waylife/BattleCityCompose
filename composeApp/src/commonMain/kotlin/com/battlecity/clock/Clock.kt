package com.battlecity.clock

/**
 * Platform-specific monotonic millisecond clock.
 *
 * The UEFI code calibrated TSC ticks; KMP uses `withFrameNanos` for the
 * main loop, but we still want a millisecond-accurate clock for the
 * timestamp-based game logic (e.g. `ShieldStartMs`, `LockStartMs`).
 */
expect fun nowMs(): Long
