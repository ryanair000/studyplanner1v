package com.example.studentcopilot

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.studentcopilot.data.local.entity.AssignmentEntity
import com.example.studentcopilot.data.local.entity.ExamEntity
import com.example.studentcopilot.util.currentLocalDayStartMillis
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MainFlowTest {

    private val ownerUserId = "test-user"

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val app: StudentCopilotApp
        get() = composeTestRule.activity.application as StudentCopilotApp

    @Before
    fun clearDatabaseAndSeedSession() {
        runBlocking(Dispatchers.IO) {
            app.database.clearAllTables()
        }
        app.getSharedPreferences("auth_session", 0)
            .edit()
            .putString("user_id", ownerUserId)
            .putString("email", "tester@pangia.app")
            .putString("access_token", "debug-token")
            .remove("refresh_token")
            .remove("expires_at")
            .apply()

        composeTestRule.activity.runOnUiThread {
            composeTestRule.activity.recreate()
        }
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText("Courses").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun addingCourseAndDeletingItRemovesRelatedWork() {
        val courseName = "Math 101"
        val assignmentTitle = "Worksheet 1"
        val examTitle = "Quiz 1"

        composeTestRule.onNodeWithText("Courses").performClick()
        composeTestRule.onNodeWithContentDescription("Add course").performClick()
        composeTestRule.onNodeWithTag("courseNameInput").performTextInput(courseName)
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText(courseName).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(courseName).assertIsDisplayed()

        val courseId = runBlocking(Dispatchers.IO) {
            app.database.courseDao().getAll(ownerUserId).first().first { it.name == courseName }.id
        }
        val dueDate = currentLocalDayStartMillis() + TimeUnit.DAYS.toMillis(3)

        runBlocking(Dispatchers.IO) {
            app.database.assignmentDao().insert(
                AssignmentEntity(
                    ownerUserId = ownerUserId,
                    courseId = courseId,
                    title = assignmentTitle,
                    dueDate = dueDate,
                ),
            )
            app.database.examDao().insert(
                ExamEntity(
                    ownerUserId = ownerUserId,
                    courseId = courseId,
                    title = examTitle,
                    date = dueDate,
                    type = "Quiz",
                ),
            )
        }

        composeTestRule.onNodeWithText("Tasks").performClick()
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText(assignmentTitle).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(assignmentTitle).assertIsDisplayed()

        composeTestRule.onNodeWithText("Exams").performClick()
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText(examTitle).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(examTitle).assertIsDisplayed()

        composeTestRule.onNodeWithText("Courses").performClick()
        composeTestRule.onNodeWithContentDescription("Delete course").performClick()
        composeTestRule.onNodeWithText("Delete course?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete").performClick()
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText("No courses yet.\nTap + to add your first course.")
                .fetchSemanticsNodes().isNotEmpty()
        }

        val (assignmentCount, examCount) = runBlocking(Dispatchers.IO) {
            val assignments = app.database.assignmentDao().getAll(ownerUserId).first()
            val exams = app.database.examDao().getAll(ownerUserId).first()
            assignments.size to exams.size
        }

        assertEquals(0, assignmentCount)
        assertEquals(0, examCount)
    }

    @Test
    fun examsTabGuidesUsersToCoursesWhenNothingExists() {
        composeTestRule.onNodeWithText("Exams").performClick()
        composeTestRule.onNodeWithText("Create a course before scheduling exams.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Go To Courses").assertIsDisplayed()

        composeTestRule.onNodeWithText("Go To Courses").performClick()
        composeTestRule.onNodeWithText("No courses yet.\nTap + to add your first course.").assertIsDisplayed()
    }
}
