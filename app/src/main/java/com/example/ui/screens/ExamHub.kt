package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.domain.ExamBucket
import com.example.domain.ExamTimelineLogic
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Ticks every [intervalMs] so countdowns and "Today / This week" groups stay correct while the screen is open. */
@Composable
fun rememberNowMillis(intervalMs: Long = 30_000L): State<Long> =
    produceState(initialValue = System.currentTimeMillis()) {
        while (true) { delay(intervalMs); value = System.currentTimeMillis() }
    }

private fun fmtDate(millis: Long) = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date(millis))
private fun fmtTime(millis: Long) = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))

// ------------------------------------------------------------------------------------------------ hero

/** Big "next exam" card with a live countdown. */
@Composable
internal fun ExamHeroCard(exams: List<LocalExam>, nowMillis: Long, modifier: Modifier = Modifier) {
    val upcoming = exams.filter { it.dateMillis >= nowMillis }.sortedBy { it.dateMillis }
    val next = upcoming.firstOrNull()
    val shape = RoundedCornerShape(28.dp)
    val brush = Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))
    val onColor = MaterialTheme.colorScheme.onPrimary
    Box(modifier = modifier.fillMaxWidth().clip(shape).background(brush).padding(20.dp).animateContentSize()) {
        if (next == null) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("NEXT EXAM", style = MaterialTheme.typography.labelMedium, color = onColor.copy(alpha = 0.8f))
                Text("Nothing scheduled", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = onColor)
                Text("Add an exam below and the countdown starts here.", style = MaterialTheme.typography.bodyMedium, color = onColor.copy(alpha = 0.9f))
            }
        } else {
            val cd = ExamTimelineLogic.countdown(next.dateMillis, nowMillis)
            val text = ExamTimelineLogic.countdownText(cd)
            val days = ExamTimelineLogic.calendarDaysUntil(next.dateMillis, nowMillis, TimeZone.getDefault())
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "Next exam ${next.title}, ${ExamTimelineLogic.relativeLabel(days)}, countdown $text" }
            ) {
                Text("NEXT EXAM  \u00B7  ${ExamTimelineLogic.relativeLabel(days).uppercase(Locale.getDefault())}", style = MaterialTheme.typography.labelMedium, color = onColor.copy(alpha = 0.85f))
                Text(text, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold, color = onColor, modifier = Modifier.testTag("exam_hero_countdown"))
                Text(next.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = onColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${next.subject}  \u00B7  ${fmtDate(next.dateMillis)}  \u00B7  ${fmtTime(next.dateMillis)}", style = MaterialTheme.typography.bodyMedium, color = onColor.copy(alpha = 0.92f))
                if (next.notes.isNotBlank()) Text(next.notes, style = MaterialTheme.typography.bodySmall, color = onColor.copy(alpha = 0.85f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (upcoming.size > 1) Text("${upcoming.size - 1} more upcoming", style = MaterialTheme.typography.labelMedium, color = onColor.copy(alpha = 0.85f))
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------ timeline

internal fun LazyListScope.examTimelineItems(
    exams: List<LocalExam>, nowMillis: Long,
    onEdit: (LocalExam) -> Unit, onRemove: (LocalExam) -> Unit
) {
    if (exams.isEmpty()) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("No exams yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Tap \u201CAdd exam\u201D to put your first one on the calendar.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        return
    }
    val tz = TimeZone.getDefault()
    val sorted = exams.sortedBy { it.dateMillis }
    fun inBucket(b: ExamBucket) = sorted.filter { ExamTimelineLogic.bucket(it.dateMillis, nowMillis, tz) == b }
    fun section(title: String, list: List<LocalExam>) {
        if (list.isEmpty()) return
        item { Text("$title  \u00B7  ${list.size}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp)) }
        items(list) { ExamRow(it, nowMillis, onEdit, onRemove) }
    }
    section("Today", inBucket(ExamBucket.TODAY))
    section("This week", inBucket(ExamBucket.THIS_WEEK))
    section("Later", inBucket(ExamBucket.LATER))
    val past = inBucket(ExamBucket.PAST).reversed()
    if (past.isNotEmpty()) item { PastExams(past, nowMillis, onEdit, onRemove) }
}

@Composable
private fun PastExams(past: List<LocalExam>, nowMillis: Long, onEdit: (LocalExam) -> Unit, onRemove: (LocalExam) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.animateContentSize()) {
        Row(
            Modifier.fillMaxWidth().clickable(onClickLabel = if (open) "Hide past exams" else "Show past exams") { open = !open }.padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Past  \u00B7  ${past.size}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(if (open) "Hide" else "Show", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        AnimatedVisibility(open) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { past.forEach { ExamRow(it, nowMillis, onEdit, onRemove) } }
        }
    }
}

@Composable
private fun ExamRow(exam: LocalExam, nowMillis: Long, onEdit: (LocalExam) -> Unit, onRemove: (LocalExam) -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    val tz = TimeZone.getDefault()
    val days = ExamTimelineLogic.calendarDaysUntil(exam.dateMillis, nowMillis, tz)
    val isPast = exam.dateMillis < nowMillis
    val label = if (isPast) "Passed" else ExamTimelineLogic.relativeLabel(days)
    val dayNum = SimpleDateFormat("dd", Locale.getDefault()).format(Date(exam.dateMillis))
    val month = SimpleDateFormat("MMM", Locale.getDefault()).format(Date(exam.dateMillis)).uppercase(Locale.getDefault())

    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false }, title = { Text("Remove exam?") },
        text = { Text("\u201C${exam.title}\u201D and its reminders will be removed from this phone.") },
        confirmButton = { TextButton(onClick = { confirm = false; onRemove(exam) }) { Text("Remove") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Keep") } }
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (isPast) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(
                Modifier.size(width = 56.dp, height = 60.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
            ) {
                Text(dayNum, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(month, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(exam.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${exam.subject}  \u00B7  ${fmtTime(exam.dateMillis)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(label, style = MaterialTheme.typography.labelLarge, color = if (!isPast && days <= 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                if (exam.notes.isNotBlank()) Text(exam.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Column {
                IconButton(onClick = { onEdit(exam) }) { Icon(Icons.Default.Edit, contentDescription = "Edit ${exam.title}") }
                IconButton(onClick = { confirm = true }) { Icon(Icons.Default.Delete, contentDescription = "Remove ${exam.title}") }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------ editor

/**
 * Add / edit dialog with real date and time pickers (no typing dd/MM/yyyy by hand).
 * [onSave] receives the exam instant in the phone's time zone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExamEditorDialog(
    initial: LocalExam?, subjectSuggestions: List<String>,
    onDismiss: () -> Unit, onSave: (title: String, subject: String, dateMillis: Long, notes: String) -> Unit
) {
    val tz = TimeZone.getDefault()
    val start = remember(initial) { initial?.let { ExamTimelineLogic.localYmdhm(it.dateMillis, tz) } }
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var subject by remember { mutableStateOf(initial?.subject ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var ymd by remember { mutableStateOf(start?.let { Triple(it[0], it[1], it[2]) }) }
    var hour by remember { mutableStateOf(start?.get(3) ?: 9) }
    var minute by remember { mutableStateOf(start?.get(4) ?: 0) }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val chosenMillis = ymd?.let { ExamTimelineLogic.localMillis(it.first, it.second, it.third, hour, minute, tz) }
    val inPast = chosenMillis != null && chosenMillis < System.currentTimeMillis()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add exam" else "Edit exam") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it.take(80) }, label = { Text("Exam title") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("exam_title"))
                OutlinedTextField(subject, { subject = it.take(80) }, label = { Text("Subject") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("exam_subject"))
                if (subjectSuggestions.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    subjectSuggestions.distinct().take(12).forEach { s -> AssistChip(onClick = { subject = s }, label = { Text(s, maxLines = 1) }) }
                }
                OutlinedButton(onClick = { showDate = true }, modifier = Modifier.fillMaxWidth().testTag("exam_pick_date")) {
                    Icon(Icons.Default.DateRange, contentDescription = null); Spacer(Modifier.size(8.dp))
                    Text(chosenMillis?.let { fmtDate(it) } ?: "Pick the exam date")
                }
                OutlinedButton(onClick = { showTime = true }, modifier = Modifier.fillMaxWidth().testTag("exam_pick_time")) {
                    Icon(Icons.Default.AccessTime, contentDescription = null); Spacer(Modifier.size(8.dp))
                    Text("%02d:%02d".format(hour, minute))
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(9 to 0, 10 to 0, 14 to 0).forEach { (h, m) -> AssistChip(onClick = { hour = h; minute = m }, label = { Text("%02d:%02d".format(h, m)) }) }
                }
                OutlinedTextField(notes, { notes = it.take(200) }, label = { Text("Notes / hall / seat (optional)") }, modifier = Modifier.fillMaxWidth())
                if (inPast) Text("This date has already passed, so it will be saved under Past exams and no reminder will fire.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    title.isBlank() -> error = "Give the exam a title."
                    subject.isBlank() -> error = "Enter or pick a subject."
                    chosenMillis == null -> error = "Pick the exam date."
                    else -> onSave(title.trim(), subject.trim(), chosenMillis, notes.trim())
                }
            }, modifier = Modifier.testTag("exam_save")) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = ymd?.let { ExamTimelineLogic.ymdToPickerMillis(it.first, it.second, it.third) })
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { ymd = ExamTimelineLogic.pickerMillisToYmd(it) }; showDate = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }

    if (showTime) {
        val ts = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
        Dialog(onDismissRequest = { showTime = false }) {
            Surface(shape = RoundedCornerShape(28.dp), tonalElevation = 6.dp, modifier = Modifier.widthIn(min = 328.dp)) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Exam start time", style = MaterialTheme.typography.labelLarge, modifier = Modifier.fillMaxWidth())
                    TimePicker(state = ts)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showTime = false }) { Text("Cancel") }
                        TextButton(onClick = { hour = ts.hour; minute = ts.minute; showTime = false }) { Text("OK") }
                    }
                }
            }
        }
    }
}
