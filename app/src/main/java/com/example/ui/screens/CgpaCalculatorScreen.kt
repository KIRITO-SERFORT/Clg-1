package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.CgpaEvaluator
import com.example.domain.CgpaRounding
import com.example.domain.CgpaUiState
import com.example.domain.Evaluation
import com.example.domain.GradeRow
import com.example.domain.GradeScales
import com.example.domain.QuickRow
import com.example.domain.SemesterBlock
import com.example.domain.SubjectRow
import com.example.domain.gradeRowsOf
import com.example.ui.CgpaViewModel

private val MODES = listOf("By subjects", "From SGPAs", "Target")

/** Full-screen CGPA calculator. Works for any college: nothing needs the ERP, an account or the academic setup. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CgpaCalculatorScreen(onClose: () -> Unit, vm: CgpaViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val eval = remember(state) { CgpaEvaluator.evaluate(state) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("CGPA Calculator") },
            navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close calculator") } }
        )
    }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ResultCard(state, eval)
            TabRow(selectedTabIndex = state.mode) {
                MODES.forEachIndexed { i, t -> Tab(selected = state.mode == i, onClick = { vm.edit { it.copy(mode = i) } }, text = { Text(t, maxLines = 1) }) }
            }
            when (state.mode) {
                0 -> SubjectsMode(state, eval, vm)
                1 -> QuickMode(state, eval, vm)
                else -> TargetMode(state, eval, vm)
            }
            SettingsCard(state, vm)
            Text("Results are calculated on this phone from what you type. Check them against your university's official result.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun NumField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, supporting: String? = null, isError: Boolean = false) {
    OutlinedTextField(
        value = value, onValueChange = { if (it.length <= 8) onChange(it) }, label = { Text(label, maxLines = 1) }, singleLine = true, isError = isError,
        supportingText = supporting?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = modifier
    )
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (title.isNotBlank()) Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

// ------------------------------------------------------------------------------------------------ result

@Composable
private fun ResultCard(state: CgpaUiState, eval: Evaluation) {
    val shown = if (state.mode == 1) eval.quick else eval.subjects
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("YOUR CGPA", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(shown.cgpa?.toPlainString() ?: "\u2014", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.testTag("cgpa_value"))
            val credits = shown.totalCredits.stripTrailingZeros().toPlainString()
            Text(buildString {
                append(if (shown.totalCredits.signum() > 0) "$credits credits counted" else "Enter your semesters below")
                eval.percent?.let { append("  \u00B7  about ${it.toPlainString()}%") }
            }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            shown.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer) }
            eval.warnings.forEach { Text("Note: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        }
    }
}

// ------------------------------------------------------------------------------------------------ by subjects

@Composable
private fun SubjectsMode(state: CgpaUiState, eval: Evaluation, vm: CgpaViewModel) {
    state.semesters.forEachIndexed { si, sem ->
        var confirm by remember { mutableStateOf(false) }
        if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Remove ${sem.name.ifBlank { "semester" }}?" ) },
            text = { Text("Its subjects are removed from the calculator.") },
            confirmButton = { TextButton(onClick = { confirm = false; vm.edit { s -> s.copy(semesters = s.semesters.filter { it.id != sem.id }) } }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Keep") } })

        Section("") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(sem.name, { v -> vm.edit { s -> s.copy(semesters = s.semesters.map { if (it.id == sem.id) it.copy(name = v.take(40)) else it }) } },
                    label = { Text("Semester name") }, singleLine = true, modifier = Modifier.weight(1f))
                IconButton(onClick = { confirm = true }) { Icon(Icons.Default.Delete, contentDescription = "Remove ${sem.name}") }
            }
            sem.subjects.forEach { r -> SubjectRowUi(state, eval, sem, r, vm) }
            val semResult = eval.subjects.semesters.getOrNull(si)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { vm.edit { s -> s.copy(semesters = s.semesters.map { if (it.id == sem.id) it.copy(subjects = it.subjects + SubjectRow(vm.newId())) else it }) } }) {
                    Icon(Icons.Default.Add, contentDescription = null); Text("Add subject")
                }
                Spacer(Modifier.weight(1f))
                Text("SGPA: ${semResult?.sgpa?.toPlainString() ?: "\u2014"}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
    Button(onClick = { vm.edit { s -> s.copy(semesters = s.semesters + SemesterBlock(vm.newId(), "Semester ${s.semesters.size + 1}", listOf(SubjectRow(vm.newId()), SubjectRow(vm.newId()), SubjectRow(vm.newId())))) } },
        modifier = Modifier.fillMaxWidth().testTag("add_semester")) { Icon(Icons.Default.Add, contentDescription = null); Text("Add semester") }
}

@Composable
private fun SubjectRowUi(state: CgpaUiState, eval: Evaluation, sem: SemesterBlock, r: SubjectRow, vm: CgpaViewModel) {
    fun upd(f: (SubjectRow) -> SubjectRow) = vm.edit { s -> s.copy(semesters = s.semesters.map { b -> if (b.id == sem.id) b.copy(subjects = b.subjects.map { if (it.id == r.id) f(it) else it }) else b }) }
    val issue = eval.rowIssues[r.id]
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(r.name, { v -> upd { it.copy(name = v.take(60)) } }, label = { Text("Subject") }, singleLine = true, modifier = Modifier.weight(1f))
            NumField("Credits", r.credits, { v -> upd { it.copy(credits = v) } }, Modifier.width(96.dp), isError = issue != null && r.credits.isNotBlank() && eval.rowIssues[r.id]!!.startsWith("Credits"))
            IconButton(onClick = { vm.edit { s -> s.copy(semesters = s.semesters.map { b -> if (b.id == sem.id) b.copy(subjects = b.subjects.filter { it.id != r.id }) else b }) } }) {
                Icon(Icons.Default.Delete, contentDescription = "Remove subject ${r.name}")
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            state.grades.filter { it.label.isNotBlank() }.forEach { g ->
                FilterChip(selected = r.grade.equals(g.label, ignoreCase = true), onClick = { upd { it.copy(grade = g.label) } }, label = { Text(g.label) })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = r.counts, onCheckedChange = { v -> upd { it.copy(counts = v) } })
            Text("Counts toward GPA", style = MaterialTheme.typography.bodySmall)
        }
        if (issue != null) Text(issue, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
}

// ------------------------------------------------------------------------------------------------ quick (SGPAs)

@Composable
private fun QuickMode(state: CgpaUiState, eval: Evaluation, vm: CgpaViewModel) {
    Section("Semester SGPAs") {
        Text("Type the SGPA your university published for each semester. Add the semester's credits for a credit-weighted CGPA.", style = MaterialTheme.typography.bodySmall)
        state.quick.forEach { q ->
            fun upd(f: (QuickRow) -> QuickRow) = vm.edit { s -> s.copy(quick = s.quick.map { if (it.id == q.id) f(it) else it }) }
            val issue = eval.rowIssues[q.id]
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(q.name, { v -> upd { it.copy(name = v.take(40)) } }, label = { Text("Semester") }, singleLine = true, modifier = Modifier.weight(1f))
                    NumField("SGPA", q.sgpa, { v -> upd { it.copy(sgpa = v) } }, Modifier.width(88.dp))
                    NumField("Credits", q.credits, { v -> upd { it.copy(credits = v) } }, Modifier.width(88.dp))
                    IconButton(onClick = { vm.edit { s -> s.copy(quick = s.quick.filter { it.id != q.id }) } }) { Icon(Icons.Default.Delete, contentDescription = "Remove ${q.name}") }
                }
                if (issue != null) Text(issue, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
        OutlinedButton(onClick = { vm.edit { s -> s.copy(quick = s.quick + QuickRow(vm.newId(), "Semester ${s.quick.size + 1}")) } }) { Icon(Icons.Default.Add, contentDescription = null); Text("Add semester") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Treat every semester equally", style = MaterialTheme.typography.bodyMedium)
                Text("Ignores credits (plain average). Only right if all semesters have equal credits.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = state.equalWeights, onCheckedChange = { v -> vm.edit { it.copy(equalWeights = v) } })
        }
    }
}

// ------------------------------------------------------------------------------------------------ target

@Composable
private fun TargetMode(state: CgpaUiState, eval: Evaluation, vm: CgpaViewModel) {
    val t = state.target
    Section("What do I need?") {
        Text("Works out the average grade point you need in your remaining credits to reach a target CGPA.", style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("Current CGPA", t.current, { v -> vm.edit { it.copy(target = it.target.copy(current = v)) } }, Modifier.weight(1f))
            NumField("Credits done", t.done, { v -> vm.edit { it.copy(target = it.target.copy(done = v)) } }, Modifier.weight(1f))
        }
        val cgpa = eval.prefillCurrent; val credits = eval.prefillCredits
        if (cgpa != null && credits != null) TextButton(onClick = { vm.edit { it.copy(target = it.target.copy(current = cgpa.toPlainString(), done = credits.stripTrailingZeros().toPlainString())) } }) {
            Text("Use my calculated CGPA (${cgpa.toPlainString()}) and ${credits.stripTrailingZeros().toPlainString()} credits")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("Credits left", t.remaining, { v -> vm.edit { it.copy(target = it.target.copy(remaining = v)) } }, Modifier.weight(1f))
            NumField("Target CGPA", t.target, { v -> vm.edit { it.copy(target = it.target.copy(target = v)) } }, Modifier.weight(1f))
        }
        val r = eval.target
        if (r == null) Text("Fill in all four numbers to see the result.", style = MaterialTheme.typography.bodySmall)
        else {
            Text(if (r.feasible) "Reachable" else "Not reachable", fontWeight = FontWeight.Bold, color = if (r.feasible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            Text(r.message)
            r.bestReachableCgpa?.let { Text("Best possible CGPA: ${it.toPlainString()}", style = MaterialTheme.typography.bodySmall) }
        }
    }
}

// ------------------------------------------------------------------------------------------------ settings

@Composable
private fun SettingsCard(state: CgpaUiState, vm: CgpaViewModel) {
    var confirmReset by remember { mutableStateOf(false) }
    if (confirmReset) AlertDialog(onDismissRequest = { confirmReset = false }, title = { Text("Reset calculator?") },
        text = { Text("All semesters, grades and settings in the calculator are cleared from this phone.") },
        confirmButton = { TextButton(onClick = { confirmReset = false; vm.resetAll() }) { Text("Reset") } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } })

    Section("Settings: match your university") {
        Text("Grade scale", fontWeight = FontWeight.SemiBold)
        Text("These are common examples. Pick one and edit it to match your regulations.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GradeScales.PRESETS.forEach { p -> FilterChip(selected = state.scaleName == p.name, onClick = { vm.edit { it.copy(scaleName = p.name, grades = gradeRowsOf(p)) } }, label = { Text(p.name, maxLines = 1) }) }
        }
        state.grades.forEach { g ->
            fun upd(f: (GradeRow) -> GradeRow) = vm.edit { s -> s.copy(scaleName = "Custom scale", grades = s.grades.map { if (it.id == g.id) f(it) else it }) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(g.label, { v -> upd { it.copy(label = v.take(6)) } }, label = { Text("Grade") }, singleLine = true, modifier = Modifier.weight(1f))
                NumField("Points", g.point, { v -> upd { it.copy(point = v) } }, Modifier.weight(1f))
                IconButton(onClick = { vm.edit { s -> s.copy(scaleName = "Custom scale", grades = s.grades.filter { it.id != g.id }) } }) { Icon(Icons.Default.Delete, contentDescription = "Remove grade ${g.label}") }
            }
        }
        OutlinedButton(onClick = { vm.edit { s -> s.copy(scaleName = "Custom scale", grades = s.grades + GradeRow(vm.newId(), "", "")) } }) { Icon(Icons.Default.Add, contentDescription = null); Text("Add grade") }

        Text("Rounding", fontWeight = FontWeight.SemiBold)
        CgpaRounding.values().forEach { r ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.RadioButton(selected = state.rounding == r, onClick = { vm.edit { it.copy(rounding = r) } })
                Text(r.label, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Text("Decimal places", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { (0..4).forEach { n -> FilterChip(selected = state.places == n, onClick = { vm.edit { it.copy(places = n) } }, label = { Text("$n") }) } }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Round each SGPA before the CGPA", style = MaterialTheme.typography.bodyMedium)
                Text("Some universities weight the published (rounded) SGPAs. Off = exact marks are used. Subjects tab only.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = state.roundSgpaFirst, onCheckedChange = { v -> vm.edit { it.copy(roundSgpaFirst = v) } })
        }

        Text("Percentage", fontWeight = FontWeight.SemiBold)
        NumField("CGPA \u00D7 this number = percentage", state.factor, { v -> vm.edit { it.copy(factor = v) } }, Modifier.fillMaxWidth(),
            supporting = "Leave empty if your university has no formula. Use the exact factor your university publishes.")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("9.5", "10").forEach { f -> FilterChip(selected = state.factor == f, onClick = { vm.edit { it.copy(factor = f) } }, label = { Text("\u00D7$f") }) }
            Text("examples only", style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.CenterVertically))
        }
        TextButton(onClick = { confirmReset = true }) { Text("Reset calculator") }
    }
}
