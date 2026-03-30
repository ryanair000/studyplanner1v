package com.example.studentcopilot.ui.courses

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
import androidx.compose.ui.unit.dp
import com.example.studentcopilot.data.local.entity.CourseEntity
import com.example.studentcopilot.viewmodel.CourseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoursesScreen(
    viewModel: CourseViewModel,
) {
    val courses by viewModel.courses.collectAsState()
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var pendingDeleteCourseId by rememberSaveable { mutableStateOf<Long?>(null) }
    val pendingDeleteCourse = courses.firstOrNull { it.id == pendingDeleteCourseId }

    if (showDialog) {
        AddCourseDialog(
            onDismiss = { showDialog = false },
            onConfirm = { name -> viewModel.addCourse(name) },
        )
    }

    if (pendingDeleteCourse != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteCourseId = null },
            title = { Text("Delete course?") },
            text = {
                Text(
                    "Deleting ${pendingDeleteCourse.name} will also remove its assignments and exams.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCourse(pendingDeleteCourse.id)
                        pendingDeleteCourseId = null
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteCourseId = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            TopAppBar(title = { Text("Courses") })

            if (courses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No courses yet.\nTap + to add your first course.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(courses, key = { it.id }) { course ->
                        CourseItem(
                            course = course,
                            onDelete = { pendingDeleteCourseId = course.id },
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add course")
        }
    }
}

@Composable
private fun CourseItem(
    course: CourseEntity,
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
            Text(
                text = course.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete course",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
