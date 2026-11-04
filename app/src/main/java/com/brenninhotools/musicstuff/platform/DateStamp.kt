package com.brenninhotools.musicstuff.platform

data class DateStamp(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val second: Int,
) {
    private fun date(): String = "$year-${two(month)}-${two(day)}"

    fun fileLabel(): String = "${date()} ${two(hour)}-${two(minute)}-${two(second)}"

    fun displayLabel(): String = "${date()} ${two(hour)}:${two(minute)}:${two(second)}"

    fun delphiDays(): Double {
        val shifted = if (month <= 2) year - 1 else year
        val era = shifted.floorDiv(400)
        val yearOfEra = shifted - era * 400
        val monthIndex = (month + 9) % 12
        val dayOfYear = (153 * monthIndex + 2) / 5 + day - 1
        val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
        val unixDays = era * 146097 + dayOfEra - UNIX_EPOCH_SHIFT
        return unixDays + DELPHI_TO_UNIX_DAYS + (hour * 3600 + minute * 60 + second) / SECONDS_PER_DAY
    }

    private fun two(value: Int): String = value.toString().padStart(2, '0')

    private companion object {
        const val UNIX_EPOCH_SHIFT = 719468
        const val DELPHI_TO_UNIX_DAYS = 25569
        const val SECONDS_PER_DAY = 86400.0
    }
}
