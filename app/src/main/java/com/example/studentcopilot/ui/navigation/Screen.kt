package com.example.studentcopilot.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * All navigation destinations in the app.
 * Sealed class makes it exhaustive — the compiler will warn if you miss one.
 */
sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    data object Dashboard : Screen("dashboard", "Home", Icons.Default.Dashboard)
    data object Assignments : Screen("assignments", "Tasks", Icons.Default.EditNote)
    data object Calendar : Screen("calendar", "Calendar", Icons.Default.DateRange)
    data object Courses : Screen("courses", "Courses", Icons.Default.Book)
    data object Exams : Screen("exams", "Exams", Icons.Default.Quiz)

}

/** Only these appear in the bottom navigation bar */
val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Assignments,
    Screen.Calendar,
    Screen.Courses,
    Screen.Exams,
)
