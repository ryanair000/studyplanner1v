package com.example.studentcopilot.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.studentcopilot.ui.assignments.AssignmentsScreen
import com.example.studentcopilot.ui.courses.CoursesScreen
import com.example.studentcopilot.ui.dashboard.DashboardScreen
import com.example.studentcopilot.ui.exams.ExamsScreen
import com.example.studentcopilot.viewmodel.AssignmentViewModel
import com.example.studentcopilot.viewmodel.CourseViewModel
import com.example.studentcopilot.viewmodel.DashboardViewModel
import com.example.studentcopilot.viewmodel.ExamViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Hide bottom bar on detail / form screens
    val showBottomBar = bottomNavItems.any { it.route == currentDestination?.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == screen.route
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    // Pop up to start so back doesn't cycle through tabs
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            // ── Bottom nav tabs ──
            composable(Screen.Dashboard.route) {
                val dashboardViewModel: DashboardViewModel = viewModel()
                DashboardScreen(viewModel = dashboardViewModel)
            }
            composable(Screen.Courses.route) {
                val courseViewModel: CourseViewModel = viewModel()
                CoursesScreen(viewModel = courseViewModel)
            }
            composable(Screen.Assignments.route) {
                val assignmentViewModel: AssignmentViewModel = viewModel()
                AssignmentsScreen(viewModel = assignmentViewModel)
            }
            composable(Screen.Exams.route) {
                val examViewModel: ExamViewModel = viewModel()
                ExamsScreen(viewModel = examViewModel)
            }
        }
    }
}
