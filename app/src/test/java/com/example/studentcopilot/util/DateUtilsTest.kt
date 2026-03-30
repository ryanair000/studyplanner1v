package com.example.studentcopilot.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class DateUtilsTest {

    @Test
    fun selectedDateMillisToLocalStartOfDay_preservesPickedDateInWesternTimezone() {
        val zoneId = ZoneId.of("America/Los_Angeles")
        val pickedDate = LocalDate.of(2026, 3, 30)
        val datePickerSelection = pickedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        val storedMillis = selectedDateMillisToLocalStartOfDay(datePickerSelection, zoneId)
        val storedDate = Instant.ofEpochMilli(storedMillis).atZone(zoneId).toLocalDate()

        assertEquals(pickedDate, storedDate)
    }

    @Test
    fun datePickerSelection_roundTripsBackToStoredLocalDate() {
        val zoneId = ZoneId.of("Africa/Nairobi")
        val pickedDate = LocalDate.of(2026, 4, 5)
        val storedMillis = pickedDate.atStartOfDay(zoneId).toInstant().toEpochMilli()

        val datePickerSelection = localDateMillisToDatePickerSelectionMillis(storedMillis, zoneId)
        val roundTrippedMillis = selectedDateMillisToLocalStartOfDay(datePickerSelection, zoneId)

        assertEquals(storedMillis, roundTrippedMillis)
    }
}
