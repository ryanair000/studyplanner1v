package com.example.studentcopilot.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.studentcopilot.util.formatLocalDateMillis
import com.example.studentcopilot.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    currentUserEmail: String?,
    isGuestMode: Boolean,
    syncErrorMessage: String?,
    onRetrySync: () -> Unit,
    isSyncing: Boolean,
    onSignOut: () -> Unit,
    isSigningOut: Boolean,
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        TopAppBar(
            title = { Text("Pangia") },
            actions = {
                TextButton(
                    onClick = onSignOut,
                    enabled = !isSigningOut,
                ) {
                    Text(
                        if (isSigningOut) {
                            if (isGuestMode) "Leaving guest mode..." else "Signing out..."
                        } else if (isGuestMode) {
                            "Leave guest mode"
                        } else {
                            "Sign out"
                        },
                    )
                }
            },
        )

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (currentUserEmail != null) {
                Text(
                    text = "Signed in as $currentUserEmail",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (isGuestMode) {
                Text(
                    text = "Guest mode is on. Your data stays on this device until you sign in.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!isGuestMode && isSyncing) {
                Text(
                    text = "Syncing your cloud data...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (!isGuestMode && syncErrorMessage != null) {
                Text(
                    text = syncErrorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(
                    onClick = onRetrySync,
                    modifier = Modifier.align(Alignment.Start),
                ) {
                    Text("Retry sync")
                }
            }
            SummaryCard(
                title = "Upcoming Assignments",
                value = state.upcomingAssignments.toString(),
                subtitle = if (state.upcomingAssignments == 0) "All caught up!" else "pending",
            )
            SummaryCard(
                title = "Upcoming Exams",
                value = state.upcomingExams.toString(),
                subtitle = if (state.upcomingExams == 0) "Nothing scheduled" else "on the calendar",
            )
            SummaryCard(
                title = "Nearest Deadline",
                value = state.nearestDeadlineMillis?.let(::formatLocalDateMillis) ?: "—",
                subtitle = if (state.nearestDeadlineMillis == null) {
                    "Add tasks to see your next deadline"
                } else {
                    "coming up"
                },
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    subtitle: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
