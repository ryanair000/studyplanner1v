package com.example.studentcopilot.reminders

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

object ReminderTimeCalculator {
    private const val DUE_ITEM_REMINDER_HOUR = 8
    private const val DUE_ITEM_REMINDER_MINUTE = 0
    private const val CLASS_REMINDER_LEAD_MINUTES = 30L

    fun assignmentReminderAt(
        dueDateMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long {
        return reminderAtStartOfDate(dueDateMillis, zoneId)
    }

    fun examReminderAt(
        examDateMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long {
        return reminderAtStartOfDate(examDateMillis, zoneId)
    }

    fun nextClassReminderAt(
        classDayOfWeek: Int,
        classStartMinuteOfDay: Int,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val now = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val targetDay = DayOfWeek.of(classDayOfWeek)
        val targetTime = LocalTime.of(classStartMinuteOfDay / 60, classStartMinuteOfDay % 60)

        var nextClassStart = now
            .toLocalDate()
            .with(TemporalAdjusters.nextOrSame(targetDay))
            .atTime(targetTime)
            .atZone(zoneId)

        if (nextClassStart.minusMinutes(CLASS_REMINDER_LEAD_MINUTES).toInstant().toEpochMilli() <= nowMillis) {
            nextClassStart = nextClassStart.plusWeeks(1)
        }

        return nextClassStart.minusMinutes(CLASS_REMINDER_LEAD_MINUTES).toInstant().toEpochMilli()
    }

    private fun reminderAtStartOfDate(
        dateMillis: Long,
        zoneId: ZoneId,
    ): Long {
        val localDate = Instant.ofEpochMilli(dateMillis)
            .atZone(zoneId)
            .toLocalDate()
        return localDate
            .atTime(DUE_ITEM_REMINDER_HOUR, DUE_ITEM_REMINDER_MINUTE)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
    }
}
