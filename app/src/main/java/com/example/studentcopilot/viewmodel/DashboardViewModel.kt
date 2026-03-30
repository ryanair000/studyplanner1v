package com.example.studentcopilot.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.studentcopilot.StudentCopilotApp
import com.example.studentcopilot.util.currentLocalDayStartMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

data class DashboardState(
    val upcomingAssignments: Int = 0,
    val upcomingExams: Int = 0,
    val nearestDeadlineMillis: Long? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    val state: StateFlow<DashboardState>

    init {
        val db = (application as StudentCopilotApp).database
        val assignmentDao = db.assignmentDao()
        val examDao = db.examDao()

        state = todayStartFlow().flatMapLatest { now ->
            combine(
                assignmentDao.countUpcoming(now),
                examDao.countUpcoming(now),
                assignmentDao.nearestDeadline(now),
                examDao.nearestDate(now),
            ) { assignCount, examCount, nearestAssignment, nearestExam ->
                val nearest = listOfNotNull(nearestAssignment, nearestExam).minOrNull()
                DashboardState(
                    upcomingAssignments = assignCount,
                    upcomingExams = examCount,
                    nearestDeadlineMillis = nearest,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardState())
    }

    private fun todayStartFlow(zoneId: ZoneId = ZoneId.systemDefault()) = flow {
        while (true) {
            emit(currentLocalDayStartMillis(zoneId))

            val nextDayStart = LocalDate.now(zoneId)
                .plusDays(1)
                .atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli()
            delay((nextDayStart - System.currentTimeMillis()).coerceAtLeast(60_000L))
        }
    }
}
