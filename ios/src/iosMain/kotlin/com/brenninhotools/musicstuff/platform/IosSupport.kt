package com.brenninhotools.musicstuff.platform

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitSecond
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

class IoFailure(message: String) : Exception(message)

class AccessFailure(message: String) : Exception(message)

class MemoryFailure(message: String) : Exception(message)

fun nowMillis(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()

fun currentStamp(): DateStamp {
    val units = NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay or
        NSCalendarUnitHour or NSCalendarUnitMinute or NSCalendarUnitSecond
    val parts = NSCalendar.currentCalendar.components(units, fromDate = NSDate())
    return DateStamp(
        year = parts.year.toInt(),
        month = parts.month.toInt(),
        day = parts.day.toInt(),
        hour = parts.hour.toInt(),
        minute = parts.minute.toInt(),
        second = parts.second.toInt(),
    )
}
