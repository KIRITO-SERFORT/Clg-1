package com.example.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.SubjectAttendance
import com.example.data.model.Timetable
import com.example.data.model.TimetablePeriod
import com.example.ui.TimetableViewModel
import com.example.ui.theme.StatusDanger
import com.example.util.DayOrderCalculator
import java.util.Calendar

private val RedChipBackground = Color(0xFFFFE0E0)
private val RedChipText = Color(0xFF8E1B1B)
private const val CHIPS_PER_ROW = 4

/** Compact "quick view" of ONE day's timetable. Periods of subjects below the target turn red. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimetableCard(
    subjects: List<SubjectAttendance>,
    targetThreshold: Double,
    modifier: Modifier = Modifier,
    viewModel: TimetableViewModel = viewModel()
) {
    val timetable by viewModel.timetable.collectAsStateWithLifecycle()
    val anchor by viewModel.anchor.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.ensureFresh() }

    var showFixDialog by remember { mutableStateOf(false) }
    var pickedDay by remember { mutableStateOf<Int?>(null) }
    var menuOpen by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            val tt = timetable
            val noOfDays = tt?.noOfDays ?: 5
            val todayOrder = if (tt != null) {
                DayOrderCalculator.deviceWeekdayDayOrder(tt.noOfDays)
            } else {
                null
            }
            val viewingDay = pickedDay ?: todayOrder
            val viewingToday = viewingDay != null && viewingDay == todayOrder

            // ---------------- Header: title + day dropdown + refresh ----------------
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Timetable",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )

                if (tt != null && viewingDay != null) {
                    Box {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { menuOpen = true },
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (viewingToday) "Today · Day $viewingDay" else "Day $viewingDay",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Choose day",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            for (day in 1..noOfDays) {
                                DropdownMenuItem(
                                    text = {
                                        Text(if (day == todayOrder) "Day $day (today)" else "Day $day")
                                    },
                                    onClick = {
                                        pickedDay = if (day == todayOrder) null else day
                                        menuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (isRefreshing) {
                    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    }
                } else {
                    IconButton(onClick = { viewModel.refresh() }, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Update timetable",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when {
                tt == null -> {
                    Text(
                        text = if (isRefreshing) "Loading your timetable…" else "Timetable not loaded yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!isRefreshing) {
                        TextButton(onClick = { viewModel.refresh() }) { Text("Load timetable") }
                    }
                }

                anchor == null -> {
                    Text(
                        text = "Which day order is it today? Your college uses Day 1 to Day $noOfDays (Mon–Fri). You only need to set this once.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (day in 1..noOfDays) {
                            FilterChip(
                                selected = false,
                                onClick = { viewModel.setTodayDayOrder(day) },
                                label = { Text("Day $day") }
                            )
                        }
                    }
                }

                viewingDay != null -> {
                    DayChips(
                        timetable = tt,
                        day = viewingDay,
                        highlightNow = viewingToday && !DayOrderCalculator.isWeekendToday(),
                        subjects = subjects,
                        targetThreshold = targetThreshold
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .padding(0.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(10.dp),
                                shape = RoundedCornerShape(3.dp),
                                color = StatusDanger
                            ) {}
                        }
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = "Below ${(targetThreshold * 100).toInt()}% attendance",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { showFixDialog = true },
                            modifier = Modifier.heightIn(min = 32.dp)
                        ) {
                            Text("Fix day order", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    if (DayOrderCalculator.isWeekendToday() && viewingToday) {
                        Text(
                            text = "Weekend: showing the next working day.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Updated ${
                            DateUtils.getRelativeTimeSpanString(
                                tt.fetchedAtMillis,
                                System.currentTimeMillis(),
                                DateUtils.MINUTE_IN_MILLIS
                            )
                        } · refreshes weekly",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            error?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (showFixDialog) {
                DayOrderDialog(
                    noOfDays = noOfDays,
                    onPick = { day ->
                        viewModel.setTodayDayOrder(day)
                        pickedDay = null
                        showFixDialog = false
                    },
                    onDismiss = { showFixDialog = false }
                )
            }
        }
    }
}

@Composable
private fun DayChips(
    timetable: Timetable,
    day: Int,
    highlightNow: Boolean,
    subjects: List<SubjectAttendance>,
    targetThreshold: Double
) {
    var selectedSession by remember(day) { mutableStateOf<Int?>(null) }

    val lastSession = timetable.maxSessionFor(day)
    if (lastSession == 0) {
        Text(
            text = "No classes on Day $day.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val byPeriod = timetable.periodsFor(day).associateBy { it.session }
    val nowMinutes = Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }

    val rows = (1..lastSession).toList().chunked(CHIPS_PER_ROW)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { rowSessions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowSessions.forEach { session ->
                    val period = byPeriod[session]
                    val range = timetable.sessionMinutes(session)
                    val isNow = highlightNow && range != null && nowMinutes >= range.first && nowMinutes < range.second
                    PeriodChip(
                        startTime = timetable.startLabel(session),
                        session = session,
                        period = period,
                        subjects = subjects,
                        targetThreshold = targetThreshold,
                        isNow = isNow,
                        onClick = { if (period != null) selectedSession = session },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(CHIPS_PER_ROW - rowSessions.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }

    val selected = selectedSession?.let { byPeriod[it] }
    if (selected != null) {
        PeriodDetailDialog(
            period = selected,
            timeText = timetable.sessionTimes[selected.session].orEmpty(),
            matched = findSubject(selected, subjects),
            targetThreshold = targetThreshold,
            onDismiss = { selectedSession = null }
        )
    }
}

private fun normalize(text: String): String =
    text.filter { it.isLetterOrDigit() }.uppercase()

private fun findSubject(period: TimetablePeriod, subjects: List<SubjectAttendance>): List<SubjectAttendance> {
    val found = mutableListOf<SubjectAttendance>()
    period.codes.forEachIndexed { index, code ->
        val c = normalize(code)
        val t = normalize(period.titles.getOrElse(index) { "" })
        val match = subjects.firstOrNull { normalize(it.code) == c && c.isNotEmpty() }
            ?: subjects.firstOrNull { t.isNotEmpty() && normalize(it.title) == t }
        if (match != null) found.add(match)
    }
    return found
}

private fun shortCode(code: String): String =
    code.replace(Regex("^\\d{2}(?=[A-Za-z])"), "")

/**
 * Short, readable subject name for a small chip:
 * "PHYSICS FOR MECHANICAL ENGINEERING-I" -> "Physics",
 * "APPLIED CHEMISTRY FOR MECHANICAL ENGINEERING - I" -> "Applied Chemistry",
 * "PROFESSIONAL ENGLISH -I" -> "Professional English -I".
 */
private fun displayName(title: String): String {
    val base = title.substringBefore(" FOR ").trim().ifBlank { title.trim() }
    val small = setOf("and", "of", "in", "to", "the")
    val romans = setOf("i", "ii", "iii", "iv", "v", "vi")
    return base.split(" ")
        .filter { it.isNotBlank() }
        .mapIndexed { index, word ->
            val lower = word.lowercase()
            val bare = lower.trim('-', '\u2013', '.')
            when {
                bare in romans -> word.uppercase()
                index > 0 && lower in small -> lower
                else -> lower.replaceFirstChar { it.uppercase() }
            }
        }
        .joinToString(" ")
}

@Composable
private fun PeriodChip(
    session: Int,
    startTime: String,
    period: TimetablePeriod?,
    subjects: List<SubjectAttendance>,
    targetThreshold: Double,
    isNow: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val matched = if (period != null) findSubject(period, subjects) else emptyList()
    val isRed = matched.any { it.isBelowExamThreshold(targetThreshold) }

    val background = when {
        isRed -> RedChipBackground
        period == null -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isRed) RedChipText else MaterialTheme.colorScheme.onSurface
    val subColor = if (isRed) RedChipText.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant

    val border = when {
        isNow -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        isRed -> BorderStroke(1.dp, StatusDanger)
        else -> null
    }

    val name = when {
        period == null -> "Free"
        period.titles.any { it.isNotBlank() } ->
            period.titles.filter { it.isNotBlank() }.joinToString(" / ") { displayName(it) }
        else -> period.codes.joinToString(" / ") { shortCode(it) }
    }
    val codeLine = if (period == null) {
        ""
    } else {
        val codes = period.codes.joinToString("/") { shortCode(it) }
        if (matched.isNotEmpty()) "$codes · ${matched.first().percentage.toInt()}%" else codes
    }

    Surface(
        modifier = modifier
            .heightIn(min = 78.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = period != null, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = background,
        border = border
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (startTime.isNotEmpty()) "S$session · $startTime" else "S$session",
                fontSize = 9.sp,
                color = subColor,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = name,
                fontSize = 11.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            if (codeLine.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = codeLine,
                    fontSize = 9.sp,
                    color = subColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** Tap a period to see its full name, teacher, time and your attendance in that subject. */
@Composable
private fun PeriodDetailDialog(
    period: TimetablePeriod,
    timeText: String,
    matched: List<SubjectAttendance>,
    targetThreshold: Double,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = period.titles.filter { it.isNotBlank() }.joinToString(" / ")
                    .ifBlank { period.codes.joinToString(" / ") },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Course code: ${period.codes.joinToString(", ")}", style = MaterialTheme.typography.bodyMedium)
                if (timeText.isNotEmpty()) {
                    Text("Session ${period.session} · ${timeText.replace("-", " – ")}", style = MaterialTheme.typography.bodyMedium)
                }
                if (period.faculty.isNotEmpty()) {
                    Text("Faculty: ${period.faculty.distinct().joinToString(", ")}", style = MaterialTheme.typography.bodyMedium)
                }
                matched.forEach { subject ->
                    val below = subject.isBelowExamThreshold(targetThreshold)
                    Text(
                        text = "Attendance: ${subject.presentHours}/${subject.totalHours} hours (${subject.percentage.toInt()}%)" +
                            if (below) " · below ${(targetThreshold * 100).toInt()}%" else "",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (below) StatusDanger else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayOrderDialog(
    noOfDays: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Today is…") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Pick the day order your college is running today. Use this after a holiday or a shifted day order.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (day in 1..noOfDays) {
                        FilterChip(
                            selected = false,
                            onClick = { onPick(day) },
                            label = { Text("Day $day") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
