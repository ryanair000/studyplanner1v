package com.example.studentcopilot.ui.calendar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.studentcopilot.ui.components.PangiaLogoLockup
import com.example.studentcopilot.util.localDateMillisToDatePickerSelectionMillis
import com.example.studentcopilot.util.selectedDateMillisToLocalStartOfDay
import com.example.studentcopilot.viewmodel.CalendarAgendaItem
import com.example.studentcopilot.viewmodel.CalendarState
import com.example.studentcopilot.viewmodel.CalendarViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val CalendarBackgroundTop = Color(0xFFF9FAFE)
private val CalendarBackgroundBottom = Color(0xFFF3F5FF)
private val CalendarPrimary = Color(0xFF4456BA)
private val CalendarSurface = Color(0xFFFFFFFF)
private val CalendarText = Color(0xFF1A1C1E)
private val CalendarMuted = Color(0xFF6A6D84)
private val CalendarDanger = Color(0xFFD62626)
private val CalendarNight = Color(0xFF101A36)
private val CalendarNightAccent = Color(0xFF3B73FF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    currentUserEmail: String?,
    isGuestMode: Boolean,
    onOpenAssignments: () -> Unit,
    onOpenCourses: () -> Unit,
    onOpenExams: () -> Unit,
    viewModel: CalendarViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val zoneId = remember { ZoneId.systemDefault() }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = localDateMillisToDatePickerSelectionMillis(
                state.selectedDate.toStartOfDayMillis(zoneId),
                zoneId,
            ),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDateMillis ->
                            val localStart = selectedDateMillisToLocalStartOfDay(selectedDateMillis, zoneId)
                            viewModel.selectDate(localStart.toLocalDate(zoneId))
                        }
                        showDatePicker = false
                    },
                ) {
                    Text("Open date")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(CalendarBackgroundTop, CalendarBackgroundBottom),
                ),
            ),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                CalendarHeader(
                    currentUserEmail = currentUserEmail,
                    isGuestMode = isGuestMode,
                    onOpenAssignments = onOpenAssignments,
                    onJumpToToday = viewModel::jumpToToday,
                )
            }
            item {
                CalendarMonthSection(
                    state = state,
                    onSelectDate = viewModel::selectDate,
                    onShiftWeek = viewModel::shiftWeek,
                    onOpenDatePicker = { showDatePicker = true },
                )
            }
            item {
                AgendaHeader(count = state.agendaItems.size)
            }
            if (state.agendaItems.isEmpty()) {
                item {
                    EmptyAgendaCard(
                        selectedDate = state.selectedDate,
                        onOpenCourses = onOpenCourses,
                    )
                }
            } else {
                items(state.agendaItems, key = { it.stableId }) { item ->
                    when (item) {
                        is CalendarAgendaItem.Class -> ClassAgendaCard(item = item, onOpenCourses = onOpenCourses)
                        is CalendarAgendaItem.Assignment -> AssignmentAgendaCard(item = item, onOpenAssignments = onOpenAssignments)
                        is CalendarAgendaItem.Exam -> ExamAgendaCard(item = item, onOpenExams = onOpenExams)
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarHeader(
    currentUserEmail: String?,
    isGuestMode: Boolean,
    onOpenAssignments: () -> Unit,
    onJumpToToday: () -> Unit,
) {
    val userLabel = if (isGuestMode) "Guest" else currentUserEmail?.substringBefore("@").orEmpty().ifBlank { "Student" }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column {
                PangiaLogoLockup(modifier = Modifier.height(34.dp))
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (isGuestMode) "Calendar preview" else "$userLabel's academic flow",
                    color = CalendarMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FloatingCircleAction(icon = Icons.Default.Search, contentDescription = "Open tasks", onClick = onOpenAssignments)
            FloatingCircleAction(icon = Icons.Default.Tune, contentDescription = "Jump to today", onClick = onJumpToToday)
        }
    }
}

@Composable
private fun FloatingCircleAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(42.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = CalendarSurface.copy(alpha = 0.78f),
            contentColor = CalendarMuted,
        ),
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}

@Composable
private fun CalendarMonthSection(
    state: CalendarState,
    onSelectDate: (LocalDate) -> Unit,
    onShiftWeek: (Long) -> Unit,
    onOpenDatePicker: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column {
                Text(
                    text = "ACADEMIC CALENDAR",
                    color = CalendarPrimary.copy(alpha = 0.55f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = state.monthLabel,
                    color = CalendarText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            TextButton(
                onClick = onOpenDatePicker,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(CalendarPrimary.copy(alpha = 0.06f)),
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = CalendarPrimary)
                Spacer(Modifier.width(6.dp))
                Text("View Month", color = CalendarPrimary, fontWeight = FontWeight.Bold)
            }
        }

        SurfaceShell {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { onShiftWeek(-1) }) { Text("Prev", color = CalendarMuted) }
                    TextButton(onClick = { onShiftWeek(1) }) { Text("Next", color = CalendarMuted) }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    state.visibleWeek.forEach { date ->
                        DayPill(
                            modifier = Modifier.weight(1f),
                            date = date,
                            isSelected = date == state.selectedDate,
                            hasActivity = state.activityCounts.getOrDefault(date, 0) > 0,
                            onClick = { onSelectDate(date) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayPill(
    modifier: Modifier,
    date: LocalDate,
    isSelected: Boolean,
    hasActivity: Boolean,
    onClick: () -> Unit,
) {
    val background by animateColorAsState(
        targetValue = if (isSelected) CalendarPrimary else Color.Transparent,
        label = "dayBackground",
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else CalendarText,
        label = "dayText",
    )
    val verticalPadding by animateDpAsState(targetValue = if (isSelected) 14.dp else 10.dp, label = "dayPadding")

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(vertical = verticalPadding, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(Locale.getDefault()),
            color = if (isSelected) Color.White.copy(alpha = 0.72f) else CalendarMuted.copy(alpha = 0.55f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = date.dayOfMonth.toString(),
            color = textColor,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isSelected -> Color.White
                        hasActivity -> CalendarPrimary.copy(alpha = 0.38f)
                        else -> Color.Transparent
                    },
                ),
        )
    }
}

@Composable
private fun AgendaHeader(count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(CalendarPrimary),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Today's Agenda",
                color = CalendarMuted,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.72f))
                .padding(horizontal = 12.dp, vertical = 5.dp),
        ) {
            Text(
                text = if (count == 1) "1 ITEM" else "$count ITEMS",
                color = CalendarMuted,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun ClassAgendaCard(
    item: CalendarAgendaItem.Class,
    onOpenCourses: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(CalendarSurface)
            .border(1.dp, CalendarPrimary.copy(alpha = 0.12f), RoundedCornerShape(26.dp))
            .padding(18.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(4.dp)
                .height(116.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(CalendarPrimary),
        )
        Column(modifier = Modifier.padding(start = 14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(item.timeLabel.uppercase(), color = CalendarPrimary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(6.dp))
                    Text(item.title, color = CalendarText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(CalendarPrimary.copy(alpha = 0.08f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(item.badge, color = CalendarPrimary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                item.venue?.let {
                    MetaPill(icon = Icons.Default.LocationOn, text = it, tint = CalendarMuted)
                }
                item.secondaryMeta?.let {
                    MetaPill(icon = Icons.Default.School, text = it, tint = CalendarMuted)
                }
            }
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(CalendarPrimary.copy(alpha = 0.08f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (item.isOngoing) item.progress.coerceAtLeast(0.08f) else 0.62f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(CalendarPrimary),
                )
            }
            Spacer(Modifier.height(14.dp))
            TextButton(onClick = onOpenCourses, modifier = Modifier.align(Alignment.End)) {
                Text("Open Courses", color = CalendarPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AssignmentAgendaCard(
    item: CalendarAgendaItem.Assignment,
    onOpenAssignments: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.White, Color(0xFFFFF5F5)),
                ),
            )
            .border(1.dp, CalendarDanger.copy(alpha = 0.12f), RoundedCornerShape(26.dp))
            .padding(18.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(item.urgencyLabel.uppercase(), color = CalendarDanger, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(6.dp))
                    Text(item.title, color = CalendarText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    item.courseName?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, color = CalendarMuted, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CalendarDanger.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.PriorityHigh, contentDescription = null, tint = CalendarDanger)
                }
            }
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(CalendarDanger)
                    .clickable(onClick = onOpenAssignments)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Open Tasks", color = Color.White, fontWeight = FontWeight.Bold)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun ExamAgendaCard(
    item: CalendarAgendaItem.Exam,
    onOpenExams: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(CalendarNight, Color(0xFF182750)),
                ),
            )
            .padding(20.dp),
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(item.timeLabel, color = Color(0xFF7CB3FF), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(6.dp))
                    Text(item.title, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    item.subtitle?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, color = Color.White.copy(alpha = 0.70f), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF3662D9).copy(alpha = 0.24f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text("EXAM", color = Color(0xFF9FC0FF), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(item.meta.orEmpty(), color = Color.White.copy(alpha = 0.80f), style = MaterialTheme.typography.bodyMedium)
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(CalendarNightAccent)
                        .clickable(onClick = onOpenExams)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Text("Open Exams", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun EmptyAgendaCard(
    selectedDate: LocalDate,
    onOpenCourses: () -> Unit,
) {
    SurfaceShell {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                color = CalendarPrimary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Light academic day",
                color = CalendarText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "No classes, assignments, or exams are scheduled yet. Add or sync your timetable to make this view shine.",
                color = CalendarMuted,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onOpenCourses) {
                Text("Open Courses", color = CalendarPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MetaPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    tint: Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, color = tint, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SurfaceShell(
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.78f))
            .border(1.dp, Color.White.copy(alpha = 0.80f), RoundedCornerShape(28.dp))
            .padding(14.dp),
        content = content,
    )
}

private fun LocalDate.toStartOfDayMillis(zoneId: ZoneId): Long {
    return atStartOfDay(zoneId).toInstant().toEpochMilli()
}

private fun Long.toLocalDate(zoneId: ZoneId): LocalDate {
    return java.time.Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()
}
