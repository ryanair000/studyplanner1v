package com.example.studentcopilot.ui.navigation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.studentcopilot.MainActivity
import com.example.studentcopilot.ui.auth.AuthScreen
import com.example.studentcopilot.ui.assignments.AssignmentsScreen
import com.example.studentcopilot.ui.courses.CoursesScreen
import com.example.studentcopilot.ui.dashboard.DashboardScreen
import com.example.studentcopilot.ui.exams.ExamsScreen
import com.example.studentcopilot.viewmodel.AssignmentViewModel
import com.example.studentcopilot.viewmodel.AppUpdateViewModel
import com.example.studentcopilot.viewmodel.AuthViewModel
import com.example.studentcopilot.viewmodel.CourseViewModel
import com.example.studentcopilot.viewmodel.DashboardViewModel
import com.example.studentcopilot.viewmodel.ExamViewModel

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val activity = context as? MainActivity
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.state.collectAsState()
    val authCallbackUri by activity?.authCallbackUris?.collectAsState(initial = null)
        ?: remember { mutableStateOf(null) }
    val appUpdateViewModel: AppUpdateViewModel = viewModel()
    val updateState by appUpdateViewModel.state.collectAsState()
    val availableUpdate = updateState.availableUpdate
    val notificationsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { }
    val hasAskedForNotifications = remember { mutableStateOf(false) }

    LaunchedEffect(authState.canEnterApp) {
        if (
            authState.canEnterApp &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasAskedForNotifications.value &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            hasAskedForNotifications.value = true
            notificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(authCallbackUri) {
        val callbackUri = authCallbackUri ?: return@LaunchedEffect
        authViewModel.completeGoogleSignIn(callbackUri)
        activity?.clearAuthCallback()
    }

    if (authState.isLoading || (authState.canEnterApp && !authState.isGuestMode && authState.isSyncing)) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text("Loading Pangia...")
        }
        return
    }

    if (!authState.canEnterApp) {
        AuthScreen(
            state = authState,
            onSignIn = authViewModel::signIn,
            onSignUp = authViewModel::signUp,
            onSignInWithGoogle = {
                authViewModel.beginGoogleSignIn()?.let { googleSignInUrl ->
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(googleSignInUrl)))
                }
            },
            onContinueAsGuest = authViewModel::continueAsGuest,
            onClearMessages = authViewModel::clearMessages,
        )
        return
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Hide bottom bar on detail / form screens
    val showBottomBar = bottomNavItems.any { it.route == currentDestination?.route }

    LaunchedEffect(Unit) {
        appUpdateViewModel.checkForUpdates()
    }

    if (availableUpdate != null) {
        AlertDialog(
            onDismissRequest = { appUpdateViewModel.dismissUpdate() },
            title = { Text("Update available") },
            text = {
                Text(
                    buildString {
                        append("Pangia ")
                        append(availableUpdate.versionName)
                        append(" is available. Download and install the latest version in your browser.")
                        availableUpdate.releaseNotes?.let {
                            append("\n\nWhat's new:\n")
                            append(it)
                        }
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        appUpdateViewModel.dismissUpdate()
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(availableUpdate.downloadUrl)),
                        )
                    },
                ) {
                    Text("Update now")
                }
            },
            dismissButton = {
                TextButton(onClick = { appUpdateViewModel.dismissUpdate() }) {
                    Text("Later")
                }
            },
        )
    }

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
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    currentUserEmail = authState.currentUserEmail,
                    isGuestMode = authState.isGuestMode,
                    syncErrorMessage = authState.syncErrorMessage,
                    onRetrySync = authViewModel::retrySync,
                    isSyncing = authState.isSyncing,
                    onSignOut = authViewModel::signOut,
                    isSigningOut = authState.isSigningOut,
                )
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
                ExamsScreen(
                    viewModel = examViewModel,
                    onGoToCourses = {
                        navController.navigate(Screen.Courses.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        }
    }
}
