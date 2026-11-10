package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Insights
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.example.data.template.TemplateLibrary
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.backup.EncryptedAcademicBackup
import com.example.data.model.AcademicRules
import com.example.data.model.AssessmentComponentDefinition
import com.example.data.model.AcademicSubject
import com.example.data.model.AssessmentMark
import com.example.data.model.GradeBand
import com.example.data.model.MarkStatus
import com.example.data.model.CombinationMethod
import com.example.data.model.MissingMarkPolicy
import com.example.data.model.CollegeTemplate
import com.example.data.template.AcademicTemplateCodec
import com.example.data.template.AcademicProfileCodec
import com.example.util.AcademicFileSharing
import com.example.worker.ExamReminderWorker
import com.example.billing.PlayBillingCard
import com.example.billing.PlayEntitlementVerifier
import com.example.datavis.DataVisTransferClient
import com.example.domain.AcademicRulesEngine
import com.example.domain.AcademicAnalytics
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import com.google.zxing.integration.android.IntentIntegrator
import com.example.data.local.AcademicRoomRepository
import com.example.data.local.Clg1Database
import com.example.data.analytics.AcademicHistoryService
import com.example.data.analytics.AcademicPdfReportGenerator
import com.example.data.analytics.HistoricalAcademicAnalytics
import com.example.util.AcademicQrCodec
import java.io.File
import java.io.InputStream
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream

private data class LocalExam(val title: String, val subject: String, val dateMillis: Long, val notes: String)

@Composable
fun ExamCommandCenterScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("clg1_academic_local", 0) }
    val scope = rememberCoroutineScope()
    var roomSaveJob by remember { mutableStateOf<Job?>(null) }
    var showSetupWizard by remember { mutableStateOf(false) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var historicalSummary by remember { mutableStateOf<com.example.data.analytics.HistoricalAnalyticsSummary?>(null) }
    var freeReportCount by remember { mutableStateOf(context.getSharedPreferences("clg1_entitlements", 0).getInt("deep_report_count", 0)) }
    var isGeneratingHistoricalReport by remember { mutableStateOf(false) }
    var workspaceLoaded by remember { mutableStateOf(false) }
    var college by remember { mutableStateOf(prefs.getString("college", "") ?: "") }
    var term by remember { mutableStateOf(prefs.getString("term", "Semester 1") ?: "Semester 1") }
    var university by remember { mutableStateOf("") }
    var program by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var regulation by remember { mutableStateOf("") }
    var academicYear by remember { mutableStateOf("") }
    var defaultComponents by remember { mutableStateOf(emptyList<AssessmentComponentDefinition>()) }
    var passMark by remember { mutableStateOf(prefs.getString("pass", "40") ?: "40") }
    val defaultGradeBands = remember { listOf(GradeBand(90.0,"O",10.0), GradeBand(80.0,"A+",9.0), GradeBand(70.0,"A",8.0), GradeBand(60.0,"B+",7.0), GradeBand(50.0,"B",6.0), GradeBand(40.0,"C",5.0), GradeBand(0.0,"F",0.0)) }
    var gradeBands by remember { mutableStateOf(defaultGradeBands) }
    var combinationMethod by remember { mutableStateOf(CombinationMethod.WEIGHTED_AVERAGE) }
    var missingMarkPolicy by remember { mutableStateOf(MissingMarkPolicy.EXCLUDE_AND_RENORMALIZE) }
    var bestN by remember { mutableStateOf(1) }
    val subjects = remember { mutableStateListOf<AcademicSubject>() }
    val exams = remember { mutableStateListOf<LocalExam>() }
    var showSubjectDialog by remember { mutableStateOf(false) }
    var showAssessmentDialog by remember { mutableStateOf(false) }
    var selectedSubjectId by remember { mutableStateOf<String?>(null) }
    var editingAssessmentIndex by remember { mutableStateOf<Int?>(null) }
    var componentName by remember { mutableStateOf("Internal test") }
    var componentObtained by remember { mutableStateOf("") }
    var componentMaximum by remember { mutableStateOf("25") }
    var componentWeight by remember { mutableStateOf("50") }
    var componentMinimumPass by remember { mutableStateOf("") }
    var componentStatus by remember { mutableStateOf(MarkStatus.NOT_CONDUCTED) }
    var targetPercent by remember { mutableStateOf("75") }
    var targetSgpaText by remember { mutableStateOf("") }
    val whatIfInputs = remember { mutableStateMapOf<String, String>() }
    var showExamDialog by remember { mutableStateOf(false) }
    var subjectName by remember { mutableStateOf("") }
    var subjectCode by remember { mutableStateOf("") }
    var credits by remember { mutableStateOf("3") }
    var subjectPassPercent by remember { mutableStateOf("") }
    var assessmentName by remember { mutableStateOf("Internal assessment") }
    var obtained by remember { mutableStateOf("") }
    var maximum by remember { mutableStateOf("25") }
    var weight by remember { mutableStateOf("100") }
    var examTitle by remember { mutableStateOf("") }
    var examSubject by remember { mutableStateOf("") }
    var examDate by remember { mutableStateOf("") }
    var examTime by remember { mutableStateOf("09:00") }
    var examNotes by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val selectedExportIds = remember { mutableStateMapOf<String, Boolean>() }
    var pendingCsvSubjectCount by remember { mutableStateOf(0) }
    var pendingCsvRowCount by remember { mutableStateOf(0) }
    var isTransferringCsv by remember { mutableStateOf(false) }
    var pendingTemplate by remember { mutableStateOf<CollegeTemplate?>(null) }
    var showCommunityTemplatesDialog by remember { mutableStateOf(false) }
    var showSaveTemplateDialog by remember { mutableStateOf(false) }
    var showPasteTemplateDialog by remember { mutableStateOf(false) }
    var pasteTemplateText by remember { mutableStateOf("") }
    var saveTemplateTitle by remember { mutableStateOf("") }
    var localTemplates by remember { mutableStateOf(TemplateLibrary.getSavedLocalTemplates(context)) }
    var showExportCsvSuccessDialog by remember { mutableStateOf(false) }
    var exportedCsvString by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    var showTemplateReplaceConfirm by remember { mutableStateOf(false) }
    var pendingCsv by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showBackupPassphraseDialog by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var backupMode by remember { mutableStateOf("export") }
    var backupPassphrase by remember { mutableStateOf("") }
    var pendingBackupBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pendingRestoreJson by remember { mutableStateOf<JSONObject?>(null) }
    var pendingRestoreBytes by remember { mutableStateOf<ByteArray?>(null) }
    var remindersEnabled by remember { mutableStateOf(prefs.getBoolean("exam_reminders_enabled", false)) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            remindersEnabled = true
            prefs.edit().putBoolean("exam_reminders_enabled", true).apply()
            exams.forEach { scheduleExamReminder(context, it) }
        } else message = "Notification permission was not granted. Exam reminders remain disabled."
    }
    val createBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val bytes = pendingBackupBytes
        if (uri != null && bytes != null) runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("Could not create backup file") }
            .onSuccess { message = "Encrypted academic backup saved." }
            .onFailure { message = "Backup save failed: ${it.message}" }
        pendingBackupBytes = null
    }
    val openBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytesLimited(6 * 1024 * 1024) } ?: error("Could not read backup") }
            .onSuccess { pendingRestoreBytes = it; backupMode = "restore"; backupPassphrase = ""; showBackupPassphraseDialog = true }
            .onFailure { message = "Backup read failed: ${it.message}" }
    }
    val templateImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytesLimited(1024 * 1024).toString(Charsets.UTF_8) } ?: error("Could not read template") }
                .mapCatching { AcademicTemplateCodec.parse(it) }
                .onSuccess { pendingTemplate = it }
                .onFailure { message = "Template import rejected: ${it.message}" }
        }
    }
    val qrScanLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val scan = IntentIntegrator.parseActivityResult(result.resultCode, result.data)
        val contents = scan?.contents
        if (result.resultCode == RESULT_OK && !contents.isNullOrBlank()) {
            runCatching { AcademicQrCodec.decode(contents) }
                .mapCatching { AcademicTemplateCodec.parse(it) }
                .onSuccess { pendingTemplate = it }
                .onFailure { message = "QR template rejected: ${it.message}" }
        } else if (result.resultCode == RESULT_OK) message = "No QR content was detected."
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        runCatching { AcademicRoomRepository.migrateLegacyPreferences(context) }
            .onFailure { message = "Local database migration is pending: ${it.message}" }
        runCatching { historicalSummary = HistoricalAcademicAnalytics.summarize(Clg1Database.get(context).academicDao().getSnapshots()) }
    }

    fun save(
        wizardResult: AcademicWizardResult? = null,
        collegeOverride: String? = null,
        termOverride: String? = null,
        passOverride: String? = null,
        gradeBandsOverride: List<GradeBand>? = null,
        combinationOverride: CombinationMethod? = null,
        missingOverride: MissingMarkPolicy? = null,
        bestNOverride: Int? = null,
        profileOverride: JSONObject? = null
    ) {
        val savedCollege = wizardResult?.profile?.institution ?: collegeOverride ?: college
        val savedTerm = wizardResult?.profile?.periods?.firstOrNull()?.name ?: termOverride ?: term
        val savedPass = wizardResult?.profile?.grading?.passingPercent?.toString() ?: passOverride ?: passMark
        val savedCombination = wizardResult?.combinationMethod ?: combinationOverride ?: combinationMethod
        val savedMissingPolicy = wizardResult?.missingMarkPolicy ?: missingOverride ?: missingMarkPolicy
        val savedBestN = wizardResult?.bestN ?: bestNOverride ?: bestN
        val savedBands = wizardResult?.profile?.grading?.bands ?: gradeBandsOverride ?: gradeBands
        val savedAcademicYear = wizardResult?.profile?.academicYear ?: profileOverride?.optString("academic_year") ?: academicYear
        prefs.edit().putString("college", savedCollege).putString("term", savedTerm).putString("pass", savedPass).putString("academic_year", savedAcademicYear).apply()
        val root = JSONObject()
        root.put("schema_version", 1)
        root.put("college", savedCollege)
        root.put("term", savedTerm)
        root.put("passing_percent", savedPass.toDoubleOrNull() ?: 40.0)
        root.put("combination_method", savedCombination.name).put("missing_mark_policy", savedMissingPolicy.name).put("best_n", savedBestN)
        val bandArray = JSONArray()
        savedBands.forEach { band -> bandArray.put(JSONObject().put("minimum", band.minimumPercent).put("label", band.label).put("point", band.gradePoint).put("inclusive", band.inclusive)) }
        root.put("grade_bands", bandArray)
        val subArray = JSONArray()
        subjects.forEach { s ->
            val obj = JSONObject().put("id", s.id).put("code", s.code).put("name", s.name).put("credits", s.credits)
                .put("subject_pass_percent", s.subjectPassPercent ?: JSONObject.NULL)
            val marks = JSONArray()
            s.assessments.forEach { a -> marks.put(JSONObject().put("name", a.name).put("obtained", a.obtained).put("maximum", a.maximum).put("weight", a.weightPercent).put("status", a.status.name).put("minimum_pass", a.minimumPassPercent ?: JSONObject.NULL).put("category", a.category).put("date", a.dateEpochMillis ?: JSONObject.NULL)) }
            obj.put("assessments", marks); subArray.put(obj)
        }
        root.put("subjects", subArray)
        val examArray = JSONArray()
        exams.forEach { examArray.put(JSONObject().put("title", it.title).put("subject", it.subject).put("date", it.dateMillis).put("notes", it.notes)) }
        root.put("exams", examArray)
        prefs.edit().putString("academic_records_v1", root.toString()).apply()
        val profileMetadata = when {
            profileOverride != null -> JSONObject(profileOverride.toString())
            wizardResult != null -> JSONObject().put("university", wizardResult.profile.university)
                .put("program", wizardResult.profile.program).put("department", wizardResult.profile.department)
                .put("regulation", wizardResult.profile.regulation).put("academic_year", wizardResult.profile.academicYear)
                .put("calendar", wizardResult.profile.calendarType.name)
                .put("periods", JSONArray().apply { wizardResult.profile.periods.forEach { put(JSONObject().put("id", it.id).put("name", it.name).put("order", it.order).put("starts_at", it.startsAtEpochMillis ?: JSONObject.NULL).put("ends_at", it.endsAtEpochMillis ?: JSONObject.NULL).put("exam_at", it.examAtEpochMillis ?: JSONObject.NULL)) } })
                .put("assessment_components", JSONArray().apply { wizardResult.profile.defaultScheme.components.forEach { put(JSONObject().put("id", it.id).put("name", it.name).put("category", it.category).put("maximum", it.maximumMarks).put("weight", it.weightPercent).put("minimum_pass", it.minimumPassPercent ?: JSONObject.NULL).put("required", it.required)) } })
                .put("attendance_affects_marks", wizardResult.profile.grading.attendanceAffectsMarks)
                .put("attendance_marks_maximum", wizardResult.profile.grading.attendanceMarksMaximum ?: JSONObject.NULL)
            else -> runCatching { JSONObject(prefs.getString("academic_profile_v1", "{}") ?: "{}") }.getOrDefault(JSONObject())
                .put("university", university).put("program", program).put("department", department)
                .put("regulation", regulation).put("academic_year", savedAcademicYear)
        }
        prefs.edit().putString("academic_profile_v1", profileMetadata.toString()).apply()
        if (wizardResult != null) {
            runCatching { AcademicProfileCodec.encode(wizardResult.profile) }
                .onSuccess { prefs.edit().putString("academic_profile_full_v1", it).apply() }
                .onFailure { message = "Academic profile metadata saved, but full profile snapshot could not be serialized: ${it.message}" }
        } else if (profileOverride != null) prefs.edit().remove("academic_profile_full_v1").apply()
        roomSaveJob?.cancel()
        roomSaveJob = scope.launch {
            delay(450)
            runCatching { AcademicRoomRepository.persistFromLegacyJson(context, root.toString(), savedCollege, savedTerm, wizardResult?.profile?.academicYear ?: academicYear) }
                .onFailure { message = "Saved locally; database mirror will retry: ${it.message}" }
        }
    }

    fun applyTemplate(template: CollegeTemplate, replaceExisting: Boolean) {
        if (replaceExisting) {
            subjects.clear()
            exams.clear()
        }
        college = template.college
        term = template.termName
        passMark = template.rules.passingPercent.toString()
        gradeBands = template.rules.gradeBands
        defaultComponents = emptyList()
        program = template.program; department = template.department; regulation = template.regulation; academicYear = template.academicYear
        combinationMethod = template.rules.combinationMethod
        missingMarkPolicy = template.rules.missingMarkPolicy
        bestN = template.rules.bestN
        val existingKeys = subjects.map { it.code.trim().lowercase(Locale.ROOT) to it.name.trim().lowercase(Locale.ROOT) }.toMutableSet()
        var addedSubjects = 0
        template.subjects.forEach { item ->
            val key = item.code.trim().lowercase(Locale.ROOT) to item.name.trim().lowercase(Locale.ROOT)
            if (key !in existingKeys) {
                subjects.add(AcademicSubject(UUID.randomUUID().toString(), item.code, item.name, item.credits,
                    item.assessments.map { AssessmentMark(it.name, null, it.maximum, it.weightPercent, MarkStatus.NOT_CONDUCTED, it.minimumPassPercent, it.category) }, item.subjectPassPercent))
                existingKeys.add(key)
                addedSubjects++
            }
        }
        save(
            collegeOverride = template.college,
            termOverride = template.termName,
            passOverride = template.rules.passingPercent.toString(),
            gradeBandsOverride = template.rules.gradeBands,
            combinationOverride = template.rules.combinationMethod,
            missingOverride = template.rules.missingMarkPolicy,
            bestNOverride = template.rules.bestN,
            profileOverride = JSONObject().put("university", university).put("program", template.program)
                .put("department", template.department).put("regulation", template.regulation)
                .put("academic_year", template.academicYear).put("calendar", "SEMESTER")
                .put("assessment_components", JSONArray())
        )
        pendingTemplate = null
        showTemplateReplaceConfirm = false
        message = if (replaceExisting) "Active academic setup replaced. $addedSubjects template subject(s) added; archived history and ERP credentials were preserved."
            else "Template structure merged. Added $addedSubjects new subject(s); existing marks were preserved."
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        try {
            runCatching {
            val profileMeta = runCatching { JSONObject(prefs.getString("academic_profile_v1", "{}") ?: "{}") }.getOrDefault(JSONObject())
            university = profileMeta.optString("university")
            program = profileMeta.optString("program")
            department = profileMeta.optString("department")
            regulation = profileMeta.optString("regulation")
            academicYear = profileMeta.optString("academic_year")
            val configuredComponents = profileMeta.optJSONArray("assessment_components")
            if (configuredComponents != null && configuredComponents.length() <= 100) {
                defaultComponents = (0 until configuredComponents.length()).map { index ->
                    val item = configuredComponents.getJSONObject(index)
                    AssessmentComponentDefinition(item.optString("id"), item.optString("name"), item.optString("category", "assessment"), item.optDouble("maximum"), item.optDouble("weight"), if (item.isNull("minimum_pass")) null else item.optDouble("minimum_pass"), item.optBoolean("required", true))
                }
            }
            val root = JSONObject(prefs.getString("academic_records_v1", "{}") ?: "{}")
            combinationMethod = runCatching { CombinationMethod.valueOf(root.optString("combination_method", CombinationMethod.WEIGHTED_AVERAGE.name)) }.getOrDefault(CombinationMethod.WEIGHTED_AVERAGE)
            missingMarkPolicy = runCatching { MissingMarkPolicy.valueOf(root.optString("missing_mark_policy", MissingMarkPolicy.EXCLUDE_AND_RENORMALIZE.name)) }.getOrDefault(MissingMarkPolicy.EXCLUDE_AND_RENORMALIZE)
            bestN = root.optInt("best_n", 1).coerceAtLeast(1)
            val savedBands = root.optJSONArray("grade_bands")
            if (savedBands != null && savedBands.length() <= 30) {
                val parsedBands = (0 until savedBands.length()).map { i -> savedBands.getJSONObject(i).let { GradeBand(it.optDouble("minimum"), it.optString("label"), it.optDouble("point"), it.optBoolean("inclusive", true)) } }
                if (parsedBands.isNotEmpty()) gradeBands = parsedBands
            }
            val s = root.optJSONArray("subjects") ?: JSONArray()
            for (i in 0 until s.length()) {
                val o = s.getJSONObject(i); val a = o.optJSONArray("assessments") ?: JSONArray()
                val marks = (0 until a.length()).map { j ->
                    val m = a.getJSONObject(j)
                    val markValue = if (m.isNull("obtained")) null else m.optDouble("obtained")
                    val fallbackStatus = if (markValue == null) MarkStatus.NOT_CONDUCTED else MarkStatus.RECORDED
                    val status = runCatching { MarkStatus.valueOf(m.optString("status")) }.getOrDefault(fallbackStatus)
                    AssessmentMark(m.optString("name"), markValue, m.optDouble("maximum", 100.0), m.optDouble("weight", 100.0), status, if (m.isNull("minimum_pass")) null else m.optDouble("minimum_pass"), m.optString("category", "assessment"), if (m.isNull("date")) null else m.optLong("date"))
                }
                subjects.add(AcademicSubject(o.optString("id", UUID.randomUUID().toString()), o.optString("code"), o.optString("name"), o.optDouble("credits", 0.0), marks, if (o.isNull("subject_pass_percent")) null else o.optDouble("subject_pass_percent")))
            }
            val e = root.optJSONArray("exams") ?: JSONArray()
            for (i in 0 until e.length()) { val o = e.getJSONObject(i); exams.add(LocalExam(o.optString("title"), o.optString("subject"), o.optLong("date"), o.optString("notes"))) }
            }.onFailure { message = "Could not load academic workspace: ${it.message}" }
        } finally {
            workspaceLoaded = true
        }
    }

    if (!workspaceLoaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator()
                Text("Loading your academic workspace…")
            }
        }
        return
    }

    val rules = AcademicRules(college = college, termName = term, gradeBands = gradeBands,
        passingPercent = passMark.toDoubleOrNull()?.coerceIn(0.0,100.0) ?: 40.0,
        combinationMethod = combinationMethod, missingMarkPolicy = missingMarkPolicy, bestN = bestN)
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Exam Command Center", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Your academic records stay on this device. Calculations are provisional until all required marks are entered.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Academic setup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(college, { college = it; save() }, Modifier.fillMaxWidth(), label = { Text("College / University (optional)") }, singleLine = true)
                    OutlinedTextField(term, { term = it; save() }, Modifier.fillMaxWidth(), label = { Text("Current term / semester") }, singleLine = true)
                    OutlinedTextField(passMark, { passMark = it; save() }, Modifier.fillMaxWidth(), label = { Text("Passing percentage") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    OutlinedTextField(targetPercent, { targetPercent = it }, Modifier.fillMaxWidth(), label = { Text("Target subject percentage") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    OutlinedTextField(targetSgpaText, { targetSgpaText = it }, Modifier.fillMaxWidth(), label = { Text("Target SGPA (optional)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    Text("Default grade bands are examples only (O ≥90, A+ ≥80, A ≥70, B+ ≥60, B ≥50, C ≥40). Import your institution's grading template or verify the rules before relying on results.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Primary action: Browse & access templates
                FilledTonalButton(
                    onClick = { showCommunityTemplatesDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Browse Community Templates (Access Others' Templates)", fontWeight = FontWeight.SemiBold)
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showSetupWizard = true }, modifier = Modifier.weight(1f)) {
                        Text("Setup wizard")
                    }
                    OutlinedButton(
                        onClick = {
                            saveTemplateTitle = "$college $term".trim()
                            showSaveTemplateDialog = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save locally")
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                val template = AcademicTemplateCodec.export(college, program, department, regulation, academicYear, term, rules, subjects)
                                AcademicFileSharing.shareTextFile(context, "clg1-college-template.clg1template", "application/json", template, "Share grading template with classmates")
                            }.onFailure { message = "Template export failed: ${it.message}" }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Share template")
                    }

                    OutlinedButton(
                        onClick = { showPasteTemplateDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Import text")
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        val activity = context as? Activity
                        if (activity == null) message = "QR scanner is unavailable in this context."
                        else {
                            val intent = IntentIntegrator(activity).setDesiredBarcodeFormats(IntentIntegrator.QR_CODE).setPrompt("Scan a Clg 1% academic template").setBeepEnabled(false).createScanIntent()
                            qrScanLauncher.launch(intent)
                        }
                    }, modifier = Modifier.weight(1f)) {
                        Text("Scan QR")
                    }
                    OutlinedButton(onClick = {
                        runCatching {
                            val template = AcademicTemplateCodec.export(college, program, department, regulation, academicYear, term, rules, subjects)
                            val payload = AcademicQrCodec.encode(template)
                            qrBitmap = AcademicQrCodec.generateBitmap(payload)
                        }.onFailure { message = it.message ?: "Could not generate QR" }
                    }, modifier = Modifier.weight(1f)) {
                        Text("Generate QR")
                    }
                }
            }
        }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { showSubjectDialog = true }, modifier = Modifier.weight(1f)) { Text("Add subject") }; OutlinedButton(onClick = { showExamDialog = true }, modifier = Modifier.weight(1f)) { Text("Add exam") } } }
        item {
            Text("Subjects & marks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    if (!isGeneratingHistoricalReport) {
                        isGeneratingHistoricalReport = true
                        scope.launch {
                            try {
                                runCatching {
                                    AcademicHistoryService.archiveTerm(context, prefs.getString("academic_year", "") ?: "", term, subjects.toList(), rules)
                                    val snapshots = Clg1Database.get(context).academicDao().getSnapshots()
                                    historicalSummary = HistoricalAcademicAnalytics.summarize(snapshots)
                                    val entitlementPrefs = context.getSharedPreferences("clg1_entitlements", 0)
                                    val reportCount = entitlementPrefs.getInt("deep_report_count", 0)
                                    val plusActive = PlayEntitlementVerifier.isActive(context)
                                    if (!plusActive && reportCount >= 10) {
                                        message = "Term archived successfully. Your 10 free PDF reports have been used; verify or restore Plus online to generate another PDF."
                                    } else {
                                        val pdf = AcademicPdfReportGenerator.generate(context, snapshots)
                                        AcademicFileSharing.shareFile(context, pdf, "application/pdf", "Share historical academic report")
                                        if (!plusActive) {
                                            entitlementPrefs.edit().putInt("deep_report_count", reportCount + 1).apply()
                                            freeReportCount = reportCount + 1
                                        }
                                    }
                                }.onFailure { message = "Historical report failed: ${it.message}" }
                            } finally {
                                isGeneratingHistoricalReport = false
                            }
                        }
                    }
                }, enabled = !isGeneratingHistoricalReport, modifier = Modifier.weight(1f)) { Text(if (isGeneratingHistoricalReport) "Generating…" else "Archive & PDF report") }
                OutlinedButton(onClick = {
                    scope.launch { runCatching { historicalSummary = HistoricalAcademicAnalytics.summarize(Clg1Database.get(context).academicDao().getSnapshots()) }
                        .onFailure { message = "History unavailable: ${it.message}" } }
                }, modifier = Modifier.weight(1f)) { Text("Refresh history") }
            }
            historicalSummary?.let { summary ->
                Text("History: ${summary.snapshots} archived term(s) · ${summary.trendLabel} · CGPA ${summary.overallCgpa?.let { "%.2f".format(it) } ?: "not enough term data"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                AcademicTrendChart(summary.termTrends)
                Text("Free historical PDF reports remaining: ${(10 - freeReportCount).coerceAtLeast(0)}", style = MaterialTheme.typography.bodySmall)
                summary.riskFlags.take(2).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                if (summary.subjectPerformance.isNotEmpty()) Text("Latest subjects: ${summary.subjectPerformance.take(4).joinToString { "${it.first} ${"%.1f".format(it.second)}%" }}", style = MaterialTheme.typography.bodySmall)
                if (summary.gradeDistribution.isNotEmpty()) Text("Latest term grades: ${summary.gradeDistribution.entries.joinToString { "${it.key}: ${it.value}" }}", style = MaterialTheme.typography.bodySmall)
                if (summary.historicalGradeDistribution.isNotEmpty()) Text("All-term grade distribution: ${summary.historicalGradeDistribution.entries.joinToString { "${it.key}: ${it.value}" }}", style = MaterialTheme.typography.bodySmall)
                if (summary.assessmentCategoryAverages.isNotEmpty()) Text("Latest assessment averages: ${summary.assessmentCategoryAverages.entries.joinToString { "${it.key}: ${"%.1f".format(it.value)}%" }}", style = MaterialTheme.typography.bodySmall)
                if (summary.historicalAssessmentCategoryAverages.isNotEmpty()) Text("Historical assessment averages: ${summary.historicalAssessmentCategoryAverages.entries.joinToString { "${it.key}: ${"%.1f".format(it.value)}%" }}", style = MaterialTheme.typography.bodySmall)
                if (summary.subjectTrendDeltas.isNotEmpty()) Text("Subject trends: ${summary.subjectTrendDeltas.entries.joinToString { "${it.key} ${if (it.value >= 0) "+" else ""}${"%.1f".format(it.value)} pp" }}", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (subjects.isEmpty()) item { Text("No subjects yet. Add a subject and enter its assessment marks.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(subjects, key = { it.id }) { subject ->
            val result = AcademicRulesEngine.calculate(subject, rules)
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = selectedExportIds[subject.id] ?: true, onCheckedChange = { selectedExportIds[subject.id] = it })
                        Text("Include in DataVis · ${subject.code} · ${subject.name}", fontWeight = FontWeight.SemiBold)
                    }
                    Text("Credits: ${subject.credits} · ${result.percentage?.let { "%.2f%%".format(it) } ?: "Awaiting marks"} · ${if (result.provisional) "Projected grade ${result.grade ?: "—"}" else "Grade ${result.grade ?: "—"}"} · GP ${if (result.provisional) "provisional" else result.gradePoint?.let { "%.2f".format(it) } ?: "—"}")
                    Text(result.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val componentTotals = listOf("Internal" to result.internalPercent, "Practical" to result.practicalPercent, "External" to result.externalPercent).filter { it.second != null }
                    if (componentTotals.isNotEmpty()) Text(componentTotals.joinToString(" · ") { "${it.first}: ${"%.1f".format(it.second!!)}%" }, style = MaterialTheme.typography.bodySmall)
                    Text(if (result.provisional) "PROVISIONAL" else if (result.passed == true) "PASS" else "BELOW PASS TARGET", color = if (result.passed == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                    result.contributions.forEach { contribution ->
                        Text("${contribution.name}: ${"%.2f".format(contribution.normalizedPercent)}% × ${"%.1f".format(contribution.appliedWeightPercent)}% = ${"%.2f".format(contribution.weightedPoints)} points · ${contribution.note}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    subject.assessments.forEachIndexed { assessmentIndex, assessment ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${assessment.name}: ${assessment.obtained?.toString() ?: assessment.status.name} / ${assessment.maximum} · ${assessment.weightPercent}%", style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = {
                                selectedSubjectId = subject.id; editingAssessmentIndex = assessmentIndex
                                componentName = assessment.name; componentObtained = assessment.obtained?.toString().orEmpty()
                                componentMaximum = assessment.maximum.toString(); componentWeight = assessment.weightPercent.toString()
                                componentMinimumPass = assessment.minimumPassPercent?.toString().orEmpty(); componentStatus = assessment.status; showAssessmentDialog = true
                            }) { Text("Edit") }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { selectedSubjectId = subject.id; editingAssessmentIndex = null; componentName = ""; componentObtained = ""; componentMaximum = "25"; componentWeight = "0"; componentMinimumPass = ""; componentStatus = MarkStatus.NOT_CONDUCTED; showAssessmentDialog = true }) { Text("Add assessment") }
                        TextButton(onClick = { subjects.remove(subject); save() }) { Text("Remove subject") }
                    }
                    if (rules.combinationMethod == CombinationMethod.WEIGHTED_AVERAGE || rules.combinationMethod == CombinationMethod.THEORY_PRACTICAL) {
                        val included = subject.assessments.filter { it.status != MarkStatus.EXEMPTED && (it.status != MarkStatus.ABSENT || rules.missingMarkPolicy == MissingMarkPolicy.COUNT_ABSENT_AS_ZERO) }
                        val totalIncludedWeight = included.sumOf { it.weightPercent }
                        val currentPoints = if (totalIncludedWeight > 0.0) included.filter { it.status == MarkStatus.RECORDED && it.obtained != null }
                            .sumOf { (it.obtained!! / it.maximum * 100.0) * it.weightPercent } / totalIncludedWeight else 0.0
                        val remainingWeight = included.filter { it.status == MarkStatus.NOT_CONDUCTED || (it.status == MarkStatus.RECORDED && it.obtained == null) }.sumOf { it.weightPercent }
                        val remainingWeightPercent = if (totalIncludedWeight > 0.0) remainingWeight / totalIncludedWeight * 100.0 else 0.0
                        val hasUnrecorded = subject.assessments.any { it.status != MarkStatus.RECORDED || it.obtained == null }
                        val target = if (rules.missingMarkPolicy == MissingMarkPolicy.REQUIRE_ALL_RECORDED && hasUnrecorded) null
                            else targetPercent.toDoubleOrNull()?.let { AcademicAnalytics.targetOnRemainingWeight(currentPoints, remainingWeightPercent, it) }
                        if (target != null) Text(target.explanation, style = MaterialTheme.typography.bodySmall, color = if (target.feasible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                    } else Text("Target-percentage projection is available for weighted schemes. Current method: ${rules.combinationMethod.name}.", style = MaterialTheme.typography.bodySmall)

                    val hypotheticalIndex = subject.assessments.indexOfFirst { it.status == MarkStatus.NOT_CONDUCTED || (it.status == MarkStatus.RECORDED && it.obtained == null) }
                        .takeIf { it >= 0 } ?: subject.assessments.indexOfLast { it.status != MarkStatus.EXEMPTED }
                    if (hypotheticalIndex >= 0) {
                        val hypothetical = subject.assessments[hypotheticalIndex]
                        val whatIfKey = "${subject.id}:$hypotheticalIndex"
                        val input = whatIfInputs[whatIfKey] ?: ""
                        Text("What-if · ${hypothetical.name}", fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(input, { whatIfInputs[whatIfKey] = it }, Modifier.fillMaxWidth(), label = { Text("Hypothetical marks / ${hypothetical.maximum}") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                        val whatIf = input.toDoubleOrNull()?.let { AcademicAnalytics.whatIf(subject, rules, hypotheticalIndex, it) }
                        if (whatIf != null) Text("Scenario result: ${"%.2f".format(whatIf.projectedPercent)}% · actual marks unchanged", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        val scenarios = AcademicAnalytics.scenarios(subject, rules, hypotheticalIndex)
                        if (scenarios.isNotEmpty()) Text(scenarios.joinToString(" · ") { "${it.label.substringBefore(' ')} ${"%.1f".format(it.projectedPercent)}%" }, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item {
            val sgpa = AcademicRulesEngine.sgpa(subjects.toList(), rules)
            val targetSgpa = targetSgpaText.toDoubleOrNull()?.let { AcademicAnalytics.targetSgpa(subjects.toList(), rules, it) }
            Card { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Semester overview", fontWeight = FontWeight.Bold)
                Text("Subjects: ${subjects.size} · Exams: ${exams.size}")
                Text("SGPA: ${sgpa?.let { "%.2f".format(it) } ?: "Not enough verified data"}")
                if (targetSgpa != null) Text(targetSgpa.explanation, style = MaterialTheme.typography.bodySmall, color = if (targetSgpa.feasible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                Text("Only final subjects with valid credits and configured grade points contribute to current SGPA. Target SGPA estimates the average grade point needed across provisional subjects.", style = MaterialTheme.typography.bodySmall)
            } }
        }
        item { Text("Exam calendar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Checkbox(checked = remindersEnabled, onCheckedChange = { enabled ->
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        remindersEnabled = enabled
                        prefs.edit().putBoolean("exam_reminders_enabled", enabled).apply()
                        if (enabled) exams.forEach { scheduleExamReminder(context, it) }
                        else exams.forEach { WorkManager.getInstance(context).cancelUniqueWork(examWorkName(it)) }
                    }
                })
                Column(Modifier.padding(top = 10.dp)) {
                    Text("Exam reminders", fontWeight = FontWeight.Medium)
                    Text("Optional local notification, scheduled one day before each exam.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (exams.isEmpty()) item { Text("No exams scheduled yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(exams.sortedBy { it.dateMillis }) { exam ->
            val days = ((exam.dateMillis - System.currentTimeMillis()) / 86_400_000L)
            Card { Column(Modifier.padding(14.dp)) { Text(exam.title, fontWeight = FontWeight.SemiBold); Text("${exam.subject} · ${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(exam.dateMillis))}"); Text(if (days >= 0) "$days days remaining" else "${-days} days ago"); if (exam.notes.isNotBlank()) Text(exam.notes, style = MaterialTheme.typography.bodySmall); TextButton(onClick = { exams.remove(exam); WorkManager.getInstance(context).cancelUniqueWork(examWorkName(exam)); save() }) { Text("Remove") } } }
        }
        item {
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Text("DataVis Analytics & Academic Export", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Export your academic dataset to a standardized CSV format, then use DataVis on Streamlit for deep cohort analysis and performance projections.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(8.dp))

            // Prominent DataVis Action Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Insights, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("DataVis Web App Integration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "1. Tap 'Export Academic Data' to save or share your academic CSV.\n2. Tap 'Open DataVis' to open https://datavis-v5.streamlit.app/ and upload your CSV for interactive grade distributions and trend analysis.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val selectedSubjects = if (subjects.any { selectedExportIds[it.id] == true }) subjects.filter { selectedExportIds[it.id] ?: true } else subjects
                                val activeTermId = runCatching {
                                    val periodArray = JSONObject(prefs.getString("academic_profile_v1", "{}") ?: "{}").optJSONArray("periods")
                                    (0 until (periodArray?.length() ?: 0)).map { periodArray!!.getJSONObject(it) }
                                        .firstOrNull { it.optString("name") == term }?.optString("id")
                                }.getOrNull().orEmpty().ifBlank { "term-${term.trim().lowercase().replace(Regex("\\s+"), "-")}" }

                                val csv = buildString {
                                    appendLine("schema_version,record_type,academic_year,term_id,term_name,subject_code,subject_name,credits,component_type,component_name,attempt_number,marks_obtained,marks_max,weight_percent,normalized_score,result_status,letter_grade,grade_point,attendance_percent,attendance_present,attendance_total,data_source,record_date")
                                    selectedSubjects.forEach { subject ->
                                        val result = AcademicRulesEngine.calculate(subject, rules)
                                        subject.assessments.forEach { mark ->
                                            val normalized = mark.obtained?.takeIf { mark.maximum > 0.0 }?.let { it / mark.maximum * 100.0 } ?: ""
                                            val recordDate = mark.dateEpochMillis?.let { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ROOT).format(Date(it)) }.orEmpty()
                                            appendLine("1,assessment,\"${academicYear.csv()}\",\"${activeTermId.csv()}\",\"${term.csv()}\",\"${subject.code.csv()}\",\"${subject.name.csv()}\",${subject.credits},\"${mark.category.csv()}\",\"${mark.name.csv()}\",1,${mark.obtained ?: ""},${mark.maximum},${mark.weightPercent},$normalized,${mark.status.name},\"${(result.grade ?: "").csv()}\",${result.gradePoint ?: ""},,,,manual,\"${recordDate.csv()}\"")
                                        }
                                    }
                                }

                                exportedCsvString = csv
                                val safeTermName = term.trim().lowercase().replace(Regex("[^a-zA-Z0-9]"), "_").ifBlank { "term" }
                                val fileName = "academic_data_${safeTermName}.csv"
                                runCatching {
                                    AcademicFileSharing.shareTextFile(context, fileName, "text/csv", csv, "Export Academic CSV for DataVis")
                                }.onFailure { message = "Export sharing failed: ${it.message}" }
                                showExportCsvSuccessDialog = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Export CSV")
                        }

                        FilledTonalButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://datavis-v5.streamlit.app/")))
                                }.onFailure { message = "Could not open DataVis: ${it.message}" }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Open DataVis")
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Grading Templates & Backups", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedButton(
                onClick = {
                    runCatching {
                        val template = AcademicTemplateCodec.export(
                            college = college, program = program, department = department, regulation = regulation, academicYear = academicYear,
                            term = term, rules = rules, subjects = subjects.toList(), includeSubjectStructure = true
                        )
                        AcademicFileSharing.shareTextFile(context, "clg1-college-template.clg1template", "application/json", template, "Share college template")
                    }.onFailure { message = "Template export failed: ${it.message}" }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Export grading template (.clg1template)")
            }
            OutlinedButton(onClick = { templateImportLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) }, modifier = Modifier.fillMaxWidth()) {
                Text("Import template file from device")
            }
            OutlinedButton(onClick = { backupMode = "export"; backupPassphrase = ""; showBackupPassphraseDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Create encrypted academic backup")
            }
            OutlinedButton(onClick = { openBackupLauncher.launch(arrayOf("application/octet-stream", "application/x-clg1backup", "*/*")) }, modifier = Modifier.fillMaxWidth()) {
                Text("Restore encrypted academic backup")
            }
            OutlinedButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Delete my academic data")
            }

            Spacer(Modifier.height(14.dp))

            if (message.isNotBlank()) Text(message)
        }
        item { Spacer(Modifier.height(20.dp)) }
    }

    if (showSubjectDialog) AlertDialog(onDismissRequest = { showSubjectDialog = false }, title = { Text("Add subject") }, text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(subjectName, { subjectName = it }, label = { Text("Subject name") }, singleLine = true)
        OutlinedTextField(subjectCode, { subjectCode = it }, label = { Text("Subject code") }, singleLine = true)
        OutlinedTextField(credits, { credits = it }, label = { Text("Credits") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
        OutlinedTextField(subjectPassPercent, { subjectPassPercent = it }, label = { Text("Subject-specific pass threshold (%) — optional") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
        if (defaultComponents.isEmpty()) {
            OutlinedTextField(assessmentName, { assessmentName = it }, label = { Text("Assessment name") }, singleLine = true)
            OutlinedTextField(obtained, { obtained = it }, label = { Text("Marks obtained (blank = not entered)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
            OutlinedTextField(maximum, { maximum = it }, label = { Text("Maximum marks") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
            OutlinedTextField(weight, { weight = it }, label = { Text("Weight (%)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
        } else Text("The ${defaultComponents.size} assessment components from your academic setup will be attached to this subject. You can override them after creating it.", style = MaterialTheme.typography.bodySmall)
    } }, confirmButton = { TextButton(onClick = {
        val max = maximum.toDoubleOrNull(); val w = weight.toDoubleOrNull(); val c = credits.toDoubleOrNull(); val mark = obtained.takeIf { it.isNotBlank() }?.toDoubleOrNull()
        val subjectPass = subjectPassPercent.takeIf { it.isNotBlank() }?.toDoubleOrNull()
        val duplicate = subjects.any { it.code.equals(subjectCode.trim(), true) || it.name.equals(subjectName.trim(), true) }
        if (subjects.size >= 300) { message = "You can add up to 300 subjects."
        } else if (subjectName.isBlank() || c == null || c < 0 || duplicate || (subjectPassPercent.isNotBlank() && (subjectPass == null || subjectPass !in 0.0..100.0)) || (defaultComponents.isEmpty() && (max == null || max <= 0 || w == null || w !in 0.0..100.0 || (obtained.isNotBlank() && mark == null) || (mark != null && (mark < 0 || mark > (max ?: 0.0)))))) { message = "Check subject details, unique code/name, credits and optional pass threshold." }
        else {
            val initialMarks = if (defaultComponents.isNotEmpty()) defaultComponents.map { component ->
                AssessmentMark(component.name, null, component.maximumMarks, component.weightPercent, MarkStatus.NOT_CONDUCTED, component.minimumPassPercent, component.category)
            } else listOf(AssessmentMark(assessmentName.ifBlank { "Assessment" }, mark, max ?: 100.0, w ?: 100.0, if (mark == null) MarkStatus.NOT_CONDUCTED else MarkStatus.RECORDED))
            val s = AcademicSubject(UUID.randomUUID().toString(), subjectCode.trim(), subjectName.trim(), c, initialMarks, subjectPass)
            val errors = AcademicRulesEngine.calculate(s, rules).errors
            if (errors.isEmpty()) { subjects.add(s); save(); showSubjectDialog = false; subjectName = ""; subjectCode = ""; obtained = ""; subjectPassPercent = "" }
            else message = errors.joinToString("; ")
        }
    } ) { Text("Save") } }, dismissButton = { TextButton(onClick = { showSubjectDialog = false }) { Text("Cancel") } })

    if (showAssessmentDialog) AlertDialog(
        onDismissRequest = { showAssessmentDialog = false },
        title = { Text(if (editingAssessmentIndex == null) "Add assessment component" else "Edit assessment component") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(componentName, { componentName = it }, label = { Text("Component name") }, singleLine = true)
            OutlinedTextField(componentObtained, { componentObtained = it }, label = { Text("Marks obtained (blank = not conducted)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
            OutlinedTextField(componentMaximum, { componentMaximum = it }, label = { Text("Maximum marks") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
            OutlinedTextField(componentWeight, { componentWeight = it }, label = { Text("Contribution weight (%)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
            OutlinedTextField(componentMinimumPass, { componentMinimumPass = it }, label = { Text("Minimum pass (%) — optional") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
            Text("Status: ${componentStatus.name}", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { componentStatus = MarkStatus.NOT_CONDUCTED }) { Text("Pending") }
                TextButton(onClick = { componentStatus = MarkStatus.ABSENT }) { Text("Absent") }
                TextButton(onClick = { componentStatus = MarkStatus.EXEMPTED }) { Text("Exempt") }
            }
        } },
        confirmButton = { TextButton(onClick = {
            val subjectIndex = subjects.indexOfFirst { it.id == selectedSubjectId }
            val maximumValue = componentMaximum.toDoubleOrNull(); val weightValue = componentWeight.toDoubleOrNull()
            val markValue = componentObtained.takeIf { it.isNotBlank() }?.toDoubleOrNull()
            val minimumValue = componentMinimumPass.takeIf { it.isNotBlank() }?.toDoubleOrNull()
            if (subjectIndex < 0 || componentName.isBlank() || maximumValue == null || maximumValue <= 0.0 || weightValue == null || weightValue !in 0.0..100.0 || (componentObtained.isNotBlank() && markValue == null) || (componentMinimumPass.isNotBlank() && minimumValue == null) || (markValue != null && markValue !in 0.0..maximumValue) || (minimumValue != null && minimumValue !in 0.0..100.0)) {
                message = "Check the component name, marks, maximum, weight and optional pass threshold."
            } else {
                val existing = subjects[subjectIndex]
                val updatedMarks = existing.assessments.toMutableList()
                val chosenStatus = if (markValue != null) MarkStatus.RECORDED else componentStatus
                val mark = AssessmentMark(componentName, markValue, maximumValue, weightValue, chosenStatus, minimumValue)
                val editIndex = editingAssessmentIndex
                if (editIndex == null) updatedMarks.add(mark) else if (editIndex in updatedMarks.indices) updatedMarks[editIndex] = mark
                val updatedSubject = existing.copy(assessments = updatedMarks)
                val errors = AcademicRulesEngine.validate(updatedSubject)
                if (errors.isEmpty()) { subjects[subjectIndex] = updatedSubject; save(); showAssessmentDialog = false }
                else message = errors.joinToString("; ")
            }
        }) { Text("Save component") } },
        dismissButton = { TextButton(onClick = { showAssessmentDialog = false }) { Text("Cancel") } }
    )

    if (showExamDialog) AlertDialog(onDismissRequest = { showExamDialog = false }, title = { Text("Add exam") }, text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(examTitle, { examTitle = it }, label = { Text("Exam title") }, singleLine = true)
        OutlinedTextField(examSubject, { examSubject = it }, label = { Text("Subject") }, singleLine = true)
        OutlinedTextField(examDate, { examDate = it }, label = { Text("Date (dd/MM/yyyy)") }, singleLine = true)
        OutlinedTextField(examTime, { examTime = it }, label = { Text("Time (HH:mm, 24-hour)") }, singleLine = true)
        OutlinedTextField(examNotes, { examNotes = it }, label = { Text("Notes / location") }, singleLine = true)
    } }, confirmButton = { TextButton(onClick = {
        val date = runCatching { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).apply { isLenient = false }.parse("$examDate $examTime")?.time }.getOrNull()
        if (exams.size >= 1000) message = "The academic calendar supports up to 1,000 exam events."
        else if (examTitle.isBlank() || examSubject.isBlank() || date == null) message = "Enter an exam title, subject, valid date and time (dd/MM/yyyy, HH:mm)."
        else { val newExam = LocalExam(examTitle, examSubject, date, examNotes); exams.add(newExam); if (remindersEnabled) scheduleExamReminder(context, newExam); save(); showExamDialog = false; examTitle = ""; examSubject = ""; examDate = ""; examTime = "09:00"; examNotes = "" }
    }) { Text("Save") } }, dismissButton = { TextButton(onClick = { showExamDialog = false }) { Text("Cancel") } })

    if (showBackupPassphraseDialog) AlertDialog(
        onDismissRequest = { showBackupPassphraseDialog = false; backupPassphrase = ""; pendingRestoreBytes = null },
        title = { Text(if (backupMode == "export") "Protect your backup" else "Unlock your backup") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (backupMode == "export") "Choose a passphrase of at least 10 characters. You will need it to restore this file. It is not stored by Clg 1%." else "Enter the passphrase used when this backup was created.")
            OutlinedTextField(backupPassphrase, { backupPassphrase = it }, label = { Text("Backup passphrase") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
        } },
        confirmButton = { TextButton(onClick = {
            if (backupPassphrase.length < 10) message = "Passphrase must contain at least 10 characters."
            else if (backupMode == "export") {
                val chosenPassphrase = backupPassphrase
                backupPassphrase = ""
                save()
                roomSaveJob?.cancel()
                showBackupPassphraseDialog = false
                message = "Preparing encrypted backup…"
                scope.launch {
                    val encrypted = runCatching {
                        withContext(Dispatchers.IO) {
                            val recordsText = prefs.getString("academic_records_v1", "{}") ?: "{}"
                            AcademicRoomRepository.persistFromLegacyJson(context, recordsText, college, term, academicYear)
                            val history = AcademicRoomRepository.historyToJson(context)
                            val snapshot = JSONObject().put("schema_version", 2)
                                .put("academic_records", recordsText)
                                .put("academic_profile", prefs.getString("academic_profile_v1", "{}") ?: "{}")
                                .put("academic_profile_full", prefs.getString("academic_profile_full_v1", "") ?: "")
                                .put("history", history)
                                .put("college", college).put("term", term).put("pass", passMark).toString()
                            val chars = chosenPassphrase.toCharArray()
                            try { EncryptedAcademicBackup.encrypt(snapshot, chars) } finally { chars.fill('\u0000') }
                        }
                    }
                    encrypted.onSuccess { pendingBackupBytes = it; message = "Backup encrypted. Choose where to save it."; createBackupLauncher.launch("clg1-academic-backup.clg1backup") }
                        .onFailure { message = "Backup encryption failed: ${it.message}" }
                }
            } else {
                runCatching {
                    val bytes = pendingRestoreBytes ?: error("Backup file is missing")
                    val chars = backupPassphrase.toCharArray()
                    val plain = try { EncryptedAcademicBackup.decrypt(bytes, chars) } finally { chars.fill('\u0000') }
                    val root = JSONObject(plain)
                    require(root.optInt("schema_version", -1) in 1..2) { "Unsupported backup version" }
                    val records = JSONObject(root.optString("academic_records", "{}"))
                    val profileMetadata = JSONObject(root.optString("academic_profile", "{}"))
                    AcademicRoomRepository.validateProfileMetadata(profileMetadata)
                    val history = root.optJSONArray("history") ?: JSONArray()
                    AcademicRoomRepository.parseHistoryJson(history)
                    require((records.optJSONArray("subjects")?.length() ?: 0) <= 500) { "Backup contains too many subjects" }
                    require((records.optJSONArray("exams")?.length() ?: 0) <= 2000) { "Backup contains too many exams" }
                    root
                }.onSuccess { pendingRestoreJson = it; showBackupPassphraseDialog = false; showRestoreConfirm = true }
                    .onFailure { message = "Backup restore rejected: ${it.message}" }
            }
            backupPassphrase = ""
        }) { Text(if (backupMode == "export") "Create backup" else "Unlock backup") } },
        dismissButton = { TextButton(onClick = { showBackupPassphraseDialog = false; backupPassphrase = ""; pendingRestoreBytes = null }) { Text("Cancel") } }
    )

    if (showRestoreConfirm) AlertDialog(
        onDismissRequest = { showRestoreConfirm = false; pendingRestoreJson = null },
        title = { Text("Restore academic backup?") },
        text = { Text("This replaces the academic workspace subjects, marks, exams and academic setup on this device. It does not change ERP login credentials or attendance data. Continue only if this is the backup you intend to restore.") },
        confirmButton = { TextButton(onClick = {
            runCatching {
                val root = pendingRestoreJson ?: error("Backup content is unavailable")
                val recordsText = root.optString("academic_records", "{}")
                val records = JSONObject(recordsText)
                val subArray = records.optJSONArray("subjects") ?: JSONArray()
                require(subArray.length() <= 300) { "Backup contains too many subjects" }
                val restoredSubjects = (0 until subArray.length()).map { i ->
                    val obj = subArray.getJSONObject(i); val markArray = obj.optJSONArray("assessments") ?: JSONArray()
                    val marks = (0 until markArray.length()).map { j ->
                        val m = markArray.getJSONObject(j)
                        val markValue = if (m.isNull("obtained")) null else m.optDouble("obtained")
                        val fallbackStatus = if (markValue == null) MarkStatus.NOT_CONDUCTED else MarkStatus.RECORDED
                        val status = runCatching { MarkStatus.valueOf(m.optString("status")) }.getOrDefault(fallbackStatus)
                        AssessmentMark(m.optString("name"), markValue, m.optDouble("maximum", 100.0), m.optDouble("weight", 100.0), status, if (m.isNull("minimum_pass")) null else m.optDouble("minimum_pass"), m.optString("category", "assessment"), if (m.isNull("date")) null else m.optLong("date"))
                    }
                    AcademicSubject(obj.optString("id", UUID.randomUUID().toString()), obj.optString("code"), obj.optString("name"), obj.optDouble("credits"), marks, if (obj.isNull("subject_pass_percent")) null else obj.optDouble("subject_pass_percent"))
                }
                restoredSubjects.forEach { val errors = AcademicRulesEngine.validate(it); require(errors.isEmpty()) { errors.joinToString("; ") } }
                require(restoredSubjects.map { it.id }.distinct().size == restoredSubjects.size) { "Backup contains duplicate subject IDs" }
                require(restoredSubjects.map { it.name.trim().lowercase(Locale.ROOT) }.distinct().size == restoredSubjects.size) { "Backup contains duplicate subject names" }
                val gradeBandsFromBackup = (0 until (records.optJSONArray("grade_bands")?.length() ?: 0)).map { index -> records.getJSONArray("grade_bands").getJSONObject(index).let { GradeBand(it.optDouble("minimum"), it.optString("label"), it.optDouble("point"), it.optBoolean("inclusive", true)) } }
                val combinationFromBackup = runCatching { CombinationMethod.valueOf(records.optString("combination_method")) }.getOrElse { throw IllegalArgumentException("Invalid combination method in backup") }
                val missingFromBackup = runCatching { MissingMarkPolicy.valueOf(records.optString("missing_mark_policy")) }.getOrElse { throw IllegalArgumentException("Invalid missing-mark policy in backup") }
                val rulesFromBackup = AcademicRules(root.optString("college"), root.optString("term", "Semester 1"), gradeBandsFromBackup, root.optString("pass", "40").toDoubleOrNull() ?: Double.NaN, 2, combinationFromBackup, missingFromBackup, records.optInt("best_n", 1))
                val ruleErrors = AcademicRulesEngine.validate(rulesFromBackup)
                require(ruleErrors.isEmpty()) { ruleErrors.joinToString("; ") }
                restoredSubjects.forEach { val result = AcademicRulesEngine.calculate(it, rulesFromBackup); require(result.errors.isEmpty()) { result.errors.joinToString("; ") } }
                val examArray = records.optJSONArray("exams") ?: JSONArray()
                require(examArray.length() <= 1000) { "Backup contains too many exam events" }
                val restoredExams = (0 until examArray.length()).map { i -> examArray.getJSONObject(i).let { LocalExam(it.optString("title"), it.optString("subject"), it.optLong("date"), it.optString("notes")) } }
                require(restoredExams.all { it.title.isNotBlank() && it.subject.isNotBlank() && it.dateMillis > 0L }) { "Backup contains an invalid exam event" }
                val profileMetadataText = root.optString("academic_profile", "{}")
                prefs.edit().putString("academic_records_v1", recordsText).putString("college", root.optString("college"))
                    .putString("term", root.optString("term", "Semester 1")).putString("pass", root.optString("pass", "40"))
                    .putString("academic_profile_v1", profileMetadataText).putString("academic_profile_full_v1", root.optString("academic_profile_full", ""))
                    .putString("academic_year", JSONObject(profileMetadataText).optString("academic_year")).apply()
                subjects.clear(); subjects.addAll(restoredSubjects)
                exams.clear(); exams.addAll(restoredExams)
                WorkManager.getInstance(context).cancelAllWorkByTag("clg1_exam_reminder")
                if (remindersEnabled) restoredExams.forEach { scheduleExamReminder(context, it) }
                college = root.optString("college"); term = root.optString("term", "Semester 1"); passMark = root.optString("pass", "40")
                val bands = records.optJSONArray("grade_bands")
                if (bands != null && bands.length() in 1..30) gradeBands = (0 until bands.length()).map { i -> bands.getJSONObject(i).let { GradeBand(it.optDouble("minimum"), it.optString("label"), it.optDouble("point"), it.optBoolean("inclusive", true)) } }
                combinationMethod = runCatching { CombinationMethod.valueOf(records.optString("combination_method")) }.getOrDefault(CombinationMethod.WEIGHTED_AVERAGE)
                missingMarkPolicy = runCatching { MissingMarkPolicy.valueOf(records.optString("missing_mark_policy")) }.getOrDefault(MissingMarkPolicy.EXCLUDE_AND_RENORMALIZE)
                bestN = records.optInt("best_n", 1).coerceAtLeast(1)
                val metadata = JSONObject(profileMetadataText)
                AcademicRoomRepository.validateProfileMetadata(metadata)
                university = metadata.optString("university"); program = metadata.optString("program")
                department = metadata.optString("department"); regulation = metadata.optString("regulation")
                academicYear = metadata.optString("academic_year")
                val componentArray = metadata.optJSONArray("assessment_components")
                if (componentArray != null) defaultComponents = (0 until componentArray.length()).map { i -> componentArray.getJSONObject(i).let { AssessmentComponentDefinition(it.optString("id"), it.optString("name"), it.optString("category"), it.optDouble("maximum"), it.optDouble("weight"), if (it.isNull("minimum_pass")) null else it.optDouble("minimum_pass"), it.optBoolean("required", true)) } }
                val historyToRestore = root.optJSONArray("history") ?: JSONArray()
                scope.launch {
                    runCatching {
                        AcademicRoomRepository.persistFromLegacyJson(context, recordsText, college, term, academicYear)
                        if (root.optInt("schema_version", 1) >= 2) AcademicRoomRepository.restoreHistory(context, historyToRestore)
                        historicalSummary = HistoricalAcademicAnalytics.summarize(Clg1Database.get(context).academicDao().getSnapshots())
                    }.onSuccess { message = "Academic backup and historical archive restored." }
                        .onFailure { message = "Backup is restored in legacy local storage; Room sync will retry: ${it.message}" }
                }
            }.onSuccess { message = "Academic backup restored locally; syncing the database…" }.onFailure { message = "Restore failed; existing data was retained: ${it.message}" }
            showRestoreConfirm = false; pendingRestoreJson = null; pendingRestoreBytes = null
        }) { Text("Restore and replace") } },
        dismissButton = { TextButton(onClick = { showRestoreConfirm = false; pendingRestoreJson = null }) { Text("Cancel") } }
    )

    pendingCsv?.let { csv ->
        AlertDialog(
            onDismissRequest = { pendingCsv = null },
            title = { Text("Review DataVis CSV export") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("This file contains $pendingCsvSubjectCount selected subject(s) and $pendingCsvRowCount assessment row(s) for $term. It includes subject codes/names, credits, assessment names, marks, maxima, weights, status and calculated grade fields. Attendance fields are blank in this release.")
                Text("It excludes student identity, ERP credentials, attendance portal cookies and device identifiers. Nothing is transferred until you choose an action below. Seamless transfer stores the CSV temporarily for up to 10 minutes and DataVis can retrieve it once.", style = MaterialTheme.typography.bodySmall)
                Text("CSV preview", fontWeight = FontWeight.SemiBold)
                Text(csv.lines().take(4).joinToString("\n").take(1200), style = MaterialTheme.typography.labelSmall, maxLines = 8, overflow = TextOverflow.Ellipsis)
            } },
            confirmButton = { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (DataVisTransferClient.isConfigured()) TextButton(enabled = !isTransferringCsv, onClick = {
                    if (!isTransferringCsv) {
                        isTransferringCsv = true
                        val selectedCsv = csv
                        scope.launch(Dispatchers.IO) {
                            val outcome = runCatching { DataVisTransferClient.uploadAndOpen(context, selectedCsv) }
                            withContext(Dispatchers.Main) {
                                message = outcome.getOrElse { "DataVis transfer failed: ${it.message}. You can still share the CSV manually." }
                                if (outcome.isSuccess) pendingCsv = null
                                isTransferringCsv = false
                            }
                        }
                    }
                }) { Text(if (isTransferringCsv) "Transferring…" else "Transfer once to DataVis") }
                TextButton(onClick = {
                    runCatching { AcademicFileSharing.shareTextFile(context, "clg1-academic-export.csv", "text/csv", csv, "Share academic CSV") }
                        .onFailure { message = "CSV export failed: ${it.message}" }
                    pendingCsv = null
                }) { Text("Share CSV manually") }
                TextButton(onClick = {
                    AcademicFileSharing.saveTextFileToDownloads(context, "clg1-academic-export.csv", "text/csv", csv)
                        .onSuccess { message = it }
                        .onFailure { message = "Download failed: ${it.message}" }
                    pendingCsv = null
                }) { Text("Download CSV") }
            } },
            dismissButton = { TextButton(onClick = { pendingCsv = null }) { Text("Cancel") } }
        )
    }

    if (showSetupWizard) {
        AcademicSetupWizard(
            initialCollege = college,
            initialTerm = term,
            initialPassingPercent = passMark.toDoubleOrNull() ?: 40.0,
            initialProfile = runCatching { AcademicProfileCodec.decode(prefs.getString("academic_setup_draft", prefs.getString("academic_profile_full_v1", "")) ?: "", validateProfile = false) }.getOrNull(),
            onDismiss = { showSetupWizard = false },
            onSaveDraft = { draft ->
                runCatching { AcademicProfileCodec.encode(draft) }
                    .onSuccess { prefs.edit().putString("academic_setup_draft", it).apply() }
                    .onFailure { message = "Could not save setup draft: ${it.message}" }
            },
            onComplete = { result ->
                prefs.edit().remove("academic_setup_draft").apply()
                college = result.profile.institution
                term = result.profile.periods.firstOrNull()?.name ?: term
                passMark = result.profile.grading.passingPercent.toString()
                combinationMethod = result.combinationMethod
                missingMarkPolicy = result.missingMarkPolicy
                bestN = result.bestN
                university = result.profile.university
                program = result.profile.program
                department = result.profile.department
                regulation = result.profile.regulation
                academicYear = result.profile.academicYear
                gradeBands = result.profile.grading.bands
                defaultComponents = result.profile.defaultScheme.components
                save(result)
                showSetupWizard = false
                message = "Academic setup saved. Confirm the configured rules against your official college handbook."
            }
        )
    }

    qrBitmap?.let { bitmap ->
        AlertDialog(
            onDismissRequest = { qrBitmap = null },
            title = { Text("Shareable academic template QR") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Image(bitmap.asImageBitmap(), contentDescription = "Clg 1% academic template QR code", modifier = Modifier.fillMaxWidth().height(260.dp))
                Text("This QR contains academic structure only. It does not contain marks, attendance, student identity or ERP credentials. If the QR is too dense or fails to scan, share the .clg1template file instead.", style = MaterialTheme.typography.bodySmall)
            } },
            confirmButton = { TextButton(onClick = {
                runCatching {
                    val exportDirectory = File(context.cacheDir, "academic_exports").apply { mkdirs() }
                    val file = File(exportDirectory, "clg1-template-qr.png")
                    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    AcademicFileSharing.shareFile(context, file, "image/png", "Share template QR")
                }.onFailure { message = "QR share failed: ${it.message}" }
                qrBitmap = null
            }) { Text("Share QR image") } },
            dismissButton = { TextButton(onClick = { qrBitmap = null }) { Text("Close") } }
        )
    }

    if (!showTemplateReplaceConfirm) pendingTemplate?.let { template ->
        AlertDialog(
            onDismissRequest = { pendingTemplate = null },
            title = { Text("Review community template") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${template.college.ifBlank { "Unnamed institution" }} · ${template.termName}")
                Text("Programme: ${template.program.ifBlank { "Not specified" }} · Regulation: ${template.regulation.ifBlank { "Not specified" }}")
                Text("Subjects: ${template.subjects.size} · Grade bands: ${template.rules.gradeBands.size}")
                Text("Passing threshold: ${template.rules.passingPercent}% · ${template.rules.combinationMethod}")
                Text("Community template — not institutionally verified. Importing adds structure only; no marks or ERP credentials are included.", style = MaterialTheme.typography.bodySmall)
            } },
            confirmButton = { TextButton(onClick = { applyTemplate(template, replaceExisting = false) }) { Text("Merge template") } },
            dismissButton = { Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { showTemplateReplaceConfirm = true }) { Text("Replace active setup…") }
                TextButton(onClick = { pendingTemplate = null }) { Text("Cancel") }
            } }
        )
    }

    if (showTemplateReplaceConfirm) pendingTemplate?.let { template ->
        AlertDialog(
            onDismissRequest = { showTemplateReplaceConfirm = false },
            title = { Text("Replace active academic setup?") },
            text = { Text("This replaces the current subjects, marks, exams and active grading configuration with the selected community template. Archived historical snapshots, ERP attendance and ERP credentials are retained. Export an encrypted backup first if you need the current active records.") },
            confirmButton = { TextButton(onClick = { applyTemplate(template, replaceExisting = true) }) { Text("Replace active setup") } },
            dismissButton = { TextButton(onClick = { showTemplateReplaceConfirm = false }) { Text("Keep current data") } }
        )
    }

    if (showDeleteConfirm) AlertDialog(
        onDismissRequest = { showDeleteConfirm = false },
        title = { Text("Delete academic data?") },
        text = { Text("This permanently deletes locally stored academic subjects, marks, exams and academic setup from this device. It does not delete attendance ERP data. Export an encrypted backup first if you need one.") },
        confirmButton = { TextButton(onClick = {
            roomSaveJob?.cancel()
            subjects.clear(); exams.clear()
            WorkManager.getInstance(context).cancelAllWorkByTag("clg1_exam_reminder")
            showDeleteConfirm = false
            scope.launch {
                runCatching { AcademicRoomRepository.deleteAll(context) }.onSuccess {
                    prefs.edit().remove("academic_records_v1").remove("academic_profile_v1").remove("academic_profile_full_v1").remove("academic_setup_draft").remove("college").remove("term").remove("pass").remove("academic_year").remove("room_mirror_hash").apply()
                    college = ""; term = "Semester 1"; passMark = "40"; gradeBands = defaultGradeBands; combinationMethod = CombinationMethod.WEIGHTED_AVERAGE; missingMarkPolicy = MissingMarkPolicy.EXCLUDE_AND_RENORMALIZE; bestN = 1
                    university = ""; program = ""; department = ""; regulation = ""; academicYear = ""; defaultComponents = emptyList(); historicalSummary = null
                    message = "Academic records and historical archives deleted from this device. ERP attendance and credentials were not touched."
                }.onFailure { message = "Could not completely delete academic database records: ${it.message}. Legacy local data remains available." }
            }
        }) { Text("Delete permanently") } },
        dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
    )

    if (showCommunityTemplatesDialog) {
        val communityTemplates = remember { TemplateLibrary.getCommunityTemplates() }
        AlertDialog(
            onDismissRequest = { showCommunityTemplatesDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Shared Grading Templates")
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            "Choose from verified institution templates or templates saved locally on your device. All grading criteria, regulations, and subject structures are imported securely.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (localTemplates.isNotEmpty()) {
                        item {
                            Text("My Saved Templates (Stored Locally)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        }
                        items(localTemplates) { saved ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(saved.title, fontWeight = FontWeight.SemiBold)
                                    Text("${saved.college} · ${saved.term}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        TextButton(onClick = {
                                            runCatching { AcademicTemplateCodec.parse(saved.templateJson) }
                                                .onSuccess {
                                                    pendingTemplate = it
                                                    showCommunityTemplatesDialog = false
                                                }
                                                .onFailure { message = "Could not parse template: ${it.message}" }
                                        }) { Text("Apply") }
                                        TextButton(onClick = {
                                            AcademicFileSharing.shareTextFile(context, "clg1-template.clg1template", "application/json", saved.templateJson, "Share saved template")
                                        }) { Text("Share") }
                                        TextButton(onClick = {
                                            AcademicFileSharing.saveTextFileToDownloads(context, "clg1-template.clg1template", "application/json", saved.templateJson)
                                                .onSuccess { message = it }
                                                .onFailure { message = "Download failed: ${it.message}" }
                                        }) { Text("Download") }
                                        TextButton(onClick = {
                                            TemplateLibrary.deleteSavedTemplate(context, saved.id)
                                            localTemplates = TemplateLibrary.getSavedLocalTemplates(context)
                                        }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                                    }
                                }
                            }
                        }
                        item { HorizontalDivider(Modifier.padding(vertical = 4.dp)) }
                    }

                    item {
                        Text("Curated Community Templates", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    }

                    items(communityTemplates) { template ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(template.college, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                                Text("${template.program} ${template.department} · ${template.regulation}", style = MaterialTheme.typography.bodySmall)
                                Text("${template.subjects.size} subjects · ${template.rules.passingPercent}% passing threshold · ${template.rules.gradeBands.size} grade bands", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = {
                                        pendingTemplate = template
                                        showCommunityTemplatesDialog = false
                                    }) { Text("Preview & Apply") }
                                    TextButton(onClick = {
                                        runCatching {
                                            val json = AcademicTemplateCodec.export(
                                                template.college, template.program, template.department,
                                                template.regulation, template.academicYear, template.termName,
                                                template.rules, TemplateLibrary.createSubjectsFromTemplate(template)
                                            )
                                            AcademicFileSharing.shareTextFile(context, "community-template.clg1template", "application/json", json, "Share community template")
                                        }.onFailure { message = "Share failed: ${it.message}" }
                                    }) { Text("Share") }
                                    TextButton(onClick = {
                                        runCatching {
                                            val json = AcademicTemplateCodec.export(
                                                template.college, template.program, template.department,
                                                template.regulation, template.academicYear, template.termName,
                                                template.rules, TemplateLibrary.createSubjectsFromTemplate(template)
                                            )
                                            AcademicFileSharing.saveTextFileToDownloads(context, "community-template.clg1template", "application/json", json)
                                                .onSuccess { message = it }
                                                .onFailure { message = "Download failed: ${it.message}" }
                                        }.onFailure { message = "Download failed: ${it.message}" }
                                    }) { Text("Download") }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCommunityTemplatesDialog = false }) { Text("Close") }
            }
        )
    }

    if (showSaveTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showSaveTemplateDialog = false },
            title = { Text("Save Template Locally") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Store this grading scheme and subject structure on your device so you can reload or share it anytime with other students.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = saveTemplateTitle,
                        onValueChange = { saveTemplateTitle = it },
                        label = { Text("Template name / note") },
                        placeholder = { Text("e.g., $college $term R$regulation") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    runCatching {
                        val json = AcademicTemplateCodec.export(college, program, department, regulation, academicYear, term, rules, subjects.toList(), true)
                        val template = AcademicTemplateCodec.parse(json)
                        val title = saveTemplateTitle.ifBlank { "$college - $term" }
                        TemplateLibrary.saveTemplateLocally(context, title, template, json)
                        localTemplates = TemplateLibrary.getSavedLocalTemplates(context)
                        message = "Template '$title' saved locally to your device."
                        showSaveTemplateDialog = false
                        saveTemplateTitle = ""
                    }.onFailure { message = "Could not save template: ${it.message}" }
                }) { Text("Save Locally") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveTemplateDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showPasteTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showPasteTemplateDialog = false },
            title = { Text("Import Template from Classmate") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Paste the template code or JSON text shared by your friend via WhatsApp, email, or chat.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = pasteTemplateText,
                        onValueChange = { pasteTemplateText = it },
                        label = { Text("Template JSON / Code") },
                        modifier = Modifier.fillMaxWidth().height(160.dp)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = {
                            val clip = clipboardManager.getText()?.text
                            if (!clip.isNullOrBlank()) {
                                pasteTemplateText = clip
                            } else {
                                message = "Clipboard is empty"
                            }
                        }) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Paste Clipboard")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val text = pasteTemplateText.trim()
                        if (text.isBlank()) {
                            message = "Paste template text first"
                        } else {
                            runCatching { AcademicTemplateCodec.parse(text) }
                                .onSuccess {
                                    pendingTemplate = it
                                    showPasteTemplateDialog = false
                                    pasteTemplateText = ""
                                }
                                .onFailure { message = "Invalid template text: ${it.message}" }
                        }
                    }
                ) { Text("Parse & Review") }
            },
            dismissButton = {
                TextButton(onClick = { showPasteTemplateDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showExportCsvSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showExportCsvSuccessDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Academic CSV Exported")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Your academic dataset has been compiled into the standardized format with ${subjects.size} subjects and ${subjects.sumOf { it.assessments.size }} assessments.")
                    Text("Next steps to visualize in DataVis:\n1. Open DataVis web app below\n2. Upload or drag & drop your saved CSV file\n3. Explore comprehensive SGPA projections, percentiles, and subject correlation charts!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://datavis-v5.streamlit.app/")))
                    }
                    showExportCsvSuccessDialog = false
                }) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Open DataVis App")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    clipboardManager.setText(AnnotatedString(exportedCsvString))
                    message = "CSV copied to clipboard!"
                    showExportCsvSuccessDialog = false
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Copy CSV")
                }
            }
        )
    }
}

private fun InputStream.readBytesLimited(maxBytes: Int): ByteArray {
    require(maxBytes > 0)
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    var total = 0
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        total += read
        require(total <= maxBytes) { "Selected file exceeds the ${maxBytes / (1024 * 1024)} MB limit" }
        output.write(buffer, 0, read)
    }
    return output.toByteArray()
}

private fun examWorkName(exam: LocalExam): String = "clg1_exam_${exam.dateMillis}_${exam.title.hashCode()}_${exam.subject.hashCode()}"

private fun scheduleExamReminder(context: android.content.Context, exam: LocalExam) {
    if (exam.dateMillis <= System.currentTimeMillis()) return
    val delay = (exam.dateMillis - TimeUnit.DAYS.toMillis(1) - System.currentTimeMillis()).coerceAtLeast(0L)
    val request = OneTimeWorkRequestBuilder<ExamReminderWorker>()
        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
        .setInputData(Data.Builder().putString(ExamReminderWorker.KEY_TITLE, exam.title)
            .putString(ExamReminderWorker.KEY_SUBJECT, exam.subject)
            .putString(ExamReminderWorker.KEY_EXAM_ID, examWorkName(exam)).build())
        .addTag("clg1_exam_reminder")
        .build()
    WorkManager.getInstance(context).enqueueUniqueWork(examWorkName(exam), ExistingWorkPolicy.REPLACE, request)
}

private fun String.csv(): String = replace("\"", "\"\"")
