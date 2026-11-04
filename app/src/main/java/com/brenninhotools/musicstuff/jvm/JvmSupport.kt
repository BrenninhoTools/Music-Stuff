package com.brenninhotools.musicstuff.platform

import java.time.LocalDateTime

typealias IoFailure = java.io.IOException

typealias AccessFailure = SecurityException

typealias MemoryFailure = OutOfMemoryError

fun nowMillis(): Long = System.currentTimeMillis()

fun currentStamp(): DateStamp {
    val now = LocalDateTime.now()
    return DateStamp(now.year, now.monthValue, now.dayOfMonth, now.hour, now.minute, now.second)
}
