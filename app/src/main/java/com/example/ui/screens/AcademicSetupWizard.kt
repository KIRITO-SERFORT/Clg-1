package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AcademicPeriod
import com.example.data.model.AcademicRules
import com.example.data.model.AcademicSubject
import com.example.data.model.AssessmentMark
import com.example.data.model.MarkStatus
import com.example.data.model.AcademicProfile
import com.example.data.model.AcademicCalendarType
import com.example.data.model.AssessmentComponentDefinition
import com.example.data.model.AssessmentScheme
import com.example.data.model.CombinationMethod
import com.example.data.model.GradingScheme
import com.example.data.model.GradeBand
import com.example.data.model.MissingMarkPolicy
import com.example.domain.AcademicSetupValidator
import com.example.domain.AcademicRulesEngine
import com.example.data.template.AcademicProfileCodec
import java.util.UUID

data class AcademicWizardResult(
    val profile: AcademicProfile,
    val combinationMethod: CombinationMethod,
    val missingMarkPolicy: MissingMarkPolicy,
    val bestN: Int
)

/** Four-step setup flow. Draft state remains in this dialog until the user confirms the review step. */
@Composable
fun AcademicSetupWizard(
    initialCollege: String,
    initialTerm: String,
    initialPassingPercent: Double,
    initialProfile: AcademicProfile? = null,
    onDismiss: () -> Unit,
    onSaveDraft: (AcademicProfile) -> Unit,
    onComplete: (AcademicWizardResult) -> Unit
) {
    var step by remember { mutableStateOf(0) }
    var institution by remember(initialProfile) { mutableStateOf(initialProfile?.institution ?: initialCollege) }
    var university by remember(initialProfile) { mutableStateOf(initialProfile?.university ?: "") }
    var program by remember(initialProfile) { mutableStateOf(initialProfile?.program ?: "") }
    var department by remember(initialProfile) { mutableStateOf(initialProfile?.department ?: "") }
    var regulation by remember(initialProfile) { mutableStateOf(initialProfile?.regulation ?: "") }
    var academicYear by remember(initialProfile) { mutableStateOf(initialProfile?.academicYear ?: "") }
    var term by remember(initialProfile) { mutableStateOf(initialProfile?.periods?.firstOrNull()?.name ?: initialTerm) }
    var periodNames by remember(initialProfile) { mutableStateOf(initialProfile?.periods?.map { it.name }?.ifEmpty { listOf(initialTerm) } ?: listOf(initialTerm)) }
    var periodStartDates by remember(initialProfile) { mutableStateOf(initialProfile?.periods?.map { it.startsAtEpochMillis?.let { time -> java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT).format(java.util.Date(time)) }.orEmpty() }?.ifEmpty { listOf("") } ?: listOf("")) }
    var periodEndDates by remember(initialProfile) { mutableStateOf(initialProfile?.periods?.map { it.endsAtEpochMillis?.let { time -> java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT).format(java.util.Date(time)) }.orEmpty() }?.ifEmpty { listOf("") } ?: listOf("")) }
    var calendar by remember(initialProfile) { mutableStateOf(initialProfile?.calendarType ?: AcademicCalendarType.SEMESTER) }
    var gradeBands by remember(initialProfile) { mutableStateOf(initialProfile?.grading?.bands ?: listOf(GradeBand(90.0, "O", 10.0), GradeBand(80.0, "A+", 9.0), GradeBand(70.0, "A", 8.0), GradeBand(60.0, "B+", 7.0), GradeBand(50.0, "B", 6.0), GradeBand(40.0, "C", 5.0), GradeBand(0.0, "F", 0.0))) }
    var components by remember(initialProfile) { mutableStateOf(initialProfile?.defaultScheme?.components ?: listOf(
        AssessmentComponentDefinition(UUID.randomUUID().toString(), "Internal assessment", "internal", 25.0, 40.0),
        AssessmentComponentDefinition(UUID.randomUUID().toString(), "End semester", "external", 75.0, 60.0)
    )) }
    var pass by remember(initialProfile) { mutableStateOf((initialProfile?.grading?.passingPercent ?: initialPassingPercent).toString()) }
    var decimalPlaces by remember(initialProfile) { mutableStateOf((initialProfile?.grading?.decimalPlaces ?: 2).toString()) }
    var combination by remember(initialProfile) { mutableStateOf(initialProfile?.defaultScheme?.combinationMethod ?: CombinationMethod.WEIGHTED_AVERAGE) }
    var missing by remember(initialProfile) { mutableStateOf(initialProfile?.defaultScheme?.missingMarkPolicy ?: MissingMarkPolicy.EXCLUDE_AND_RENORMALIZE) }
    var bestN by remember(initialProfile) { mutableStateOf((initialProfile?.defaultScheme?.bestN ?: 1).toString()) }
    var error by remember { mutableStateOf("") }
    var draftNotice by remember { mutableStateOf("") }
    var showSampleCalculation by remember { mutableStateOf(false) }

    val profile = AcademicProfile(
        institution = institution.trim(), university = university.trim(), program = program.trim(),
        department = department.trim(), regulation = regulation.trim(), academicYear = academicYear.trim(),
        calendarType = calendar,
        periods = periodNames.mapIndexed { index, name -> AcademicPeriod(
            "period-$index-${name.trim().lowercase().replace(" ", "-" )}", name.trim(), index,
            AcademicProfileCodec.parseDate(periodStartDates.getOrNull(index).orEmpty()),
            AcademicProfileCodec.parseDate(periodEndDates.getOrNull(index).orEmpty())
        ) },
        defaultScheme = AssessmentScheme(components, combination, missing, bestN.toIntOrNull() ?: 1, true),
        grading = GradingScheme(
            bands = gradeBands,
            passingPercent = pass.toDoubleOrNull() ?: Double.NaN,
            decimalPlaces = decimalPlaces.toIntOrNull() ?: -1
        )
    )
    val invalidDateInputs = periodStartDates.indices.any { index ->
        (periodStartDates.getOrNull(index).orEmpty().isNotBlank() && AcademicProfileCodec.parseDate(periodStartDates[index]) == null) ||
            (periodEndDates.getOrNull(index).orEmpty().isNotBlank() && AcademicProfileCodec.parseDate(periodEndDates[index]) == null)
    }
    val warnings = AcademicSetupValidator.warnings(profile)
    val errors = AcademicSetupValidator.validate(profile) +
        (if (combination == CombinationMethod.BEST_N_OF_M && bestN.toIntOrNull() == null) listOf("Enter a valid Best-N value") else emptyList()) +
        (if (invalidDateInputs) listOf("Period dates must use yyyy-MM-dd") else emptyList())
    val sampleRules = AcademicRules(institution, term, gradeBands, pass.toDoubleOrNull() ?: 40.0,
        decimalPlaces.toIntOrNull() ?: 2, combination, missing, bestN.toIntOrNull() ?: 1)
    val sampleSubject = AcademicSubject("sample-subject", "DEMO", "Fictional sample subject", 3.0,
        components.map { AssessmentMark(it.name, it.maximumMarks * 0.8, it.maximumMarks, it.weightPercent, MarkStatus.RECORDED, it.minimumPassPercent, it.category) })
    val sampleResult = AcademicRulesEngine.calculate(sampleSubject, sampleRules)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Academic setup · Step ${step + 1} of 4", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                LinearProgressIndicator(progress = (step + 1) / 4f, modifier = Modifier.fillMaxWidth())
                Text(listOf("Institution & programme", "Academic calendar", "Assessment structure", "Grading & review")[step], color = MaterialTheme.colorScheme.primary)
                when (step) {
                    0 -> {
                        OutlinedTextField(institution, { institution = it }, Modifier.fillMaxWidth(), label = { Text("College / institution") }, singleLine = true)
                        OutlinedTextField(university, { university = it }, Modifier.fillMaxWidth(), label = { Text("University (optional)") }, singleLine = true)
                        OutlinedTextField(program, { program = it }, Modifier.fillMaxWidth(), label = { Text("Programme") }, singleLine = true)
                        OutlinedTextField(department, { department = it }, Modifier.fillMaxWidth(), label = { Text("Department / branch") }, singleLine = true)
                        OutlinedTextField(regulation, { regulation = it }, Modifier.fillMaxWidth(), label = { Text("Regulation / batch") }, singleLine = true)
                        OutlinedTextField(academicYear, { academicYear = it }, Modifier.fillMaxWidth(), label = { Text("Academic year (e.g. 2026–27)") }, singleLine = true)
                    }
                    1 -> {
                        Text("Academic periods", fontWeight = FontWeight.SemiBold)
                        periodNames.forEachIndexed { index, periodName ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedTextField(periodName, { value ->
                                        periodNames = periodNames.toMutableList().also { it[index] = value }
                                        if (index == 0) term = value
                                    }, Modifier.fillMaxWidth(), label = { Text("Period ${index + 1} name") }, singleLine = true)
                                    OutlinedTextField(periodStartDates.getOrNull(index).orEmpty(), { value -> periodStartDates = periodStartDates.toMutableList().also { it[index] = value } }, Modifier.fillMaxWidth(), label = { Text("Start date (yyyy-MM-dd, optional)") }, singleLine = true)
                                    OutlinedTextField(periodEndDates.getOrNull(index).orEmpty(), { value -> periodEndDates = periodEndDates.toMutableList().also { it[index] = value } }, Modifier.fillMaxWidth(), label = { Text("End date (yyyy-MM-dd, optional)") }, singleLine = true)
                                    if (index > 0) TextButton(onClick = {
                                        periodNames = periodNames.filterIndexed { i, _ -> i != index }
                                        periodStartDates = periodStartDates.filterIndexed { i, _ -> i != index }
                                        periodEndDates = periodEndDates.filterIndexed { i, _ -> i != index }
                                    }) { Text("Remove period") }
                                }
                            }
                        }
                        OutlinedButton(onClick = {
                            if (periodNames.size < 16) {
                                periodNames = periodNames + "Semester ${periodNames.size + 1}"
                                periodStartDates = periodStartDates + ""
                                periodEndDates = periodEndDates + ""
                            }
                        }) { Text("Add academic period") }
                        Text("Calendar type: ${calendar.name.lowercase().replaceFirstChar { it.uppercase() }}")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(AcademicCalendarType.SEMESTER, AcademicCalendarType.TRIMESTER, AcademicCalendarType.QUARTER).forEach { type ->
                                OutlinedButton(onClick = { calendar = type }) { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(AcademicCalendarType.ANNUAL, AcademicCalendarType.MODULE, AcademicCalendarType.CUSTOM).forEach { type ->
                                OutlinedButton(onClick = { calendar = type }) { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                            }
                        }
                        Text("Period dates can be added later. Archived periods remain separate in historical analytics.", style = MaterialTheme.typography.bodySmall)
                    }
                    2 -> {
                        Text("Components must use the real maxima and contribution weights from your institution.", style = MaterialTheme.typography.bodySmall)
                        components.forEachIndexed { index, component ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Component ${index + 1}", fontWeight = FontWeight.SemiBold)
                                    OutlinedTextField(component.name, { value -> components = components.toMutableList().also { it[index] = component.copy(name = value) } }, Modifier.fillMaxWidth(), label = { Text("Name") }, singleLine = true)
                                    OutlinedTextField(component.category, { value -> components = components.toMutableList().also { it[index] = component.copy(category = value) } }, Modifier.fillMaxWidth(), label = { Text("Category (internal / practical / external)") }, singleLine = true)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(component.maximumMarks.toString(), { value -> value.toDoubleOrNull()?.let { components = components.toMutableList().also { list -> list[index] = component.copy(maximumMarks = it) } } }, Modifier.weight(1f), label = { Text("Maximum") }, singleLine = true)
                                        OutlinedTextField(component.weightPercent.toString(), { value -> value.toDoubleOrNull()?.let { components = components.toMutableList().also { list -> list[index] = component.copy(weightPercent = it) } } }, Modifier.weight(1f), label = { Text("Weight %") }, singleLine = true)
                                    }
                                    OutlinedTextField(component.minimumPassPercent?.toString().orEmpty(), { value -> components = components.toMutableList().also { it[index] = component.copy(minimumPassPercent = value.toDoubleOrNull()) } }, Modifier.fillMaxWidth(), label = { Text("Optional minimum pass %") }, singleLine = true)
                                    TextButton(onClick = { if (components.size > 1) components = components.filterIndexed { i, _ -> i != index } }) { Text("Remove component") }
                                }
                            }
                        }
                        OutlinedButton(onClick = { if (components.size < 20) components = components + AssessmentComponentDefinition(UUID.randomUUID().toString(), "New component", "assessment", 100.0, 0.0) }) { Text("Add component") }
                        Text("Combination: ${combination.name}")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { combination = CombinationMethod.WEIGHTED_AVERAGE }) { Text("Weighted") }
                            OutlinedButton(onClick = { combination = CombinationMethod.ARITHMETIC_AVERAGE }) { Text("Average") }
                            OutlinedButton(onClick = { combination = CombinationMethod.BEST_N_OF_M }) { Text("Best N") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { combination = CombinationMethod.HIGHEST }) { Text("Highest") }
                            OutlinedButton(onClick = { combination = CombinationMethod.SUM }) { Text("Sum") }
                        }
                        OutlinedButton(onClick = { combination = CombinationMethod.THEORY_PRACTICAL }, modifier = Modifier.fillMaxWidth()) { Text("Weighted result + internal/practical/external subtotals") }
                        if (combination == CombinationMethod.BEST_N_OF_M) OutlinedTextField(bestN, { bestN = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("Best N") }, singleLine = true)
                    }
                    else -> {
                        OutlinedTextField(pass, { pass = it }, Modifier.fillMaxWidth(), label = { Text("Overall passing percentage") }, singleLine = true)
                        OutlinedTextField(decimalPlaces, { decimalPlaces = it.filter(Char::isDigit).take(1) }, Modifier.fillMaxWidth(), label = { Text("Decimal places (0–4)") }, singleLine = true)
                        Text("Grade bands", fontWeight = FontWeight.SemiBold)
                        gradeBands.forEachIndexed { index, band ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(band.label, { value -> gradeBands = gradeBands.toMutableList().also { it[index] = band.copy(label = value) } }, Modifier.weight(1f), label = { Text("Grade") }, singleLine = true)
                                OutlinedTextField(band.minimumPercent.toString(), { value -> value.toDoubleOrNull()?.let { parsed -> gradeBands = gradeBands.toMutableList().also { list -> list[index] = band.copy(minimumPercent = parsed) } } }, Modifier.weight(1f), label = { Text("Min %") }, singleLine = true)
                                OutlinedTextField(band.gradePoint.toString(), { value -> value.toDoubleOrNull()?.let { parsed -> gradeBands = gradeBands.toMutableList().also { list -> list[index] = band.copy(gradePoint = parsed) } } }, Modifier.weight(1f), label = { Text("GP") }, singleLine = true)
                            }
                            TextButton(onClick = { if (gradeBands.size > 1) gradeBands = gradeBands.filterIndexed { i, _ -> i != index } }) { Text("Remove grade band") }
                        }
                        OutlinedButton(onClick = { if (gradeBands.size < 30) gradeBands = gradeBands + GradeBand(0.0, "New", 0.0) }) { Text("Add grade band") }
                        Text("Missing mark policy: ${missing.name}")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { missing = MissingMarkPolicy.EXCLUDE_AND_RENORMALIZE }) { Text("Exclude missing") }
                            OutlinedButton(onClick = { missing = MissingMarkPolicy.COUNT_ABSENT_AS_ZERO }) { Text("Absent = 0") }
                        }
                        OutlinedButton(onClick = { missing = MissingMarkPolicy.REQUIRE_ALL_RECORDED }, modifier = Modifier.fillMaxWidth()) { Text("Require all marks before final calculation") }
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Review", fontWeight = FontWeight.Bold)
                                Text("${institution.ifBlank { "Institution not set" }} · ${program.ifBlank { "Programme not set" }}")
                                Text("${periodNames.joinToString(" · ")} · ${calendar.name}")
                                Text("${components.size} assessment components · ${components.sumOf { it.weightPercent }}% total weight")
                                Text("Passing: ${pass}% · ${gradeBands.size} configurable grade bands")
                                Text("Attendance marks are separate unless your institution's exact rule is configured.")
                            }
                        }
                        OutlinedButton(onClick = { showSampleCalculation = !showSampleCalculation }) { Text(if (showSampleCalculation) "Hide sample calculation" else "Test with fictional 80% marks") }
                        if (showSampleCalculation) {
                            Text("Sample result: ${sampleResult.percentage?.let { "%.2f%%".format(it) } ?: "Not calculable"} · ${sampleResult.grade ?: "No mapped grade"}", fontWeight = FontWeight.SemiBold)
                            Text(sampleResult.explanation, style = MaterialTheme.typography.bodySmall)
                            Text("This is fictional test data only; it does not change your actual marks.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (warnings.isNotEmpty()) Text(warnings.joinToString("\n"), color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall)
                        if (errors.isNotEmpty()) {
                            Text(errors.joinToString("\n"), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (draftNotice.isNotBlank()) Text(draftNotice, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = onDismiss) { Text("Cancel") }
                        TextButton(onClick = {
                            val draft = profile.copy(grading = profile.grading.copy(
                                passingPercent = pass.toDoubleOrNull() ?: 40.0,
                                decimalPlaces = decimalPlaces.toIntOrNull()?.coerceIn(0, 4) ?: 2
                            ))
                            val safeDraft = draft.copy(grading = draft.grading.copy(
                                passingPercent = draft.grading.passingPercent.takeIf { it.isFinite() && it in 0.0..100.0 } ?: 40.0,
                                decimalPlaces = draft.grading.decimalPlaces.coerceIn(0, 4)
                            ))
                            runCatching { onSaveDraft(safeDraft) }
                                .onSuccess { draftNotice = "Draft saved securely on this device. Reopen the wizard to continue." }
                                .onFailure { error = "Could not save draft: ${it.message}" }
                        }) { Text("Save draft") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (step > 0) OutlinedButton(onClick = { step-- }) { Text("Back") }
                        Button(onClick = {
                            error = ""
                            if (step < 3) step++ else if (errors.isEmpty()) onComplete(AcademicWizardResult(profile, combination, missing, bestN.toIntOrNull() ?: 1)) else error = "Fix the validation issues before saving."
                        }) { Text(if (step == 3) "Save setup" else "Continue") }
                    }
                }
            }
        }
    }
}
