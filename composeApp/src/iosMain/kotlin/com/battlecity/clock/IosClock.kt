package com.battlecity.clock

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

/** Wall-clock-ish millisecond timer. iOS doesn't expose a high-resolution
 *  monotonic counter directly; `NSDate.timeIntervalSince1970` is good enough
 *  for our use (shield/lock/spawn timers). */
actual fun nowMs(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()
