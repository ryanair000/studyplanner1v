package com.example.studentcopilot.util

import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val classTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

fun formatClassSchedule(
    classDayOfWeek: Int?,
    classStartMinuteOfDay: Int?,
    locale: Locale = Locale.getDefault(),
): String? {
    if (classDayOfWeek == null || classStartMinuteOfDay == null) return null
    return "${dayOfWeekLabel(classDayOfWeek, locale)} at ${minuteOfDayLabel(classStartMinuteOfDay)}"
}

fun minuteOfDayLabel(minuteOfDay: Int): String {
    return LocalTime.of(minuteOfDay / 60, minuteOfDay % 60).format(classTimeFormatter)
}

fun dayOfWeekLabel(dayOfWeek: Int, locale: Locale = Locale.getDefault()): String {
    return DayOfWeek.of(dayOfWeek).getDisplayName(TextStyle.SHORT, locale)
}
