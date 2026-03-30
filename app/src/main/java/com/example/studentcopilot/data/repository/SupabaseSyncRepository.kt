package com.example.studentcopilot.data.repository

import androidx.room.withTransaction
import com.example.studentcopilot.BuildConfig
import com.example.studentcopilot.data.local.database.AppDatabase
import com.example.studentcopilot.data.local.entity.AssignmentEntity
import com.example.studentcopilot.data.local.entity.CourseEntity
import com.example.studentcopilot.data.local.entity.ExamEntity
import com.example.studentcopilot.data.local.entity.TimetableEntryEntity
import com.example.studentcopilot.reminders.ReminderScheduler
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

sealed interface SyncResult {
    data object Success : SyncResult
    data class Failure(val message: String) : SyncResult
}

class SupabaseSyncRepository(
    private val database: AppDatabase,
    private val authRepository: AuthRepository,
    private val reminderScheduler: ReminderScheduler,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun syncCurrentUser(): SyncResult = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            val session = authRepository.restoreSession()
                ?: return@runCatching SyncResult.Failure("Sign in again to resume cloud sync.")
            if (authRepository.isGuestSession(session)) {
                reminderScheduler.rescheduleAll(session.userId)
                return@runCatching SyncResult.Success
            }

            uploadPendingCourses(session)
            uploadPendingTimetableEntries(session)
            uploadPendingAssignments(session)
            uploadPendingExams(session)

            val remoteCourses = fetchCourses(session)
            val remoteTimetableEntries = fetchTimetableEntries(session)
            val remoteAssignments = fetchAssignments(session)
            val remoteExams = fetchExams(session)
            replaceLocalSnapshot(
                ownerUserId = session.userId,
                remoteCourses = remoteCourses,
                remoteTimetableEntries = remoteTimetableEntries,
                remoteAssignments = remoteAssignments,
                remoteExams = remoteExams,
            )
            reminderScheduler.rescheduleAll(session.userId)
            SyncResult.Success
        }.getOrElse { throwable ->
            SyncResult.Failure(throwable.toReadableMessage())
        }
    }

    suspend fun addCourse(
        ownerUserId: String,
        name: String,
        classDayOfWeek: Int?,
        classStartMinuteOfDay: Int?,
    ) = withContext(Dispatchers.IO) {
        val trimmedName = name.trim()
        val session = currentCloudSessionOrNull()
        val remoteCourse = session?.let {
            runCatching {
                insertRemoteCourse(
                    session = it,
                    ownerUserId = ownerUserId,
                    name = trimmedName,
                    classDayOfWeek = classDayOfWeek,
                    classStartMinuteOfDay = classStartMinuteOfDay,
                )
            }.getOrNull()
        }

        val localCourseId = database.courseDao().insert(
            CourseEntity(
                remoteId = remoteCourse?.id,
                ownerUserId = ownerUserId,
                name = trimmedName,
                classDayOfWeek = classDayOfWeek,
                classStartMinuteOfDay = classStartMinuteOfDay,
            ),
        )
        upsertGeneratedCourseScheduleEntry(
            ownerUserId = ownerUserId,
            localCourseId = localCourseId,
            remoteCourseId = remoteCourse?.id,
            classDayOfWeek = classDayOfWeek,
            classStartMinuteOfDay = classStartMinuteOfDay,
            session = session,
        )
        reminderScheduler.rescheduleAll(ownerUserId)
    }

    suspend fun updateCourse(
        ownerUserId: String,
        localId: Long,
        name: String,
        classDayOfWeek: Int?,
        classStartMinuteOfDay: Int?,
    ) = withContext(Dispatchers.IO) {
        val existingCourse = database.courseDao().getById(ownerUserId, localId)
            ?: throw SyncFailureException("Couldn't find that course.")
        val trimmedName = name.trim()

        val session = existingCourse.remoteId?.takeIf { it.isNotBlank() }?.let { remoteId ->
            currentCloudSessionOrNull()
                ?.also {
                    patchRemoteRow(
                        table = "courses",
                        filters = listOf(
                            "id=eq.${encodeValue(remoteId)}",
                            "owner_user_id=eq.${encodeValue(ownerUserId)}",
                        ),
                        accessToken = it.accessToken,
                        body = buildCoursePayload(
                            ownerUserId = ownerUserId,
                            name = trimmedName,
                            classDayOfWeek = classDayOfWeek,
                            classStartMinuteOfDay = classStartMinuteOfDay,
                        ),
                    )
                }
                ?: throw SyncFailureException("Sign in again before updating synced courses.")
        }

        database.courseDao().update(
            existingCourse.copy(
                name = trimmedName,
                classDayOfWeek = classDayOfWeek,
                classStartMinuteOfDay = classStartMinuteOfDay,
            ),
        )
        upsertGeneratedCourseScheduleEntry(
            ownerUserId = ownerUserId,
            localCourseId = localId,
            remoteCourseId = existingCourse.remoteId,
            classDayOfWeek = classDayOfWeek,
            classStartMinuteOfDay = classStartMinuteOfDay,
            session = session,
        )
        reminderScheduler.rescheduleAll(ownerUserId)
    }

    suspend fun deleteCourse(ownerUserId: String, localId: Long) = withContext(Dispatchers.IO) {
        val course = database.courseDao().getById(ownerUserId, localId) ?: return@withContext
        course.remoteId?.takeIf { it.isNotBlank() }?.let { remoteId ->
            val session = currentCloudSessionOrNull()
                ?: throw SyncFailureException("Sign in again before deleting synced courses.")
            deleteRemoteRow(
                table = "courses",
                filters = listOf(
                    "id=eq.${encodeValue(remoteId)}",
                    "owner_user_id=eq.${encodeValue(ownerUserId)}",
                ),
                accessToken = session.accessToken,
            )
        }
        database.courseDao().deleteById(ownerUserId, localId)
        reminderScheduler.rescheduleAll(ownerUserId)
    }

    suspend fun addAssignment(
        ownerUserId: String,
        title: String,
        courseId: Long,
        dueDate: Long,
    ) = withContext(Dispatchers.IO) {
        val trimmedTitle = title.trim()
        val localCourse = database.courseDao().getById(ownerUserId, courseId)
            ?: throw SyncFailureException("Choose a valid course first.")

        val session = currentCloudSessionOrNull()
        val remoteCourseId = session?.let { ensureRemoteCourseId(localCourse, it) }
        val remoteAssignment = if (session != null && remoteCourseId != null) {
            runCatching {
                insertRemoteAssignment(
                    session = session,
                    ownerUserId = ownerUserId,
                    remoteCourseId = remoteCourseId,
                    title = trimmedTitle,
                    dueDate = dueDate,
                    isCompleted = false,
                )
            }.getOrNull()
        } else {
            null
        }

        database.assignmentDao().insert(
            AssignmentEntity(
                remoteId = remoteAssignment?.id,
                ownerUserId = ownerUserId,
                courseId = courseId,
                title = trimmedTitle,
                dueDate = dueDate,
            ),
        )
        reminderScheduler.rescheduleAll(ownerUserId)
    }

    suspend fun toggleAssignmentCompleted(
        ownerUserId: String,
        localId: Long,
        completed: Boolean,
    ) = withContext(Dispatchers.IO) {
        val assignment = database.assignmentDao().getById(ownerUserId, localId) ?: return@withContext
        assignment.remoteId?.takeIf { it.isNotBlank() }?.let { remoteId ->
            val session = currentCloudSessionOrNull()
                ?: throw SyncFailureException("Sign in again before updating synced assignments.")
            patchRemoteRow(
                table = "assignments",
                filters = listOf(
                    "id=eq.${encodeValue(remoteId)}",
                    "owner_user_id=eq.${encodeValue(ownerUserId)}",
                ),
                accessToken = session.accessToken,
                body = buildJsonObject {
                    put("is_completed", completed)
                },
            )
        }

        database.assignmentDao().setCompleted(ownerUserId, localId, completed)
        reminderScheduler.rescheduleAll(ownerUserId)
    }

    suspend fun deleteAssignment(ownerUserId: String, localId: Long) = withContext(Dispatchers.IO) {
        val assignment = database.assignmentDao().getById(ownerUserId, localId) ?: return@withContext
        assignment.remoteId?.takeIf { it.isNotBlank() }?.let { remoteId ->
            val session = currentCloudSessionOrNull()
                ?: throw SyncFailureException("Sign in again before deleting synced assignments.")
            deleteRemoteRow(
                table = "assignments",
                filters = listOf(
                    "id=eq.${encodeValue(remoteId)}",
                    "owner_user_id=eq.${encodeValue(ownerUserId)}",
                ),
                accessToken = session.accessToken,
            )
        }
        database.assignmentDao().deleteById(ownerUserId, localId)
        reminderScheduler.rescheduleAll(ownerUserId)
    }

    suspend fun addExam(
        ownerUserId: String,
        title: String,
        courseId: Long,
        date: Long,
        type: String,
    ) = withContext(Dispatchers.IO) {
        val trimmedTitle = title.trim()
        val trimmedType = type.trim()
        val localCourse = database.courseDao().getById(ownerUserId, courseId)
            ?: throw SyncFailureException("Choose a valid course first.")

        val session = currentCloudSessionOrNull()
        val remoteCourseId = session?.let { ensureRemoteCourseId(localCourse, it) }
        val remoteExam = if (session != null && remoteCourseId != null) {
            runCatching {
                insertRemoteExam(
                    session = session,
                    ownerUserId = ownerUserId,
                    remoteCourseId = remoteCourseId,
                    title = trimmedTitle,
                    date = date,
                    type = trimmedType,
                )
            }.getOrNull()
        } else {
            null
        }

        database.examDao().insert(
            ExamEntity(
                remoteId = remoteExam?.id,
                ownerUserId = ownerUserId,
                courseId = courseId,
                title = trimmedTitle,
                date = date,
                type = trimmedType,
            ),
        )
        reminderScheduler.rescheduleAll(ownerUserId)
    }

    suspend fun deleteExam(ownerUserId: String, localId: Long) = withContext(Dispatchers.IO) {
        val exam = database.examDao().getById(ownerUserId, localId) ?: return@withContext
        exam.remoteId?.takeIf { it.isNotBlank() }?.let { remoteId ->
            val session = currentCloudSessionOrNull()
                ?: throw SyncFailureException("Sign in again before deleting synced exams.")
            deleteRemoteRow(
                table = "exams",
                filters = listOf(
                    "id=eq.${encodeValue(remoteId)}",
                    "owner_user_id=eq.${encodeValue(ownerUserId)}",
                ),
                accessToken = session.accessToken,
            )
        }
        database.examDao().deleteById(ownerUserId, localId)
        reminderScheduler.rescheduleAll(ownerUserId)
    }

    private suspend fun currentCloudSessionOrNull(): AuthSession? {
        return authRepository.restoreSession()?.takeUnless { authRepository.isGuestSession(it) }
    }

    private suspend fun uploadPendingCourses(session: AuthSession) {
        val courses = database.courseDao().getAllSnapshot(session.userId)
        courses.filter { it.remoteId.isNullOrBlank() }.forEach { course ->
            val remoteCourse = insertRemoteCourse(
                session = session,
                ownerUserId = session.userId,
                name = course.name,
                classDayOfWeek = course.classDayOfWeek,
                classStartMinuteOfDay = course.classStartMinuteOfDay,
            )
            database.courseDao().updateRemoteId(session.userId, course.id, remoteCourse.id)
        }
    }

    private suspend fun uploadPendingTimetableEntries(session: AuthSession) {
        val entries = database.timetableEntryDao().getAllSnapshot(session.userId)
        entries.filter { it.remoteId.isNullOrBlank() }.forEach { entry ->
            val localCourse = database.courseDao().getById(session.userId, entry.courseId)
                ?: return@forEach
            val remoteCourseId = ensureRemoteCourseId(localCourse, session) ?: return@forEach
            val remoteEntry = insertRemoteTimetableEntry(
                session = session,
                ownerUserId = session.userId,
                remoteCourseId = remoteCourseId,
                dayOfWeek = entry.dayOfWeek,
                startMinuteOfDay = entry.startMinuteOfDay,
                endMinuteOfDay = entry.endMinuteOfDay,
                classType = entry.classType,
                section = entry.section,
                venue = entry.venue,
                lecturer = entry.lecturer,
                weekPattern = entry.weekPattern,
                source = entry.source,
            )
            database.timetableEntryDao().updateRemoteId(session.userId, entry.id, remoteEntry.id)
        }
    }

    private suspend fun uploadPendingAssignments(session: AuthSession) {
        val assignments = database.assignmentDao().getAllSnapshot(session.userId)
        assignments.filter { it.remoteId.isNullOrBlank() }.forEach { assignment ->
            val localCourse = database.courseDao().getById(session.userId, assignment.courseId)
                ?: return@forEach
            val remoteCourseId = ensureRemoteCourseId(localCourse, session) ?: return@forEach
            val remoteAssignment = insertRemoteAssignment(
                session = session,
                ownerUserId = session.userId,
                remoteCourseId = remoteCourseId,
                title = assignment.title,
                dueDate = assignment.dueDate,
                isCompleted = assignment.isCompleted,
            )
            database.assignmentDao().updateRemoteId(session.userId, assignment.id, remoteAssignment.id)
        }
    }

    private suspend fun uploadPendingExams(session: AuthSession) {
        val exams = database.examDao().getAllSnapshot(session.userId)
        exams.filter { it.remoteId.isNullOrBlank() }.forEach { exam ->
            val localCourse = database.courseDao().getById(session.userId, exam.courseId)
                ?: return@forEach
            val remoteCourseId = ensureRemoteCourseId(localCourse, session) ?: return@forEach
            val remoteExam = insertRemoteExam(
                session = session,
                ownerUserId = session.userId,
                remoteCourseId = remoteCourseId,
                title = exam.title,
                date = exam.date,
                type = exam.type,
            )
            database.examDao().updateRemoteId(session.userId, exam.id, remoteExam.id)
        }
    }

    private suspend fun ensureRemoteCourseId(course: CourseEntity, session: AuthSession): String? {
        course.remoteId?.takeIf { it.isNotBlank() }?.let { return it }
        val remoteCourse = insertRemoteCourse(
            session = session,
            ownerUserId = session.userId,
            name = course.name,
            classDayOfWeek = course.classDayOfWeek,
            classStartMinuteOfDay = course.classStartMinuteOfDay,
        )
        database.courseDao().updateRemoteId(session.userId, course.id, remoteCourse.id)
        return remoteCourse.id
    }

    private suspend fun upsertGeneratedCourseScheduleEntry(
        ownerUserId: String,
        localCourseId: Long,
        remoteCourseId: String?,
        classDayOfWeek: Int?,
        classStartMinuteOfDay: Int?,
        session: AuthSession?,
    ) {
        val dao = database.timetableEntryDao()
        val existingEntry = dao.getGeneratedForCourse(ownerUserId, localCourseId)

        if (classDayOfWeek == null || classStartMinuteOfDay == null) {
            existingEntry?.let { entry ->
                entry.remoteId?.takeIf { it.isNotBlank() }?.let { remoteId ->
                    val activeSession = session
                        ?: throw SyncFailureException("Sign in again before updating synced timetable reminders.")
                    deleteRemoteRow(
                        table = "timetable_entries",
                        filters = listOf(
                            "id=eq.${encodeValue(remoteId)}",
                            "owner_user_id=eq.${encodeValue(ownerUserId)}",
                        ),
                        accessToken = activeSession.accessToken,
                    )
                }
                dao.deleteById(ownerUserId, entry.id)
            }
            return
        }

        val resolvedRemoteId = if (remoteCourseId.isNullOrBlank() || session == null) {
            existingEntry?.remoteId
        } else {
            val syncedRemoteCourseId = remoteCourseId
            val activeSession = session
            if (existingEntry?.remoteId?.isNotBlank() == true) {
                patchRemoteRow(
                    table = "timetable_entries",
                    filters = listOf(
                        "id=eq.${encodeValue(existingEntry.remoteId)}",
                        "owner_user_id=eq.${encodeValue(ownerUserId)}",
                    ),
                    accessToken = activeSession.accessToken,
                    body = buildTimetableEntryPayload(
                        ownerUserId = ownerUserId,
                        remoteCourseId = syncedRemoteCourseId,
                        dayOfWeek = classDayOfWeek,
                        startMinuteOfDay = classStartMinuteOfDay,
                        endMinuteOfDay = null,
                        classType = null,
                        section = null,
                        venue = null,
                        lecturer = null,
                        weekPattern = null,
                        source = TimetableEntryEntity.SOURCE_COURSE_SCHEDULE,
                    ),
                )
                existingEntry.remoteId
            } else {
                runCatching {
                    insertRemoteTimetableEntry(
                        session = activeSession,
                        ownerUserId = ownerUserId,
                        remoteCourseId = syncedRemoteCourseId,
                        dayOfWeek = classDayOfWeek,
                        startMinuteOfDay = classStartMinuteOfDay,
                        endMinuteOfDay = null,
                        classType = null,
                        section = null,
                        venue = null,
                        lecturer = null,
                        weekPattern = null,
                        source = TimetableEntryEntity.SOURCE_COURSE_SCHEDULE,
                    ).id
                }.getOrNull()
            }
        }

        if (existingEntry != null) {
            dao.update(
                existingEntry.copy(
                    remoteId = resolvedRemoteId ?: existingEntry.remoteId,
                    dayOfWeek = classDayOfWeek,
                    startMinuteOfDay = classStartMinuteOfDay,
                    endMinuteOfDay = null,
                    classType = null,
                    section = null,
                    venue = null,
                    lecturer = null,
                    weekPattern = null,
                    source = TimetableEntryEntity.SOURCE_COURSE_SCHEDULE,
                ),
            )
        } else {
            dao.insert(
                TimetableEntryEntity(
                    remoteId = resolvedRemoteId,
                    ownerUserId = ownerUserId,
                    courseId = localCourseId,
                    dayOfWeek = classDayOfWeek,
                    startMinuteOfDay = classStartMinuteOfDay,
                    source = TimetableEntryEntity.SOURCE_COURSE_SCHEDULE,
                ),
            )
        }
    }

    private suspend fun replaceLocalSnapshot(
        ownerUserId: String,
        remoteCourses: List<RemoteCourse>,
        remoteTimetableEntries: List<RemoteTimetableEntry>,
        remoteAssignments: List<RemoteAssignment>,
        remoteExams: List<RemoteExam>,
    ) {
        database.withTransaction {
            database.timetableEntryDao().deleteAllByOwner(ownerUserId)
            database.assignmentDao().deleteAllByOwner(ownerUserId)
            database.examDao().deleteAllByOwner(ownerUserId)
            database.courseDao().deleteAllByOwner(ownerUserId)

            val remoteCourseToLocalId = mutableMapOf<String, Long>()
            remoteCourses.forEach { course ->
                val localId = database.courseDao().insert(
                    CourseEntity(
                        remoteId = course.id,
                        ownerUserId = ownerUserId,
                        name = course.name,
                        classDayOfWeek = course.classDayOfWeek,
                        classStartMinuteOfDay = course.classStartMinuteOfDay,
                    ),
                )
                remoteCourseToLocalId[course.id] = localId
            }

            val coursesWithRemoteEntries = mutableSetOf<String>()
            remoteTimetableEntries.forEach { entry ->
                val localCourseId = remoteCourseToLocalId[entry.courseId] ?: return@forEach
                database.timetableEntryDao().insert(
                    TimetableEntryEntity(
                        remoteId = entry.id,
                        ownerUserId = ownerUserId,
                        courseId = localCourseId,
                        dayOfWeek = entry.dayOfWeek,
                        startMinuteOfDay = entry.startMinuteOfDay,
                        endMinuteOfDay = entry.endMinuteOfDay,
                        classType = entry.classType,
                        section = entry.section,
                        venue = entry.venue,
                        lecturer = entry.lecturer,
                        weekPattern = entry.weekPattern,
                        source = entry.source,
                    ),
                )
                coursesWithRemoteEntries += entry.courseId
            }

            remoteCourses.forEach { course ->
                val localCourseId = remoteCourseToLocalId[course.id] ?: return@forEach
                if (course.id in coursesWithRemoteEntries) return@forEach
                val classDay = course.classDayOfWeek ?: return@forEach
                val classStartMinute = course.classStartMinuteOfDay ?: return@forEach
                database.timetableEntryDao().insert(
                    TimetableEntryEntity(
                        ownerUserId = ownerUserId,
                        courseId = localCourseId,
                        dayOfWeek = classDay,
                        startMinuteOfDay = classStartMinute,
                        source = TimetableEntryEntity.SOURCE_COURSE_SCHEDULE,
                    ),
                )
            }

            remoteAssignments.forEach { assignment ->
                val localCourseId = remoteCourseToLocalId[assignment.courseId] ?: return@forEach
                database.assignmentDao().insert(
                    AssignmentEntity(
                        remoteId = assignment.id,
                        ownerUserId = ownerUserId,
                        courseId = localCourseId,
                        title = assignment.title,
                        dueDate = assignment.dueDate,
                        isCompleted = assignment.isCompleted,
                    ),
                )
            }

            remoteExams.forEach { exam ->
                val localCourseId = remoteCourseToLocalId[exam.courseId] ?: return@forEach
                database.examDao().insert(
                    ExamEntity(
                        remoteId = exam.id,
                        ownerUserId = ownerUserId,
                        courseId = localCourseId,
                        title = exam.title,
                        date = exam.date,
                        type = exam.type,
                    ),
                )
            }
        }
    }

    private suspend fun fetchCourses(session: AuthSession): List<RemoteCourse> {
        val response = request(
            method = "GET",
            endpoint = "rest/v1/courses?select=id,owner_user_id,name,class_day_of_week,class_start_minute_of_day&owner_user_id=eq.${encodeValue(session.userId)}&order=name.asc",
            accessToken = session.accessToken,
        )
        return parseJsonArray(response.body).mapNotNull { parseRemoteCourse(it.jsonObject) }
    }

    private suspend fun fetchTimetableEntries(session: AuthSession): List<RemoteTimetableEntry> {
        val response = request(
            method = "GET",
            endpoint = "rest/v1/timetable_entries?select=id,owner_user_id,course_id,day_of_week,start_minute_of_day,end_minute_of_day,class_type,section,venue,lecturer,week_pattern,source&owner_user_id=eq.${encodeValue(session.userId)}&order=day_of_week.asc,start_minute_of_day.asc",
            accessToken = session.accessToken,
        )
        return parseJsonArray(response.body).mapNotNull { parseRemoteTimetableEntry(it.jsonObject) }
    }

    private suspend fun fetchAssignments(session: AuthSession): List<RemoteAssignment> {
        val response = request(
            method = "GET",
            endpoint = "rest/v1/assignments?select=id,owner_user_id,course_id,title,due_date,is_completed&owner_user_id=eq.${encodeValue(session.userId)}&order=due_date.asc",
            accessToken = session.accessToken,
        )
        return parseJsonArray(response.body).mapNotNull { parseRemoteAssignment(it.jsonObject) }
    }

    private suspend fun fetchExams(session: AuthSession): List<RemoteExam> {
        val response = request(
            method = "GET",
            endpoint = "rest/v1/exams?select=id,owner_user_id,course_id,title,date,type&owner_user_id=eq.${encodeValue(session.userId)}&order=date.asc",
            accessToken = session.accessToken,
        )
        return parseJsonArray(response.body).mapNotNull { parseRemoteExam(it.jsonObject) }
    }

    private suspend fun insertRemoteCourse(
        session: AuthSession,
        ownerUserId: String,
        name: String,
        classDayOfWeek: Int?,
        classStartMinuteOfDay: Int?,
    ): RemoteCourse {
        val response = request(
            method = "POST",
            endpoint = "rest/v1/courses?select=id,owner_user_id,name,class_day_of_week,class_start_minute_of_day",
            accessToken = session.accessToken,
            body = buildCoursePayload(
                ownerUserId = ownerUserId,
                name = name,
                classDayOfWeek = classDayOfWeek,
                classStartMinuteOfDay = classStartMinuteOfDay,
            ),
            prefer = "return=representation",
        )
        return parseSingleObject(response.body)?.let(::parseRemoteCourse)
            ?: throw SyncFailureException("Supabase returned an invalid course response.")
    }

    private fun buildCoursePayload(
        ownerUserId: String,
        name: String,
        classDayOfWeek: Int?,
        classStartMinuteOfDay: Int?,
    ) = buildJsonObject {
        put("owner_user_id", ownerUserId)
        put("name", name)
        put("class_day_of_week", classDayOfWeek?.let(::JsonPrimitive) ?: JsonNull)
        put("class_start_minute_of_day", classStartMinuteOfDay?.let(::JsonPrimitive) ?: JsonNull)
    }

    private suspend fun insertRemoteTimetableEntry(
        session: AuthSession,
        ownerUserId: String,
        remoteCourseId: String,
        dayOfWeek: Int,
        startMinuteOfDay: Int,
        endMinuteOfDay: Int?,
        classType: String?,
        section: String?,
        venue: String?,
        lecturer: String?,
        weekPattern: String?,
        source: String,
    ): RemoteTimetableEntry {
        val response = request(
            method = "POST",
            endpoint = "rest/v1/timetable_entries?select=id,owner_user_id,course_id,day_of_week,start_minute_of_day,end_minute_of_day,class_type,section,venue,lecturer,week_pattern,source",
            accessToken = session.accessToken,
            body = buildTimetableEntryPayload(
                ownerUserId = ownerUserId,
                remoteCourseId = remoteCourseId,
                dayOfWeek = dayOfWeek,
                startMinuteOfDay = startMinuteOfDay,
                endMinuteOfDay = endMinuteOfDay,
                classType = classType,
                section = section,
                venue = venue,
                lecturer = lecturer,
                weekPattern = weekPattern,
                source = source,
            ),
            prefer = "return=representation",
        )
        return parseSingleObject(response.body)?.let(::parseRemoteTimetableEntry)
            ?: throw SyncFailureException("Supabase returned an invalid timetable entry response.")
    }

    private fun buildTimetableEntryPayload(
        ownerUserId: String,
        remoteCourseId: String,
        dayOfWeek: Int,
        startMinuteOfDay: Int,
        endMinuteOfDay: Int?,
        classType: String?,
        section: String?,
        venue: String?,
        lecturer: String?,
        weekPattern: String?,
        source: String,
    ) = buildJsonObject {
        put("owner_user_id", ownerUserId)
        put("course_id", remoteCourseId)
        put("day_of_week", dayOfWeek)
        put("start_minute_of_day", startMinuteOfDay)
        put("end_minute_of_day", endMinuteOfDay?.let(::JsonPrimitive) ?: JsonNull)
        put("class_type", classType?.takeIf { it.isNotBlank() }?.let(::JsonPrimitive) ?: JsonNull)
        put("section", section?.takeIf { it.isNotBlank() }?.let(::JsonPrimitive) ?: JsonNull)
        put("venue", venue?.takeIf { it.isNotBlank() }?.let(::JsonPrimitive) ?: JsonNull)
        put("lecturer", lecturer?.takeIf { it.isNotBlank() }?.let(::JsonPrimitive) ?: JsonNull)
        put("week_pattern", weekPattern?.takeIf { it.isNotBlank() }?.let(::JsonPrimitive) ?: JsonNull)
        put("source", source)
    }

    private suspend fun insertRemoteAssignment(
        session: AuthSession,
        ownerUserId: String,
        remoteCourseId: String,
        title: String,
        dueDate: Long,
        isCompleted: Boolean,
    ): RemoteAssignment {
        val response = request(
            method = "POST",
            endpoint = "rest/v1/assignments?select=id,owner_user_id,course_id,title,due_date,is_completed",
            accessToken = session.accessToken,
            body = buildJsonObject {
                put("owner_user_id", ownerUserId)
                put("course_id", remoteCourseId)
                put("title", title)
                put("due_date", dueDate)
                put("is_completed", isCompleted)
            },
            prefer = "return=representation",
        )
        return parseSingleObject(response.body)?.let(::parseRemoteAssignment)
            ?: throw SyncFailureException("Supabase returned an invalid assignment response.")
    }

    private suspend fun insertRemoteExam(
        session: AuthSession,
        ownerUserId: String,
        remoteCourseId: String,
        title: String,
        date: Long,
        type: String,
    ): RemoteExam {
        val response = request(
            method = "POST",
            endpoint = "rest/v1/exams?select=id,owner_user_id,course_id,title,date,type",
            accessToken = session.accessToken,
            body = buildJsonObject {
                put("owner_user_id", ownerUserId)
                put("course_id", remoteCourseId)
                put("title", title)
                put("date", date)
                put("type", type)
            },
            prefer = "return=representation",
        )
        return parseSingleObject(response.body)?.let(::parseRemoteExam)
            ?: throw SyncFailureException("Supabase returned an invalid exam response.")
    }

    private suspend fun patchRemoteRow(
        table: String,
        filters: List<String>,
        accessToken: String,
        body: JsonObject,
    ) {
        request(
            method = "PATCH",
            endpoint = "rest/v1/$table?${filters.joinToString("&")}",
            accessToken = accessToken,
            body = body,
            prefer = "return=minimal",
        )
    }

    private suspend fun deleteRemoteRow(
        table: String,
        filters: List<String>,
        accessToken: String,
    ) {
        request(
            method = "DELETE",
            endpoint = "rest/v1/$table?${filters.joinToString("&")}",
            accessToken = accessToken,
            prefer = "return=minimal",
        )
    }

    private suspend fun request(
        method: String,
        endpoint: String,
        accessToken: String,
        body: JsonObject? = null,
        prefer: String? = null,
    ): HttpResponse = withContext(Dispatchers.IO) {
        val connection = (URL("${BuildConfig.SUPABASE_URL.trimEnd('/')}/$endpoint").openConnection() as HttpURLConnection)
            .apply {
                requestMethod = method
                connectTimeout = 12_000
                readTimeout = 12_000
                doInput = true
                setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("Accept", "application/json")
                prefer?.let { setRequestProperty("Prefer", it) }
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }
            }

        try {
            if (body != null) {
                connection.outputStream.bufferedWriter().use { it.write(body.toString()) }
            }

            val statusCode = connection.responseCode
            val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
            val response = HttpResponse(
                statusCode = statusCode,
                body = stream?.readUtf8().orEmpty(),
            )
            if (!response.isSuccessful) {
                throw SyncHttpException(statusCode, response.body)
            }
            response
        } finally {
            connection.disconnect()
        }
    }

    private fun InputStream.readUtf8(): String = bufferedReader().use { it.readText() }

    private fun parseJsonArray(body: String): JsonArray {
        val element = json.parseToJsonElement(body)
        return element.jsonArray
    }

    private fun parseSingleObject(body: String): JsonObject? {
        val element = json.parseToJsonElement(body)
        return when (element) {
            is JsonArray -> element.firstOrNull()?.jsonObject
            is JsonObject -> element
            else -> null
        }
    }

    private fun parseRemoteCourse(element: JsonObject): RemoteCourse? {
        return RemoteCourse(
            id = element.stringValue("id") ?: return null,
            ownerUserId = element.stringValue("owner_user_id") ?: return null,
            name = element.stringValue("name") ?: return null,
            classDayOfWeek = element.intValue("class_day_of_week"),
            classStartMinuteOfDay = element.intValue("class_start_minute_of_day"),
        )
    }

    private fun parseRemoteTimetableEntry(element: JsonObject): RemoteTimetableEntry? {
        return RemoteTimetableEntry(
            id = element.stringValue("id") ?: return null,
            ownerUserId = element.stringValue("owner_user_id") ?: return null,
            courseId = element.stringValue("course_id") ?: return null,
            dayOfWeek = element.intValue("day_of_week") ?: return null,
            startMinuteOfDay = element.intValue("start_minute_of_day") ?: return null,
            endMinuteOfDay = element.intValue("end_minute_of_day"),
            classType = element.stringValue("class_type"),
            section = element.stringValue("section"),
            venue = element.stringValue("venue"),
            lecturer = element.stringValue("lecturer"),
            weekPattern = element.stringValue("week_pattern"),
            source = element.stringValue("source") ?: TimetableEntryEntity.SOURCE_MANUAL,
        )
    }

    private fun parseRemoteAssignment(element: JsonObject): RemoteAssignment? {
        return RemoteAssignment(
            id = element.stringValue("id") ?: return null,
            ownerUserId = element.stringValue("owner_user_id") ?: return null,
            courseId = element.stringValue("course_id") ?: return null,
            title = element.stringValue("title") ?: return null,
            dueDate = element.longValue("due_date") ?: return null,
            isCompleted = element.booleanValue("is_completed") ?: false,
        )
    }

    private fun parseRemoteExam(element: JsonObject): RemoteExam? {
        return RemoteExam(
            id = element.stringValue("id") ?: return null,
            ownerUserId = element.stringValue("owner_user_id") ?: return null,
            courseId = element.stringValue("course_id") ?: return null,
            title = element.stringValue("title") ?: return null,
            date = element.longValue("date") ?: return null,
            type = element.stringValue("type") ?: return null,
        )
    }

    private fun JsonObject.stringValue(key: String): String? {
        return (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
    }

    private fun JsonObject.longValue(key: String): Long? {
        return (this[key] as? JsonPrimitive)?.longOrNull
    }

    private fun JsonObject.intValue(key: String): Int? {
        return (this[key] as? JsonPrimitive)?.intOrNull
    }

    private fun JsonObject.booleanValue(key: String): Boolean? {
        return (this[key] as? JsonPrimitive)?.booleanOrNull
    }

    private fun Throwable.toReadableMessage(): String {
        return when (this) {
            is SyncFailureException -> message ?: "Cloud sync failed."
            is SyncHttpException -> when (statusCode) {
                401, 403 -> "Your Supabase session expired. Sign out and sign back in."
                404 -> "Supabase data tables are missing. Run supabase/schema.sql in the Supabase SQL Editor, then retry sync."
                else -> "Supabase sync failed. ${extractApiMessage(body) ?: "Check your internet connection and schema setup."}"
            }
            else -> message ?: "Cloud sync failed."
        }
    }

    private fun extractApiMessage(body: String): String? {
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
        return root?.stringValue("message")
            ?: root?.stringValue("hint")
            ?: root?.stringValue("details")
    }

    private fun encodeValue(value: String): String = URLEncoder.encode(value, "UTF-8")

    private data class HttpResponse(
        val statusCode: Int,
        val body: String,
    ) {
        val isSuccessful: Boolean
            get() = statusCode in 200..299
    }

    private data class RemoteCourse(
        val id: String,
        val ownerUserId: String,
        val name: String,
        val classDayOfWeek: Int?,
        val classStartMinuteOfDay: Int?,
    )

    private data class RemoteTimetableEntry(
        val id: String,
        val ownerUserId: String,
        val courseId: String,
        val dayOfWeek: Int,
        val startMinuteOfDay: Int,
        val endMinuteOfDay: Int?,
        val classType: String?,
        val section: String?,
        val venue: String?,
        val lecturer: String?,
        val weekPattern: String?,
        val source: String,
    )

    private data class RemoteAssignment(
        val id: String,
        val ownerUserId: String,
        val courseId: String,
        val title: String,
        val dueDate: Long,
        val isCompleted: Boolean,
    )

    private data class RemoteExam(
        val id: String,
        val ownerUserId: String,
        val courseId: String,
        val title: String,
        val date: Long,
        val type: String,
    )

    private class SyncFailureException(message: String) : IllegalStateException(message)

    private class SyncHttpException(
        val statusCode: Int,
        val body: String,
    ) : IllegalStateException("Supabase request failed with HTTP $statusCode")
}
