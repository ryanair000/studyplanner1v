package com.example.studentcopilot.ui.exams

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.studentcopilot.data.local.entity.CourseEntity
import com.example.studentcopilot.util.formatLocalDateMillis
import com.example.studentcopilot.util.localDateMillisToDatePickerSelectionMillis
import com.example.studentcopilot.util.selectedDateMillisToLocalStartOfDay

private val examTypes = listOf("Midterm", "Final", "Quiz", "Other")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExamDialog(
    courses: List<CourseEntity>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, courseId: Long, date: Long, type: String) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var selectedCourseId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedType by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableLongStateOf(0L) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var courseMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var typeMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val selectedCourse = courses.firstOrNull { it.id == selectedCourseId }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date
                .takeIf { it > 0L }
                ?.let(::localDateMillisToDatePickerSelectionMillis),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        date = selectedDateMillisToLocalStartOfDay(it)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Exam") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("examTitleInput"),
                )

                Spacer(Modifier.height(8.dp))

                // Course picker
                ExposedDropdownMenuBox(
                    expanded = courseMenuExpanded,
                    onExpandedChange = { courseMenuExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedCourse?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Course") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("examCourseInput"),
                    )
                    ExposedDropdownMenu(
                        expanded = courseMenuExpanded,
                        onDismissRequest = { courseMenuExpanded = false },
                    ) {
                        courses.forEach { course ->
                            DropdownMenuItem(
                                text = { Text(course.name) },
                                onClick = {
                                    selectedCourseId = course.id
                                    courseMenuExpanded = false
                                },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Exam type picker
                ExposedDropdownMenuBox(
                    expanded = typeMenuExpanded,
                    onExpandedChange = { typeMenuExpanded = it },
                ) {
                    OutlinedTextField(
                        value = selectedType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("examTypeInput"),
                    )
                    ExposedDropdownMenu(
                        expanded = typeMenuExpanded,
                        onDismissRequest = { typeMenuExpanded = false },
                    ) {
                        examTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    selectedType = type
                                    typeMenuExpanded = false
                                },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Date picker
                OutlinedTextField(
                    value = if (date > 0) formatLocalDateMillis(date) else "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Exam date") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                        .testTag("examDateInput"),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedCourseId?.let { courseId ->
                        onConfirm(title, courseId, date, selectedType)
                    }
                    onDismiss()
                },
                enabled = title.isNotBlank() && selectedCourseId != null && date > 0 && selectedType.isNotBlank(),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
