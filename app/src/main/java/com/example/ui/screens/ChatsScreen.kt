package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ChatTask
import com.example.data.model.TaskPriority
import com.example.data.repository.ChatsRepository
import com.example.ui.kit.ClgColors
import com.example.ui.kit.GlassCard
import com.example.worker.scheduleAutoDeadlineWarning
import com.example.worker.scheduleCustomReminder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun ChatsScreen(
    studentName: String,
    collegeName: String,
    batch: String
) {
    val context = LocalContext.current
    val scope = remember {
        kotlinx.coroutines.CoroutineScope(
            kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main.immediate
        )
    }
    val repository = remember { ChatsRepository() }

    val tasks by repository.observeChats().collectAsStateWithLifecycle(initialValue = emptyList())
    val currentUserId = repository.currentUserId().orEmpty()
    var userRole by remember { mutableStateOf("Student") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showRoleDialog by remember { mutableStateOf(false) }
    var reminderTargetTask by remember { mutableStateOf<ChatTask?>(null) }

    // Live ticker for HIGH priority countdown timers
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    LaunchedEffect(currentUserId) {
        userRole = repository.getCurrentUserRole()
    }

    // Automatically schedule 1-day before deadline local warnings for incomplete tasks
    LaunchedEffect(tasks, currentUserId) {
        tasks.forEach { task ->
            val completed = currentUserId.isNotBlank() && task.isCompletedBy(currentUserId)
            if (!completed && task.deadlineMillis > System.currentTimeMillis()) {
                scheduleAutoDeadlineWarning(
                    context = context,
                    taskId = task.id,
                    taskTitle = task.title,
                    deadlineMillis = task.deadlineMillis,
                    priority = task.priority.label
                )
            }
        }
    }

    val isOfficialRep = userRole == "Class Rep" || userRole == "Club Leader"

    if (showRoleDialog) {
        AlertDialog(
            onDismissRequest = { showRoleDialog = false },
            title = { Text("Select Your Role") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Student", "Class Rep", "Club Leader").forEach { roleOption ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    scope.launch {
                                        repository.setCurrentUserRole(roleOption).onSuccess {
                                            userRole = roleOption
                                            showRoleDialog = false
                                            Toast.makeText(context, "Role updated to $roleOption", Toast.LENGTH_SHORT).show()
                                        }.onFailure {
                                            Toast.makeText(context, it.message ?: "Failed to update role", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.RadioButton(selected = userRole == roleOption, onClick = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(roleOption, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRoleDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(ClgColors.Cyan.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = "CHATS",
                                    tint = ClgColors.Cyan
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "CHATS • Official Tasks",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "Priority announcements from Class Reps & Club Leaders",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ClgColors.TextMuted
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (isOfficialRep) ClgColors.Cyan.copy(alpha = 0.2f)
                                    else Color.White.copy(alpha = 0.08f)
                                )
                                .clickable(enabled = currentUserId.isNotBlank()) { showRoleDialog = true }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = userRole,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isOfficialRep) ClgColors.Cyan else ClgColors.TextMuted
                            )
                        }
                    }
                }
            }

            if (tasks.isEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = ClgColors.Cyan,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No official tasks yet",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (currentUserId.isBlank()) {
                                    "Sign in with Google to receive official Class Rep & Club Leader tasks."
                                } else {
                                    "Tasks and announcements from your Class Rep or Club Leaders will appear here."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = ClgColors.TextMuted
                            )
                        }
                    }
                }
            } else {
                items(tasks, key = { it.id }) { task ->
                    val isCompleted = currentUserId.isNotBlank() && task.isCompletedBy(currentUserId)
                    ChatTaskCard(
                        task = task,
                        nowMillis = nowMillis,
                        isCompleted = isCompleted,
                        onToggleComplete = { checked ->
                            scope.launch {
                                val res = repository.toggleTaskCompletion(task.id, checked)
                                res.onFailure {
                                    Toast.makeText(
                                        context,
                                        it.message ?: "Could not update completion",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        onSetReminder = { reminderTargetTask = task }
                    )
                }
            }
        }

        if (isOfficialRep) {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = ClgColors.Cyan,
                contentColor = ClgColors.Navy,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("create_chat_task_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Post Official Task")
            }
        }
    }

    if (showCreateDialog) {
        CreateChatTaskDialog(
            senderRole = userRole,
            defaultGroup = batch.ifBlank { "All_Students" },
            onDismiss = { showCreateDialog = false },
            onSubmit = { title, desc, groups, priority, deadlineMillis ->
                scope.launch {
                    val res = repository.createChatTask(
                        title = title,
                        description = desc,
                        senderRole = userRole,
                        targetGroup = groups,
                        deadlineMillis = deadlineMillis,
                        priority = priority
                    )
                    res.onSuccess {
                        showCreateDialog = false
                        Toast.makeText(context, "Official task sent to CHATS!", Toast.LENGTH_SHORT).show()
                    }.onFailure {
                        Toast.makeText(
                            context,
                            it.message ?: "Failed to post task. Check representative role.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
    }

    reminderTargetTask?.let { targetTask ->
        CustomReminderDialog(
            task = targetTask,
            onDismiss = { reminderTargetTask = null },
            onSchedule = { delayMinutes ->
                scheduleCustomReminder(
                    context = context,
                    taskId = targetTask.id,
                    taskTitle = targetTask.title,
                    delayInMinutes = delayMinutes,
                    priority = targetTask.priority.label
                )
                reminderTargetTask = null
                Toast.makeText(
                    context,
                    "Reminder scheduled in $delayMinutes min(s)!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChatTaskCard(
    task: ChatTask,
    nowMillis: Long,
    isCompleted: Boolean,
    onToggleComplete: (Boolean) -> Unit,
    onSetReminder: () -> Unit
) {
    val containerColor = when (task.priority) {
        TaskPriority.HIGH -> Color(0xFFEF4444).copy(alpha = 0.14f)
        TaskPriority.MEDIUM -> Color(0xFFF59E0B).copy(alpha = 0.12f)
        TaskPriority.LOW -> Color(0xFF38BDF8).copy(alpha = 0.10f)
    }
    val borderColor = when (task.priority) {
        TaskPriority.HIGH -> Color(0xFFEF4444).copy(alpha = 0.65f)
        TaskPriority.MEDIUM -> Color(0xFFF59E0B).copy(alpha = 0.60f)
        TaskPriority.LOW -> Color(0xFF38BDF8).copy(alpha = 0.45f)
    }
    val badgeColor = when (task.priority) {
        TaskPriority.HIGH -> Color(0xFFEF4444)
        TaskPriority.MEDIUM -> Color(0xFFF59E0B)
        TaskPriority.LOW -> Color(0xFF38BDF8)
    }

    val dateFormat = remember { SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()) }
    val remainingMillis = task.deadlineMillis - nowMillis
    val countdownText = remember(remainingMillis) {
        if (remainingMillis <= 0L) {
            "Deadline passed"
        } else {
            val days = TimeUnit.MILLISECONDS.toDays(remainingMillis)
            val hours = TimeUnit.MILLISECONDS.toHours(remainingMillis) % 24
            val mins = TimeUnit.MILLISECONDS.toMinutes(remainingMillis) % 60
            when {
                days > 0 -> "${days}d ${hours}h ${mins}m left"
                hours > 0 -> "${hours}h ${mins}m left"
                else -> "Urgent: ${mins.coerceAtLeast(1)}m left"
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .testTag("chat_task_card_${task.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.5.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeColor.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${task.priority.label} PRIORITY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.6.sp
                            ),
                            color = badgeColor
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = task.senderRole,
                        style = MaterialTheme.typography.labelSmall,
                        color = ClgColors.TextMuted
                    )
                }

                if (task.priority == TaskPriority.HIGH) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = 0.25f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Countdown",
                            tint = Color(0xFFFCA5A5),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = countdownText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFCA5A5)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                ),
                color = if (isCompleted) ClgColors.TextMuted else Color.White
            )

            if (task.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ClgColors.TextMuted
                )
            }

            if (task.targetGroup.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    task.targetGroup.forEach { group ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = group,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Due: ${dateFormat.format(Date(task.deadlineMillis))} ($countdownText)",
                style = MaterialTheme.typography.labelMedium,
                color = badgeColor
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onToggleComplete(!isCompleted) }
                        .padding(end = 8.dp)
                        .testTag("complete_checkbox_${task.id}")
                ) {
                    Checkbox(
                        checked = isCompleted,
                        onCheckedChange = onToggleComplete,
                        colors = CheckboxDefaults.colors(
                            checkedColor = ClgColors.Cyan,
                            checkmarkColor = ClgColors.Navy
                        )
                    )
                    Text(
                        text = if (isCompleted) "Completed" else "Mark as Completed",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isCompleted) ClgColors.Cyan else Color.White
                    )
                }

                OutlinedButton(
                    onClick = onSetReminder,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ClgColors.Cyan.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("set_reminder_button_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = "Set Custom Reminder",
                        tint = ClgColors.Cyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Set Reminder",
                        style = MaterialTheme.typography.labelMedium,
                        color = ClgColors.Cyan
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateChatTaskDialog(
    senderRole: String,
    defaultGroup: String,
    onDismiss: () -> Unit,
    onSubmit: (title: String, desc: String, groups: List<String>, priority: TaskPriority, deadlineMillis: Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var groupText by remember { mutableStateOf(defaultGroup) }
    var priority by remember { mutableStateOf(TaskPriority.HIGH) }
    var hoursFromNow by remember { mutableStateOf("24") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Post Official Task ($senderRole)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task / Announcement Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Instructions") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = groupText,
                    onValueChange = { groupText = it },
                    label = { Text("Target Groups (comma separated)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = hoursFromNow,
                    onValueChange = { hoursFromNow = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Deadline (hours from now)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Priority", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskPriority.entries.forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ClgColors.Cyan.copy(alpha = 0.25f)
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val hrs = hoursFromNow.toLongOrNull()?.coerceAtLeast(1L) ?: 24L
                    val deadline = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(hrs)
                    val groups = groupText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    onSubmit(title, description, groups, priority, deadline)
                },
                enabled = title.isNotBlank()
            ) {
                Text("Send to CHATS")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CustomReminderDialog(
    task: ChatTask,
    onDismiss: () -> Unit,
    onSchedule: (delayInMinutes: Long) -> Unit
) {
    var customMinutes by remember { mutableStateOf("120") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = ClgColors.Cyan)
        },
        title = { Text("Set Custom Reminder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Schedule an offline local reminder for \"${task.title}\":",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onSchedule(30L) },
                        modifier = Modifier.weight(1f)
                    ) { Text("30m") }
                    OutlinedButton(
                        onClick = { onSchedule(120L) },
                        modifier = Modifier.weight(1f)
                    ) { Text("2 hrs") }
                    OutlinedButton(
                        onClick = { onSchedule(1440L) },
                        modifier = Modifier.weight(1f)
                    ) { Text("1 day") }
                }
                OutlinedTextField(
                    value = customMinutes,
                    onValueChange = { customMinutes = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Or custom delay (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val mins = customMinutes.toLongOrNull()?.coerceAtLeast(1L) ?: 60L
                    onSchedule(mins)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ClgColors.Cyan,
                    contentColor = ClgColors.Navy
                )
            ) {
                Text("Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
