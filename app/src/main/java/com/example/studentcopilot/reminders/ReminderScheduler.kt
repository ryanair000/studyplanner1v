package com.example.studentcopilot.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.studentcopilot.data.local.database.AppDatabase
import com.example.studentcopilot.util.minuteOfDayLabel
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReminderScheduler(
    private val context: Context,
    private val database: AppDatabase,
) {
    private val workManager by lazy { WorkManager.getInstance(context) }

    fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Pangia reminders",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Assignment, exam, and class reminders"
            },
        )
    }

    suspend fun cancelAllForOwner(ownerUserId: String) = withContext(Dispatchers.IO) {
        if (ownerUserId.isBlank()) return@withContext
        workManager.cancelAllWorkByTag(ownerTag(ownerUserId)).result.get()
    }

    suspend fun rescheduleAll(ownerUserId: String) = withContext(Dispatchers.IO) {
        if (ownerUserId.isBlank()) return@withContext

        cancelAllForOwner(ownerUserId)

        val now = System.currentTimeMillis()
        val courses = database.courseDao().getAllSnapshot(ownerUserId)
        val assignments = database.assignmentDao().getAllSnapshot(ownerUserId)
        val exams = database.examDao().getAllSnapshot(ownerUserId)
        val courseNamesById = courses.associateBy({ it.id }, { it.name })

        assignments
            .filter { !it.isCompleted }
            .forEach { assignment ->
                val triggerAt = ReminderTimeCalculator.assignmentReminderAt(assignment.dueDate)
                if (triggerAt <= now) return@forEach
                val courseName = courseNamesById[assignment.courseId]
                enqueueAssignmentReminder(
                    ownerUserId = ownerUserId,
                    assignmentId = assignment.id,
                    triggerAt = triggerAt,
                    title = "Assignment due today",
                    message = buildString {
                        append(assignment.title)
                        courseName?.let {
                            append(" for ")
                            append(it)
                        }
                        append(" is due today.")
                    },
                )
            }

        exams.forEach { exam ->
            val triggerAt = ReminderTimeCalculator.examReminderAt(exam.date)
            if (triggerAt <= now) return@forEach
            val courseName = courseNamesById[exam.courseId]
            enqueueExamReminder(
                ownerUserId = ownerUserId,
                examId = exam.id,
                triggerAt = triggerAt,
                title = "Exam today",
                message = buildString {
                    append(exam.title)
                    courseName?.let {
                        append(" for ")
                        append(it)
                    }
                    append(" is scheduled for today.")
                },
            )
        }

        courses.forEach { course ->
            val classDay = course.classDayOfWeek ?: return@forEach
            val classStartMinute = course.classStartMinuteOfDay ?: return@forEach
            val triggerAt = ReminderTimeCalculator.nextClassReminderAt(
                classDayOfWeek = classDay,
                classStartMinuteOfDay = classStartMinute,
                nowMillis = now,
            )
            val initialDelay = (triggerAt - now).coerceAtLeast(0L)
            val workName = classWorkName(ownerUserId, course.id)
            workManager.enqueueUniquePeriodicWork(
                workName,
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<ReminderWorker>(7, TimeUnit.DAYS)
                    .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                    .setInputData(
                        reminderData(
                            title = "Class starting soon",
                            message = "${course.name} starts at ${minuteOfDayLabel(classStartMinute)}.",
                            notificationId = workName.hashCode(),
                        ),
                    )
                    .addTag(ownerTag(ownerUserId))
                    .build(),
            )
        }
    }

    private fun enqueueAssignmentReminder(
        ownerUserId: String,
        assignmentId: Long,
        triggerAt: Long,
        title: String,
        message: String,
    ) {
        val workName = assignmentWorkName(ownerUserId, assignmentId)
        val delay = (triggerAt - System.currentTimeMillis()).coerceAtLeast(0L)
        workManager.enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(reminderData(title, message, workName.hashCode()))
                .addTag(ownerTag(ownerUserId))
                .build(),
        )
    }

    private fun enqueueExamReminder(
        ownerUserId: String,
        examId: Long,
        triggerAt: Long,
        title: String,
        message: String,
    ) {
        val workName = examWorkName(ownerUserId, examId)
        val delay = (triggerAt - System.currentTimeMillis()).coerceAtLeast(0L)
        workManager.enqueueUniqueWork(
            workName,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(reminderData(title, message, workName.hashCode()))
                .addTag(ownerTag(ownerUserId))
                .build(),
        )
    }

    private fun reminderData(title: String, message: String, notificationId: Int): Data {
        return Data.Builder()
            .putString(ReminderWorker.KEY_TITLE, title)
            .putString(ReminderWorker.KEY_MESSAGE, message)
            .putInt(ReminderWorker.KEY_NOTIFICATION_ID, notificationId)
            .build()
    }

    private fun ownerTag(ownerUserId: String) = "pangia-reminders-$ownerUserId"

    private fun assignmentWorkName(ownerUserId: String, assignmentId: Long) =
        "pangia-assignment-reminder-$ownerUserId-$assignmentId"

    private fun examWorkName(ownerUserId: String, examId: Long) =
        "pangia-exam-reminder-$ownerUserId-$examId"

    private fun classWorkName(ownerUserId: String, courseId: Long) =
        "pangia-class-reminder-$ownerUserId-$courseId"

    companion object {
        const val CHANNEL_ID = "pangia_reminders"
    }
}
