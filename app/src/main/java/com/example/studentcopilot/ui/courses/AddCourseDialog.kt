package com.example.studentcopilot.ui.courses

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.studentcopilot.data.local.entity.CourseEntity
import com.example.studentcopilot.util.dayOfWeekLabel
import com.example.studentcopilot.util.minuteOfDayLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCourseDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, classDayOfWeek: Int?, classStartMinuteOfDay: Int?) -> Unit,
    initialCourse: CourseEntity? = null,
) {
    var name by rememberSaveable { mutableStateOf(initialCourse?.name.orEmpty()) }
    var remindersEnabled by rememberSaveable {
        mutableStateOf(initialCourse?.classDayOfWeek != null && initialCourse.classStartMinuteOfDay != null)
    }
    var classDayOfWeek by rememberSaveable { mutableStateOf(initialCourse?.classDayOfWeek) }
    var classStartMinuteOfDay by rememberSaveable { mutableStateOf(initialCourse?.classStartMinuteOfDay) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var dayMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var pickerHour by rememberSaveable { mutableIntStateOf(initialCourse?.classStartMinuteOfDay?.div(60) ?: 9) }
    var pickerMinute by rememberSaveable { mutableIntStateOf(initialCourse?.classStartMinuteOfDay?.rem(60) ?: 0) }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = pickerHour,
            initialMinute = pickerMinute,
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Class start time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerHour = timePickerState.hour
                        pickerMinute = timePickerState.minute
                        classStartMinuteOfDay = (timePickerState.hour * 60) + timePickerState.minute
                        showTimePicker = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialCourse == null) "Add Course" else "Edit Course") },
        text = {
            Column {
                Text(
                    if (initialCourse == null) {
                        "Enter the course name and optionally add a weekly class reminder."
                    } else {
                        "Update the course details and weekly class reminder."
                    },
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Course name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("courseNameInput"),
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Checkbox(
                        checked = remindersEnabled,
                        onCheckedChange = { enabled ->
                            remindersEnabled = enabled
                            if (!enabled) {
                                classDayOfWeek = null
                                classStartMinuteOfDay = null
                            }
                        },
                    )
                    Text("Weekly class reminder")
                }

                if (remindersEnabled) {
                    Spacer(Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = dayMenuExpanded,
                        onExpandedChange = { dayMenuExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = classDayOfWeek?.let(::dayOfWeekLabel).orEmpty(),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Class day") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayMenuExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                        )
                        ExposedDropdownMenu(
                            expanded = dayMenuExpanded,
                            onDismissRequest = { dayMenuExpanded = false },
                        ) {
                            (1..7).forEach { day ->
                                DropdownMenuItem(
                                    text = { Text(dayOfWeekLabel(day)) },
                                    onClick = {
                                        classDayOfWeek = day
                                        dayMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = classStartMinuteOfDay?.let(::minuteOfDayLabel).orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Class start time") },
                        supportingText = { Text("Pangia will remind you 30 minutes before class.") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTimePicker = true },
                    )
                }
            }
        },
        confirmButton = {
            val hasValidReminder = !remindersEnabled || (classDayOfWeek != null && classStartMinuteOfDay != null)
            TextButton(
                onClick = {
                    onConfirm(
                        name,
                        if (remindersEnabled) classDayOfWeek else null,
                        if (remindersEnabled) classStartMinuteOfDay else null,
                    )
                    onDismiss()
                },
                enabled = name.isNotBlank() && hasValidReminder,
            ) {
                Text(if (initialCourse == null) "Save" else "Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
