package com.example.studentcopilot.ui.exams

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.studentcopilot.data.local.entity.ExamEntity
import com.example.studentcopilot.util.formatLocalDateMillis
import com.example.studentcopilot.viewmodel.ExamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamsScreen(
    viewModel: ExamViewModel,
    onGoToCourses: () -> Unit = {},
) {
    val exams by viewModel.exams.collectAsState()
    val courses by viewModel.courses.collectAsState()
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var pendingDeleteExamId by rememberSaveable { mutableStateOf<Long?>(null) }
    val pendingDeleteExam = exams.firstOrNull { it.id == pendingDeleteExamId }

    if (showDialog && courses.isNotEmpty()) {
        AddExamDialog(
            courses = courses,
            onDismiss = { showDialog = false },
            onConfirm = { title, courseId, date, type ->
                viewModel.addExam(title, courseId, date, type)
            },
        )
    }

    if (pendingDeleteExam != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteExamId = null },
            title = { Text("Delete exam?") },
            text = {
                Text(
                    "Remove ${pendingDeleteExam.title} from your exam schedule?",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteExam(pendingDeleteExam.id)
                        pendingDeleteExamId = null
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteExamId = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            TopAppBar(title = { Text("Exams") })

            if (courses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Create a course before scheduling exams.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(onClick = onGoToCourses) {
                            Text("Go To Courses")
                        }
                    }
                }
            } else if (exams.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No exams yet.\nTap + to add one.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(exams, key = { it.id }) { exam ->
                        ExamItem(
                            exam = exam,
                            courseName = courses.firstOrNull { it.id == exam.courseId }?.name ?: "Unknown",
                            onDelete = { pendingDeleteExamId = exam.id },
                        )
                    }
                }
            }
        }

        if (courses.isNotEmpty()) {
            FloatingActionButton(
                onClick = { showDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add exam")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExamItem(
    exam: ExamEntity,
    courseName: String,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = exam.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "$courseName  •  ${formatLocalDateMillis(exam.date)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AssistChip(
                    onClick = {},
                    label = { Text(exam.type) },
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete exam",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
