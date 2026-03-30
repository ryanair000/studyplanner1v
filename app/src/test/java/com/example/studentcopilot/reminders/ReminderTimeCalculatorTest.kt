package com.example.studentcopilot.reminders

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderTimeCalculatorTest {

    private val zoneId = ZoneId.of("Africa/Nairobi")

    @Test
    fun assignmentReminderAt_returnsEightAmOnDueDate() {
        val dueDate = ZonedDateTime.of(2026, 4, 2, 0, 0, 0, 0, zoneId)
            .toInstant()
            .toEpochMilli()

        val reminder = ReminderTimeCalculator.assignmentReminderAt(dueDate, zoneId)

        assertEquals(
            ZonedDateTime.of(2026, 4, 2, 8, 0, 0, 0, zoneId).toInstant().toEpochMilli(),
            reminder,
        )
    }

    @Test
    fun nextClassReminderAt_returnsUpcomingWeekWhenTodayReminderAlreadyPassed() {
        val now = ZonedDateTime.of(2026, 4, 6, 9, 45, 0, 0, zoneId)
            .toInstant()
            .toEpochMilli()

        val reminder = ReminderTimeCalculator.nextClassReminderAt(
            classDayOfWeek = 1,
            classStartMinuteOfDay = 10 * 60,
            nowMillis = now,
            zoneId = zoneId,
        )

        assertEquals(
            ZonedDateTime.of(2026, 4, 13, 9, 30, 0, 0, zoneId).toInstant().toEpochMilli(),
            reminder,
        )
    }

    @Test
    fun nextClassReminderAt_returnsSameDayWhenReminderStillAhead() {
        val now = ZonedDateTime.of(2026, 4, 6, 9, 20, 0, 0, zoneId)
            .toInstant()
            .toEpochMilli()

        val reminder = ReminderTimeCalculator.nextClassReminderAt(
            classDayOfWeek = 1,
            classStartMinuteOfDay = 10 * 60,
            nowMillis = now,
            zoneId = zoneId,
        )

        assertEquals(
            ZonedDateTime.of(2026, 4, 6, 9, 30, 0, 0, zoneId).toInstant().toEpochMilli(),
            reminder,
        )
    }
}
