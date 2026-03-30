package com.example.studentcopilot.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val appDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

fun currentLocalDayStartMillis(zoneId: ZoneId = ZoneId.systemDefault()): Long {
    return LocalDate.now(zoneId)
        .atStartOfDay(zoneId)
        .toInstant()
        .toEpochMilli()
}

fun selectedDateMillisToLocalStartOfDay(
    selectedDateMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
): Long {
    val localDate = Instant.ofEpochMilli(selectedDateMillis)
        .atOffset(ZoneOffset.UTC)
        .toLocalDate()
    return localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
}

fun localDateMillisToDatePickerSelectionMillis(
    localDateMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
): Long {
    val localDate = Instant.ofEpochMilli(localDateMillis)
        .atZone(zoneId)
        .toLocalDate()
    return localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

fun formatLocalDateMillis(
    dateMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
): String {
    return Instant.ofEpochMilli(dateMillis)
        .atZone(zoneId)
        .toLocalDate()
        .format(appDateFormatter)
}
