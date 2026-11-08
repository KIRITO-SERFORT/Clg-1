package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.R
import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.BuildConfig
import com.example.util.DeviceSetup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceSummary
import com.example.data.model.SubjectAttendance
import com.example.data.network.ErpConnectors
import com.example.data.preferences.ThemeMode
import com.example.ui.theme.EmeraldTertiary
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.StatusCaution
import com.example.ui.theme.StatusCautionContainer
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusDangerContainer
import com.example.ui.theme.StatusSafe
import com.example.ui.theme.StatusSafeContainer
import com.example.ui.theme.TealSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class DashboardTab {
    OVERVIEW,
    SUBJECTS,
    SIMULATOR,
    EXAMS,
    SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    username: String,
    studentName: String,
    summary: AttendanceSummary,
    subjects: List<SubjectAttendance>,
    targetThreshold: Double,
    themeMode: ThemeMode,
    notificationsEnabled: Boolean,
    isRefreshing: Boolean,
    onRefreshClick: () -> Unit,
    onThresholdChange: (Double) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onNotificationsEnabledChange: (Boolean) -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(DashboardTab.OVERVIEW) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out? Your credentials and background sync will be cleared from this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogoutClick()
                    }
                ) {
                    Text("Sign Out", color = StatusDanger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "Clg 1% logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (studentName.isNotBlank()) studentName else "Clg 1%",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Reg: ${username.uppercase(Locale.getDefault())} • ${ErpConnectors.DEFAULT_SHORT_NAME}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onRefreshClick,
                        enabled = !isRefreshing,
                        modifier = Modifier.testTag("refresh_button")
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Attendance",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.testTag("logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = selectedTab == DashboardTab.OVERVIEW,
                    onClick = { selectedTab = DashboardTab.OVERVIEW },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Overview") },
                    label = { Text("Overview") }
                )
                NavigationBarItem(
                    selected = selectedTab == DashboardTab.SUBJECTS,
                    onClick = { selectedTab = DashboardTab.SUBJECTS },
                    icon = { Icon(Icons.Default.School, contentDescription = "Subjects") },
                    label = { Text("Subjects") }
                )
                NavigationBarItem(
                    selected = selectedTab == DashboardTab.SIMULATOR,
                    onClick = { selectedTab = DashboardTab.SIMULATOR },
                    icon = { Icon(Icons.Default.Insights, contentDescription = "Analytics") },
                    label = { Text("Analytics") }
                )
                NavigationBarItem(
                    selected = selectedTab == DashboardTab.EXAMS,
                    onClick = { selectedTab = DashboardTab.EXAMS },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Exams") },
                    label = { Text("Exams") }
                )
                NavigationBarItem(
                    selected = selectedTab == DashboardTab.SETTINGS,
                    onClick = { selectedTab = DashboardTab.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                DashboardTab.OVERVIEW -> {
                    OverviewTabContent(
                        summary = summary,
                        subjects = subjects,
                        targetThreshold = targetThreshold,
                        onThresholdChange = onThresholdChange,
                        onNavigateToExams = { selectedTab = DashboardTab.EXAMS }
                    )
                }

                DashboardTab.SUBJECTS -> {
                    SubjectsTabContent(
                        subjects = subjects,
                        targetThreshold = targetThreshold
                    )
                }

                DashboardTab.SIMULATOR -> {
                    AnalyticsAndSimulatorTabContent(
                        summary = summary,
                        subjects = subjects,
                        targetThreshold = targetThreshold
                    )
                }

                DashboardTab.EXAMS -> {
                    ExamCommandCenterScreen()
                }

                DashboardTab.SETTINGS -> {
                    SettingsTabContent(
                        themeMode = themeMode,
                        notificationsEnabled = notificationsEnabled,
                        targetThreshold = targetThreshold,
                        onThemeModeChange = onThemeModeChange,
                        onNotificationsEnabledChange = onNotificationsEnabledChange,
                        onThresholdChange = onThresholdChange
                    )
                }
            }
        }
    }
}

// =========================================================================
// Tab 1: Overview
// =========================================================================

@Composable
private fun OverviewTabContent(
    summary: AttendanceSummary,
    subjects: List<SubjectAttendance>,
    targetThreshold: Double,
    onThresholdChange: (Double) -> Unit,
    onNavigateToExams: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val percentage = summary.percentage
    val targetPercent = (targetThreshold * 100).toInt()
    val safeMiss = summary.safeToMiss(targetThreshold)
    val hoursRecover = summary.hoursToRecover(targetThreshold)

    val statusColor = when {
        percentage >= targetPercent -> StatusSafe
        percentage >= (targetPercent - 5) -> StatusCaution
        else -> StatusDanger
    }

    val animatedSweep by animateFloatAsState(
        targetValue = (percentage.toFloat() / 100f).coerceIn(0f, 1f) * 360f,
        animationSpec = tween(durationMillis = 1000),
        label = "percentage_arc"
    )

    val subjectShortages = subjects.filter { it.isBelowExamThreshold(0.75) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Attendance Percentage Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val strokeBg = MaterialTheme.colorScheme.surfaceVariant
                    Canvas(modifier = Modifier.size(190.dp)) {
                        drawArc(
                            color = strokeBg,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = statusColor,
                            startAngle = -90f,
                            sweepAngle = animatedSweep,
                            useCenter = false,
                            style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = summary.formattedPercentage,
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 38.sp
                            ),
                            color = statusColor
                        )
                        Text(
                            text = "Target: $targetPercent%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Status Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(
                            when {
                                percentage >= targetPercent -> StatusSafeContainer
                                percentage >= (targetPercent - 5) -> StatusCautionContainer
                                else -> StatusDangerContainer
                            }
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when {
                            percentage >= targetPercent -> Icons.Default.CheckCircle
                            else -> Icons.Default.Warning
                        },
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            percentage >= targetPercent -> "Above Target Margin"
                            percentage >= (targetPercent - 5) -> "Near Target Margin"
                            else -> "Attendance Shortage Alert"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = statusColor
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "${summary.present} Hours Present / ${summary.marked} Hours Marked",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Exam Eligibility Summary Pill
        if (subjects.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (subjectShortages.isEmpty()) StatusSafeContainer else StatusDangerContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (subjectShortages.isEmpty()) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (subjectShortages.isEmpty()) StatusSafe else StatusDanger,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (subjectShortages.isEmpty()) {
                                "All Enrolled Subjects Meet 75% Exam Criteria"
                            } else {
                                "Exam Shortage Warning in ${subjectShortages.size} Subject(s)!"
                            },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (subjectShortages.isEmpty()) StatusSafe else StatusDanger
                        )
                        Text(
                            text = if (subjectShortages.isEmpty()) {
                                "You are eligible to sit for all semester examinations."
                            } else {
                                "Immediate recovery needed in: ${subjectShortages.joinToString { it.code }}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Compact timetable quick view (today's periods; red = subject below target)
        TimetableCard(
            subjects = subjects,
            targetThreshold = targetThreshold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Bunk / Safe to Miss or Recovery Hours Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (safeMiss > 0) StatusSafeContainer else StatusDangerContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (safeMiss > 0) "Safe to Miss (Overall)" else "Hours Needed to Recover",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (safeMiss > 0) StatusSafe else StatusDanger
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when {
                            safeMiss > 0 -> "You can skip $safeMiss more hours and still maintain $targetPercent% overall."
                            hoursRecover > 0 -> "Attend the next $hoursRecover hours consecutively without absence to reach $targetPercent%."
                            else -> "You are exactly on the $targetPercent% threshold."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(if (safeMiss > 0) StatusSafe else StatusDanger),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (if (safeMiss > 0) safeMiss else hoursRecover).toString(),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Detailed Attendance Breakdown Grid
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "College ERP Attendance Data",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                StatRow(label = "Hours Present", value = "${summary.present} hrs", valueColor = StatusSafe)
                StatDivider()
                StatRow(label = "Hours Absent", value = "${summary.absent} hrs", valueColor = StatusDanger)
                StatDivider()
                StatRow(label = "Total Hours Marked", value = "${summary.marked} hrs")
                StatDivider()
                StatRow(label = "Total Hours Not Marked", value = "${summary.notMarked} hrs")
                StatDivider()
                StatRow(label = "Total Working Hours", value = "${summary.totalWorking} hrs")
                StatDivider()
                StatRow(label = "Portal Percentage (Rounded)", value = "${summary.apiPercentage}%")
                if (summary.tillDate.isNotBlank()) {
                    StatDivider()
                    StatRow(label = "ERP Record As Of", value = summary.tillDate)
                }

                val formattedSyncTime = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                    .format(Date(summary.lastSyncedMillis))
                StatDivider()
                StatRow(label = "Last Synced Time", value = formattedSyncTime)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // DataVis & Academic Setup Shortcut Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Insights,
                        contentDescription = "DataVis",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DataVis Analytics & Grading Setup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Export your academic data as a CSV, access community grading templates, and visualize deep performance distributions on DataVis.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onNavigateToExams,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Exams & Grading")
                    }

                    FilledTonalButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://datavis-v5.streamlit.app/"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open DataVis")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// =========================================================================
// Tab 2: Subject-Wise Attendance
// =========================================================================

@Composable
private fun SubjectsTabContent(
    subjects: List<SubjectAttendance>,
    targetThreshold: Double
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Subject-Wise Attendance",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Target attendance: ${(targetThreshold * 100).roundToInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(NavyPrimary.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${subjects.size} Subjects",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = NavyPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (subjects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No subject records loaded. Tap Refresh on the top bar to synchronize.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            subjects.forEach { subject ->
                SubjectCard(subject = subject, targetThreshold = targetThreshold)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SubjectCard(
    subject: SubjectAttendance,
    targetThreshold: Double
) {
    val isDebarred = subject.isBelowExamThreshold(0.75)
    val safeMiss = subject.safeToMiss(0.75)
    val recover = subject.hoursToRecover(0.75)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Code & Percentage Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = subject.code,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(if (isDebarred) StatusDangerContainer else StatusSafeContainer)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${String.format("%.1f", subject.percentage)}%",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (isDebarred) StatusDanger else StatusSafe
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = subject.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (subject.facultyName.isNotBlank()) {
                Text(
                    text = "Faculty: ${subject.facultyName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Hours Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Present: ${subject.presentHours}/${subject.totalHours} hrs",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Absent: ${subject.absentHours} hrs",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = if (subject.absentHours > 0) StatusDanger else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Open: ${subject.openHours} hrs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Status notice
            Text(
                text = when {
                    subject.totalHours == 0 -> "No classes marked yet."
                    isDebarred -> "⚠️ Shortage for Exam: Attend the next $recover hour(s) to reach 75%!"
                    safeMiss > 0 -> "✅ Exam Eligible: Can safely miss up to $safeMiss class(es)."
                    else -> "✅ On the 75% threshold. Cannot miss upcoming classes."
                },
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (isDebarred) StatusDanger else StatusSafe
            )
        }
    }
}

// =========================================================================
// Tab 3: Analytics & What-If Simulator
// =========================================================================

@Composable
private fun AnalyticsAndSimulatorTabContent(
    summary: AttendanceSummary,
    subjects: List<SubjectAttendance>,
    targetThreshold: Double
) {
    val scrollState = rememberScrollState()

    var deltaClasses by remember { mutableIntStateOf(0) }
    var simulationMode by remember { mutableStateOf("attend") } // "attend" or "bunk"

    val basePresent = summary.present
    val baseMarked = summary.marked

    val simulatedPresent = if (simulationMode == "attend") {
        basePresent + deltaClasses
    } else {
        basePresent
    }
    val simulatedMarked = baseMarked + deltaClasses

    val simulatedPercentage = if (simulatedMarked > 0) {
        (simulatedPresent * 100.0) / simulatedMarked
    } else 0.0

    val diffPercentage = simulatedPercentage - summary.percentage

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Attendance Trend & Trajectory",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp)
        )
        Text(
            text = "Visual comparison against the 75% critical cutoff line",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp).padding(bottom = 16.dp)
        )

        // Trajectory Chart Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                AttendanceTrajectoryGraph(
                    currentPercentage = summary.percentage,
                    thresholdPercent = (targetThreshold * 100).toFloat(),
                    presentHours = summary.present,
                    markedHours = summary.marked
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    LegendItem(color = StatusSafe, label = "Current Path (${summary.formattedPercentage})")
                    LegendItem(color = StatusDanger, label = "75% Cutoff Line", isDashed = true)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // "What-If" Class Attendance Simulator
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Insights,
                        contentDescription = null,
                        tint = TealSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "What-If Attendance Simulator",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "Simulate attending or skipping upcoming classes to project your exact attendance percentage.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // Mode Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = simulationMode == "attend",
                        onClick = { simulationMode = "attend" },
                        label = { Text("Attending Classes") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StatusSafe,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = simulationMode == "bunk",
                        onClick = { simulationMode = "bunk" },
                        label = { Text("Skipping / Bunking") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StatusDanger,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stepper Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Upcoming classes to simulate:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (deltaClasses > 0) deltaClasses-- },
                            enabled = deltaClasses > 0
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease")
                        }

                        Text(
                            text = "$deltaClasses hrs",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        IconButton(
                            onClick = { if (deltaClasses < 30) deltaClasses++ },
                            enabled = deltaClasses < 30
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Result Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (simulatedPercentage >= 75) StatusSafeContainer else StatusDangerContainer
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Projected Attendance:",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (simulatedPercentage >= 75) StatusSafe else StatusDanger
                            )

                            Text(
                                text = "${String.format("%.2f", simulatedPercentage)}%",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (simulatedPercentage >= 75) StatusSafe else StatusDanger
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        val diffStr = if (diffPercentage >= 0) {
                            "+${String.format("%.2f", diffPercentage)}%"
                        } else {
                            "${String.format("%.2f", diffPercentage)}%"
                        }

                        Text(
                            text = if (deltaClasses == 0) {
                                "Adjust the counter above to see future projections."
                            } else if (simulationMode == "attend") {
                                "Attending $deltaClasses more class(es) will increase your attendance from ${summary.formattedPercentage} to ${String.format("%.1f", simulatedPercentage)}% ($diffStr)."
                            } else {
                                "Missing $deltaClasses class(es) will drop your attendance from ${summary.formattedPercentage} to ${String.format("%.1f", simulatedPercentage)}% ($diffStr)."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun AttendanceTrajectoryGraph(
    currentPercentage: Double,
    thresholdPercent: Float,
    presentHours: Int,
    markedHours: Int
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        val width = size.width
        val height = size.height

        val paddingBottom = 20.dp.toPx()
        val paddingTop = 10.dp.toPx()
        val graphHeight = height - paddingBottom - paddingTop

        fun yForPercent(p: Float): Float {
            // Map 50%..100% to graphHeight
            val clamped = p.coerceIn(50f, 100f)
            val normalized = (clamped - 50f) / 50f
            return (paddingTop + graphHeight * (1f - normalized))
        }

        // Draw horizontal grid lines (60%, 70%, 80%, 90%, 100%)
        listOf(60f, 75f, 90f, 100f).forEach { p ->
            val y = yForPercent(p)
            drawLine(
                color = Color.LightGray.copy(alpha = 0.4f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // 75% Cutoff line (Dashed red)
        val cutoffY = yForPercent(thresholdPercent)
        drawLine(
            color = StatusDanger,
            start = Offset(0f, cutoffY),
            end = Offset(width, cutoffY),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
        )

        // Trajectory curve
        val path = Path()
        val startY = yForPercent(85f)
        val midY = yForPercent(((currentPercentage + 85f) / 2f).toFloat())
        val endY = yForPercent(currentPercentage.toFloat())

        path.moveTo(0f, startY)
        path.cubicTo(
            width * 0.4f, midY,
            width * 0.7f, endY,
            width, endY
        )

        drawPath(
            path = path,
            color = StatusSafe,
            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // Current point dot
        drawCircle(
            color = StatusSafe,
            radius = 6.dp.toPx(),
            center = Offset(width, endY)
        )
        drawCircle(
            color = Color.White,
            radius = 3.dp.toPx(),
            center = Offset(width, endY)
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String, isDashed: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(3.dp)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// =========================================================================
// Tab 4: Settings, About & Contact
// =========================================================================

private const val CONTACT_EMAIL = "arunsanna071@gmail.com"

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text = text.uppercase(Locale.getDefault()),
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .padding(start = 4.dp, top = 10.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsIconBadge(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun SettingsTabContent(
    themeMode: ThemeMode,
    notificationsEnabled: Boolean,
    targetThreshold: Double,
    onThemeModeChange: (ThemeMode) -> Unit,
    onNotificationsEnabledChange: (Boolean) -> Unit,
    onThresholdChange: (Double) -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var showContactDialog by remember { mutableStateOf(false) }
    var showTeaDialog by remember { mutableStateOf(false) }
    var awaitingUpiReturn by remember { mutableStateOf(false) }
    var showThanksDialog by remember { mutableStateOf(false) }
    var batteryUnrestricted by remember { mutableStateOf(DeviceSetup.isBatteryUnrestricted(context)) }

    // Re-check the battery status whenever the user comes back from the system settings screen.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                batteryUnrestricted = DeviceSetup.isBatteryUnrestricted(context)
                if (awaitingUpiReturn) {
                    awaitingUpiReturn = false
                    showThanksDialog = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showContactDialog) {
        ContactDialog(onDismiss = { showContactDialog = false })
    }

    if (showTeaDialog) {
        TeaDialog(
            onDismiss = { showTeaDialog = false },
            onPaymentLaunched = { awaitingUpiReturn = true }
        )
    }

    if (showThanksDialog) {
        PaymentCheckDialog(onDismiss = { showThanksDialog = false })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .padding(start = 4.dp)
        )

        // ---------------- Appearance ----------------
        SettingsSectionLabel("Appearance")
        SettingsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SettingsIconBadge(Icons.Default.Palette, MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    ThemeMode.SYSTEM to "System",
                    ThemeMode.LIGHT to "Light",
                    ThemeMode.DARK to "Dark"
                ).forEach { (mode, label) ->
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { onThemeModeChange(mode) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ---------------- Alerts & goals ----------------
        SettingsSectionLabel("Alerts & goals")
        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsIconBadge(Icons.Default.Notifications, TealSecondary)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Attendance shortage alerts",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Get a warning if any subject drops below your ${(targetThreshold * 100).roundToInt()}% target.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { onNotificationsEnabledChange(it) }
                )
            }
        }

        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsIconBadge(Icons.Default.CheckCircle, EmeraldTertiary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Target attendance goal",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${(targetThreshold * 100).toInt()}%",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = targetThreshold.toFloat(),
                onValueChange = { onThresholdChange((it * 100).roundToInt() / 100.0) },
                valueRange = 0.60f..0.90f,
                steps = 29,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Used for the \"safe to miss\" and \"hours to recover\" numbers.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ---------------- Background updates ----------------
        SettingsSectionLabel("Background updates")
        SettingsCard {
            val statusColor = if (batteryUnrestricted) StatusSafe else StatusCaution
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsIconBadge(Icons.Default.BatteryChargingFull, statusColor)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Widget reliability",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = if (batteryUnrestricted) {
                            "Battery usage: Unrestricted"
                        } else {
                            "Battery usage: Restricted. Your phone may pause widget updates."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = statusColor,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = DeviceSetup.autostartHint(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (!batteryUnrestricted) {
                Button(
                    onClick = { DeviceSetup.requestBatteryExemption(context) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Allow unrestricted battery")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            OutlinedButton(
                onClick = {
                    if (!DeviceSetup.openAutostartSettings(context)) {
                        Toast.makeText(context, "Couldn't open settings on this phone", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open autostart settings")
            }
        }

        // ---------------- About ----------------
        SettingsSectionLabel("About")
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "Clg 1% logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            RoundedCornerShape(18.dp)
                        )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Clg 1%",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Attendance, marks and exams, using the rules of your own college.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Developer note (quote) followed by the Android Studio logo
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Made with love by kirito-serfort a.k.a Arun Sanna",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Developer's note: made by a mech student with the help of ChatGPT.",
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_android_studio),
                                contentDescription = "Android Studio",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Android Studio",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // ---------------- Contact & bugs ----------------
        Spacer(modifier = Modifier.height(6.dp))
        FilledTonalButton(
            onClick = { showContactDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .height(52.dp)
                .testTag("contact_bug_button"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Contact & Bugs Report",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = { showTeaDialog = true },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("tea_button")
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_tea_cup),
                    contentDescription = "Buy me a tea",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(30.dp)
                )
            }
            Image(
                painter = painterResource(id = R.drawable.ikegai_telos_logo),
                contentDescription = "Ikegai Telos website",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .clickable { openIkegaiWebsite(context) }
            )
        }
        Text(
            text = "Still in development  •  v${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun ToolChip(label: String, icon: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContactDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    fun copyEmail() {
        clipboard.setText(AnnotatedString(CONTACT_EMAIL))
        Toast.makeText(context, "Email copied", Toast.LENGTH_SHORT).show()
    }

    fun writeEmail() {
        val body = "Describe the problem or suggestion:\n\n\n---\n" +
            "App: Clg 1% ${BuildConfig.VERSION_NAME}\n" +
            "Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n" +
            "Device: ${Build.MANUFACTURER} ${Build.MODEL}"
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(CONTACT_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, "Clg 1% - Bug report")
            putExtra(Intent.EXTRA_TEXT, body)
        }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            copyEmail() // no mail app installed: copy the address instead
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        title = { Text("Contact & Bugs Report") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ---- Contact ----
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "CONTACT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { writeEmail() }
                            .padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Email: $CONTACT_EMAIL",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Opens a draft in your email app. Nothing is sent until you press Send.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { copyEmail() }) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy email",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ---- Tools used ----
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "TOOLS USED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ToolChip("AI") {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        ToolChip("Android Studio") {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_android_studio),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        ToolChip("VS Code") {
                            Icon(
                                Icons.Default.Code,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // ---- Batch box (different font) ----
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.5.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "College and academic structure are configurable in Exam Command Center",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }

                // ---- Status note ----
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Construction,
                        contentDescription = null,
                        tint = StatusCaution,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Still in development. Some features may change and bugs are expected.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

// =========================================================================
// Helpers
// =========================================================================

@Composable
private fun StatRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = valueColor
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )
}

// =========================================================================
// "Buy me a tea" window, UPI payment and website link
// =========================================================================

private const val UPI_ID = "arunsanna071@okaxis"
private const val UPI_PAYEE_NAME = "Arun Sanna"
private const val IKEGAI_WEBSITE_URL = "https://ikegai-telos.kiritokitagawa77.workers.dev/"

private fun openIkegaiWebsite(context: Context) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(IKEGAI_WEBSITE_URL)))
    } catch (_: Exception) {
        Toast.makeText(context, "Couldn't open the website", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Opens a UPI app (Google Pay, PhonePe, Paytm...) with the payee already filled in.
 * No amount is passed, so the person types any amount they like.
 * If no UPI app exists, [copyUpiId] is called so the ID can be pasted manually.
 */
private fun startUpiPayment(context: Context, copyUpiId: () -> Unit): Boolean {
    val uri = Uri.parse(
        "upi://pay?pa=$UPI_ID" +
            "&pn=${Uri.encode(UPI_PAYEE_NAME)}" +
            "&tn=${Uri.encode("Tea for Clg 1%")}" +
            "&cu=INR"
    )
    val intent = Intent(Intent.ACTION_VIEW, uri)
    val hasUpiApp = context.packageManager.queryIntentActivities(intent, 0).isNotEmpty()
    if (!hasUpiApp) {
        copyUpiId()
        Toast.makeText(context, "No UPI app found. UPI ID copied.", Toast.LENGTH_LONG).show()
        return false
    }
    return try {
        context.startActivity(Intent.createChooser(intent, "Pay with"))
        true
    } catch (_: Exception) {
        copyUpiId()
        Toast.makeText(context, "Couldn't open UPI. UPI ID copied.", Toast.LENGTH_LONG).show()
        false
    }
}

@Composable
private fun TeaDialog(onDismiss: () -> Unit, onPaymentLaunched: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_tea_cup),
                    contentDescription = "Tea cup",
                    modifier = Modifier.size(88.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Buy me a tea?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "\uD83D\uDE02", fontSize = 36.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "A voluntary tip. It does not unlock any features.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        val launched = startUpiPayment(context) {
                            clipboard.setText(AnnotatedString(UPI_ID))
                        }
                        if (launched) onPaymentLaunched()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text(text = "ofc", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Shown when the person comes back from the UPI app. Returning from a UPI app is NOT proof of payment,
 * so we ask instead of assuming. Nothing in the app is unlocked by a tea, whatever the answer is.
 */
@Composable
private fun PaymentCheckDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Did your payment complete?") },
        text = {
            Text(
                "Coming back from a UPI app does not tell Clg 1% whether a payment went through, " +
                    "so we are asking instead of assuming. A tea is a voluntary tip and does not unlock anything in the app."
            )
        },
        confirmButton = {
            TextButton(onClick = {
                Toast.makeText(context, "Thank you for the tea!", Toast.LENGTH_SHORT).show()
                onDismiss()
            }) { Text("Yes, it completed") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("No / not sure") }
        }
    )
}
