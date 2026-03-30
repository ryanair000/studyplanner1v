package com.example.studentcopilot.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.studentcopilot.StudentCopilotApp
import com.example.studentcopilot.data.local.entity.AssignmentEntity
import com.example.studentcopilot.data.local.entity.CourseEntity
import com.example.studentcopilot.data.local.entity.ExamEntity
import com.example.studentcopilot.data.local.entity.TimetableEntryEntity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class CalendarState(
    val selectedDate: LocalDate = LocalDate.now(),
    val monthLabel: String = "",
    val visibleWeek: List<LocalDate> = emptyList(),
    val activityCounts: Map<LocalDate, Int> = emptyMap(),
    val agendaItems: List<CalendarAgendaItem> = emptyList(),
)

sealed interface CalendarAgendaItem {
    val stableId: String
    val sortKey: Int

    data class Class(
        override val stableId: String,
        override val sortKey: Int,
        val timeLabel: String,
        val title: String,
        val badge: String,
        val venue: String?,
        val secondaryMeta: String?,
        val isOngoing: Boolean,
        val progress: Float,
    ) : CalendarAgendaItem

    data class Assignment(
        override val stableId: String,
        override val sortKey: Int,
        val urgencyLabel: String,
        val title: String,
        val courseName: String?,
    ) : CalendarAgendaItem

    data class Exam(
        override val stableId: String,
        override val sortKey: Int,
        val timeLabel: String,
        val title: String,
        val subtitle: String?,
        val meta: String?,
    ) : CalendarAgendaItem
}

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val zoneId = ZoneId.systemDefault()
    private val selectedDate = MutableStateFlow(LocalDate.now(zoneId))
    val state: StateFlow<CalendarState>

    init {
        val app = application as StudentCopilotApp
        val db = app.database
        val ownerUserId = app.authRepository.currentUserIdOrNull().orEmpty()

        state = combine(
            selectedDate,
            db.courseDao().getAll(ownerUserId),
            db.timetableEntryDao().getAll(ownerUserId),
            db.assignmentDao().getAll(ownerUserId),
            db.examDao().getAll(ownerUserId),
        ) { selected, courses, timetableEntries, assignments, exams ->
            buildState(
                selectedDate = selected,
                courses = courses,
                timetableEntries = timetableEntries,
                assignments = assignments,
                exams = exams,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            CalendarState(
                selectedDate = LocalDate.now(zoneId),
                monthLabel = LocalDate.now(zoneId).format(MONTH_FORMATTER),
                visibleWeek = visibleWeek(LocalDate.now(zoneId)),
            ),
        )
    }

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
    }

    fun shiftWeek(weeks: Long) {
        selectedDate.update { it.plusWeeks(weeks) }
    }

    fun jumpToToday() {
        selectedDate.value = LocalDate.now(zoneId)
    }

    private fun buildState(
        selectedDate: LocalDate,
        courses: List<CourseEntity>,
        timetableEntries: List<TimetableEntryEntity>,
        assignments: List<AssignmentEntity>,
        exams: List<ExamEntity>,
    ): CalendarState {
        val visibleWeek = visibleWeek(selectedDate)
        val activityCounts = buildActivityCounts(
            visibleWeek = visibleWeek,
            assignments = assignments,
            exams = exams,
            timetableEntries = timetableEntries,
        )

        val courseNamesById = courses.associate { it.id to it.name }
        val agendaItems = buildList {
            val today = LocalDate.now(zoneId)
            val nowMinute = LocalTime.now(zoneId).let { (it.hour * 60) + it.minute }

            timetableEntries
                .filter { it.dayOfWeek == selectedDate.dayOfWeek.value }
                .sortedBy { it.startMinuteOfDay }
                .forEach { entry ->
                    val defaultEnd = (entry.endMinuteOfDay ?: entry.startMinuteOfDay + DEFAULT_CLASS_DURATION_MINUTES)
                        .coerceAtMost((23 * 60) + 59)
                    val isOngoing = selectedDate == today && nowMinute in entry.startMinuteOfDay until defaultEnd
                    val progress = if (isOngoing) {
                        ((nowMinute - entry.startMinuteOfDay).toFloat() /
                            (defaultEnd - entry.startMinuteOfDay).coerceAtLeast(1).toFloat())
                            .coerceIn(0f, 1f)
                    } else {
                        0f
                    }

                    add(
                        CalendarAgendaItem.Class(
                            stableId = "class-${entry.id}",
                            sortKey = if (isOngoing) entry.startMinuteOfDay - 1_000 else entry.startMinuteOfDay,
                            timeLabel = buildTimeLabel(
                                startMinute = entry.startMinuteOfDay,
                                endMinute = entry.endMinuteOfDay,
                                isOngoing = isOngoing,
                            ),
                            title = courseNamesById[entry.courseId] ?: "Course session",
                            badge = entry.classType?.uppercase().orEmpty().ifBlank { "CLASS" },
                            venue = entry.venue,
                            secondaryMeta = listOfNotNull(
                                entry.lecturer,
                                entry.section?.takeIf { it.isNotBlank() }?.let { "Section $it" },
                            ).firstOrNull(),
                            isOngoing = isOngoing,
                            progress = progress,
                        ),
                    )
                }

            exams
                .filter { exam -> exam.date.toLocalDate(zoneId) == selectedDate }
                .forEach { exam ->
                    add(
                        CalendarAgendaItem.Exam(
                            stableId = "exam-${exam.id}",
                            sortKey = EXAM_SORT_KEY,
                            timeLabel = exam.type.uppercase(),
                            title = exam.title,
                            subtitle = courseNamesById[exam.courseId],
                            meta = "Exam day",
                        ),
                    )
                }

            assignments
                .filter { assignment -> assignment.dueDate.toLocalDate(zoneId) == selectedDate }
                .forEach { assignment ->
                    add(
                        CalendarAgendaItem.Assignment(
                            stableId = "assignment-${assignment.id}",
                            sortKey = ASSIGNMENT_SORT_KEY,
                            urgencyLabel = if (selectedDate == today) "Due today" else "Deadline",
                            title = assignment.title,
                            courseName = courseNamesById[assignment.courseId],
                        ),
                    )
                }
        }.sortedBy { it.sortKey }

        return CalendarState(
            selectedDate = selectedDate,
            monthLabel = selectedDate.format(MONTH_FORMATTER),
            visibleWeek = visibleWeek,
            activityCounts = activityCounts,
            agendaItems = agendaItems,
        )
    }

    private fun buildActivityCounts(
        visibleWeek: List<LocalDate>,
        assignments: List<AssignmentEntity>,
        exams: List<ExamEntity>,
        timetableEntries: List<TimetableEntryEntity>,
    ): Map<LocalDate, Int> {
        val counts = visibleWeek.associateWith { 0 }.toMutableMap()

        timetableEntries.forEach { entry ->
            visibleWeek.firstOrNull { it.dayOfWeek.value == entry.dayOfWeek }?.let { date ->
                counts[date] = counts.getOrDefault(date, 0) + 1
            }
        }
        assignments.forEach { assignment ->
            val localDate = assignment.dueDate.toLocalDate(zoneId)
            if (localDate in counts) {
                counts[localDate] = counts.getOrDefault(localDate, 0) + 1
            }
        }
        exams.forEach { exam ->
            val localDate = exam.date.toLocalDate(zoneId)
            if (localDate in counts) {
                counts[localDate] = counts.getOrDefault(localDate, 0) + 1
            }
        }

        return counts
    }

    private fun buildTimeLabel(
        startMinute: Int,
        endMinute: Int?,
        isOngoing: Boolean,
    ): String {
        val start = minuteLabel(startMinute)
        val end = endMinute?.let(::minuteLabel)
        return if (isOngoing) {
            "Now — $start"
        } else {
            listOfNotNull(start, end).joinToString(" — ")
        }
    }

    private fun minuteLabel(minuteOfDay: Int): String {
        return LocalTime.of(minuteOfDay / 60, minuteOfDay % 60).format(TIME_FORMATTER)
    }

    private fun Long.toLocalDate(zoneId: ZoneId): LocalDate {
        return Instant.ofEpochMilli(this)
            .atZone(zoneId)
            .toLocalDate()
    }

    private fun visibleWeek(selectedDate: LocalDate): List<LocalDate> {
        val start = selectedDate.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        return (0L..6L).map { start.plusDays(it) }
    }

    private companion object {
        const val DEFAULT_CLASS_DURATION_MINUTES = 90
        const val EXAM_SORT_KEY = 700
        const val ASSIGNMENT_SORT_KEY = 900
        val MONTH_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
        val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    }
}
