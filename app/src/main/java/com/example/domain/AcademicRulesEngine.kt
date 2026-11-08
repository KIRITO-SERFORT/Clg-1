package com.example.domain

import com.example.data.model.AcademicRules
import com.example.data.model.AcademicSubject
import com.example.data.model.AcademicTermResult
import com.example.data.model.AssessmentContribution
import com.example.data.model.CombinationMethod
import com.example.data.model.MarkStatus
import com.example.data.model.MissingMarkPolicy
import com.example.data.model.SubjectResult
import java.math.BigDecimal
import java.math.RoundingMode

/** Pure deterministic academic calculation service. No Android or UI dependencies. */
object AcademicRulesEngine {
    fun validate(subject: AcademicSubject): List<String> = buildList {
        if (subject.id.isBlank()) add("Subject ID is required")
        if (subject.name.isBlank()) add("Subject name is required")
        if (!subject.credits.isFinite() || subject.credits < 0.0) add("Credits must be finite and non-negative")
        if (subject.subjectPassPercent != null && (!subject.subjectPassPercent.isFinite() || subject.subjectPassPercent !in 0.0..100.0)) add("Subject pass percentage must be between 0 and 100")
        val names = subject.assessments.map { it.name.trim().lowercase() }
        if (names.any { it.isBlank() }) add("Assessment names cannot be blank")
        if (names.distinct().size != names.size) add("Assessment names must be unique within a subject")
        subject.assessments.forEachIndexed { index, item ->
            val n = index + 1
            if (!item.maximum.isFinite() || item.maximum <= 0.0) add("Assessment $n: maximum marks must be finite and positive")
            if (!item.weightPercent.isFinite() || item.weightPercent < 0.0) add("Assessment $n: weight must be finite and non-negative")
            if (item.obtained != null && (!item.obtained.isFinite() || item.obtained < 0.0 || item.obtained > item.maximum)) add("Assessment $n: marks are outside the valid range")
            if (item.minimumPassPercent != null && (!item.minimumPassPercent.isFinite() || item.minimumPassPercent !in 0.0..100.0)) add("Assessment $n: minimum pass percentage must be between 0 and 100")
        }
        val configuredWeights = subject.assessments.filter { it.status != MarkStatus.EXEMPTED }.sumOf { it.weightPercent }
        if (configuredWeights > 100.0000001) add("Assessment weights exceed 100%")
    }

    fun validate(rules: AcademicRules): List<String> = buildList {
        if (!rules.passingPercent.isFinite() || rules.passingPercent !in 0.0..100.0) add("Passing percentage must be between 0 and 100")
        if (rules.decimalPlaces !in 0..4) add("Decimal places must be between 0 and 4")
        if (rules.bestN < 1) add("Best-N must be at least 1")
        if (rules.requiredWeightPercent != null && (!rules.requiredWeightPercent.isFinite() || rules.requiredWeightPercent !in 0.0..100.0)) add("Required weight must be between 0 and 100")
        if (rules.gradeBands.any { !it.minimumPercent.isFinite() || it.minimumPercent !in 0.0..100.0 || !it.gradePoint.isFinite() || it.gradePoint < 0.0 || it.label.isBlank() }) add("Grade bands contain an invalid boundary, label or grade point")
        if (rules.gradeBands.map { it.minimumPercent }.distinct().size != rules.gradeBands.size) add("Grade boundaries must be unique")
        if (rules.gradeBands.map { it.label.trim().lowercase() }.distinct().size != rules.gradeBands.size) add("Grade labels must be unique")
    }

    fun calculate(subject: AcademicSubject, rules: AcademicRules): SubjectResult {
        val errors = validate(subject) + validate(rules)
        if (errors.isNotEmpty()) return SubjectResult(null, null, null, null, errors.joinToString("; "), true, errors = errors)
        if (subject.assessments.isEmpty()) return SubjectResult(null, null, null, null, "No assessment components are configured.", true)

        val nonExempt = subject.assessments.filter { it.status != MarkStatus.EXEMPTED }
        if (rules.combinationMethod == CombinationMethod.BEST_N_OF_M && rules.bestN > nonExempt.size) {
            return SubjectResult(null, null, null, null, "Best-N cannot exceed the configured non-exempt assessment count.", true,
                errors = listOf("Best-N cannot exceed the configured non-exempt assessment count."))
        }
        val missing = nonExempt.filter { it.status == MarkStatus.NOT_CONDUCTED || (it.status == MarkStatus.RECORDED && it.obtained == null) }
        val absent = nonExempt.filter { it.status == MarkStatus.ABSENT }
        if (rules.missingMarkPolicy == MissingMarkPolicy.REQUIRE_ALL_RECORDED && (missing.isNotEmpty() || absent.isNotEmpty())) {
            return SubjectResult(null, null, null, null, "Waiting for all non-exempt assessment marks.", true)
        }

        data class Item(val markIndex: Int, val name: String, val percent: Double, val weight: Double, val pass: Boolean?, val note: String)
        val items = mutableListOf<Item>()
        subject.assessments.forEachIndexed { index, mark ->
            when (mark.status) {
                MarkStatus.EXEMPTED -> Unit
                MarkStatus.ABSENT -> if (rules.missingMarkPolicy == MissingMarkPolicy.COUNT_ABSENT_AS_ZERO) {
                    items += Item(index, mark.name, 0.0, mark.weightPercent, false, "Absent treated as zero by configured rule")
                }
                MarkStatus.NOT_CONDUCTED -> Unit
                MarkStatus.RECORDED -> {
                    val obtained = mark.obtained ?: return@forEachIndexed
                    val pct = obtained / mark.maximum * 100.0
                    val componentPass = mark.minimumPassPercent?.let { pct >= it }
                    items += Item(index, mark.name, pct, mark.weightPercent, componentPass, "${format(obtained, 2)}/${format(mark.maximum, 2)}")
                }
            }
        }
        if (items.isEmpty()) return SubjectResult(null, null, null, null, "No eligible assessment marks are available yet.", true)

        val result = when (rules.combinationMethod) {
            CombinationMethod.WEIGHTED_AVERAGE, CombinationMethod.THEORY_PRACTICAL -> {
                val weightTotal = items.sumOf { it.weight }
                if (weightTotal <= 0.0) return SubjectResult(null, null, null, null, "Assessment weights are not configured for the available marks.", true)
                items.sumOf { it.percent * it.weight } / weightTotal
            }
            CombinationMethod.ARITHMETIC_AVERAGE -> items.map { it.percent }.average()
            CombinationMethod.BEST_N_OF_M -> items.map { it.percent }.sortedDescending().take(rules.bestN).average()
            CombinationMethod.HIGHEST -> items.maxOf { it.percent }
            CombinationMethod.SUM -> {
                val includedMarks = items.map { subject.assessments[it.markIndex] }
                val obtainedTotal = includedMarks.sumOf { if (it.status == MarkStatus.ABSENT) 0.0 else it.obtained ?: 0.0 }
                val maxTotal = includedMarks.sumOf { it.maximum }
                if (maxTotal <= 0.0) return SubjectResult(null, null, null, null, "No valid maximum marks are available.", true)
                obtainedTotal / maxTotal * 100.0
            }
        }.coerceIn(0.0, 100.0)

        fun categoryAverage(categories: Set<String>): Double? {
            val selected = items.filter { subject.assessments[it.markIndex].category.trim().lowercase() in categories }
            val totalWeight = selected.sumOf { it.weight }
            return if (selected.isEmpty() || totalWeight <= 0.0) null else roundTo(selected.sumOf { it.percent * it.weight } / totalWeight, rules.decimalPlaces)
        }
        val internalPercent = categoryAverage(setOf("internal", "internal assessment", "internal_assessment", "test", "assignment", "quiz", "continuous assessment", "continuous_assessment"))
        val practicalPercent = categoryAverage(setOf("practical", "lab", "laboratory"))
        val externalPercent = categoryAverage(setOf("external", "end semester", "end_semester", "final", "theory", "university exam"))
        val rounded = roundTo(result, rules.decimalPlaces)
        val band = rules.gradeBands.sortedByDescending { it.minimumPercent }.firstOrNull {
            if (it.inclusive) rounded >= it.minimumPercent else rounded > it.minimumPercent
        }
        val subjectPass = subject.subjectPassPercent ?: rules.passingPercent
        val componentPass = items.all { it.pass != false }
        val passed = rounded >= subjectPass && componentPass
        val recordedCount = subject.assessments.count { it.status == MarkStatus.RECORDED && it.obtained != null }
        val provisional = missing.isNotEmpty() || absent.isNotEmpty() || recordedCount + subject.assessments.count { it.status == MarkStatus.EXEMPTED } != subject.assessments.size
        val effectiveWeights = mutableMapOf<Int, Double>()
        val droppedByRule = mutableSetOf<Int>()
        when (rules.combinationMethod) {
            CombinationMethod.WEIGHTED_AVERAGE, CombinationMethod.THEORY_PRACTICAL -> {
                val totalWeight = items.sumOf { it.weight }
                if (totalWeight > 0.0) items.forEach { effectiveWeights[it.markIndex] = it.weight / totalWeight * 100.0 }
            }
            CombinationMethod.ARITHMETIC_AVERAGE -> items.forEach { effectiveWeights[it.markIndex] = 100.0 / items.size }
            CombinationMethod.BEST_N_OF_M -> {
                val selected = items.sortedWith(compareByDescending<Item> { it.percent }.thenBy { it.markIndex }).take(rules.bestN)
                selected.forEach { effectiveWeights[it.markIndex] = 100.0 / selected.size }
                droppedByRule += items.filter { it !in selected }.map { it.markIndex }
            }
            CombinationMethod.HIGHEST -> {
                val selected = items.maxWithOrNull(compareBy<Item> { it.percent }.thenBy { -it.markIndex })
                selected?.let { effectiveWeights[it.markIndex] = 100.0 }
                droppedByRule += items.filter { it.markIndex != selected?.markIndex }.map { it.markIndex }
            }
            CombinationMethod.SUM -> {
                val maximumTotal = items.sumOf { subject.assessments[it.markIndex].maximum }
                if (maximumTotal > 0.0) items.forEach { item -> effectiveWeights[item.markIndex] = subject.assessments[item.markIndex].maximum / maximumTotal * 100.0 }
            }
        }
        val contributions = subject.assessments.mapIndexed { index, mark ->
            val item = items.firstOrNull { it.markIndex == index }
            val effectiveWeight = effectiveWeights[index]
            when {
                item == null -> AssessmentContribution(mark.name, 0.0, 0.0, 0.0, false,
                    when (mark.status) { MarkStatus.EXEMPTED -> "Exempted"; MarkStatus.NOT_CONDUCTED -> "Not conducted"; MarkStatus.ABSENT -> "Absent excluded by rule"; else -> "Mark missing" })
                effectiveWeight == null || index in droppedByRule -> AssessmentContribution(item.name, item.percent, 0.0, 0.0, false, "Eligible mark dropped by the selected combination rule")
                else -> AssessmentContribution(item.name, item.percent, effectiveWeight, item.percent * effectiveWeight / 100.0, true, item.note)
            }
        }
        val methodLabel = when (rules.combinationMethod) {
            CombinationMethod.WEIGHTED_AVERAGE -> "weighted normalized average"
            CombinationMethod.ARITHMETIC_AVERAGE -> "arithmetic average of normalized percentages"
            CombinationMethod.BEST_N_OF_M -> "best ${rules.bestN} of ${items.size} normalized percentages"
            CombinationMethod.HIGHEST -> "highest normalized percentage"
            CombinationMethod.SUM -> "sum of obtained marks divided by sum of maximum marks"
            CombinationMethod.THEORY_PRACTICAL -> "combined weighted result with separate internal, practical/lab and external subtotals"
        }
        val explanation = "${methodLabel}; ${recordedCount}/${subject.assessments.size} assessments recorded; pass rule ${format(subjectPass, 2)}%; rounded to ${rules.decimalPlaces} decimal places."
        return SubjectResult(rounded, band?.label, band?.gradePoint, passed, explanation, provisional, contributions,
            internalPercent = internalPercent, practicalPercent = practicalPercent, externalPercent = externalPercent)
    }

    fun sgpa(subjects: List<AcademicSubject>, rules: AcademicRules): Double? {
        if (subjects.isEmpty() || validate(rules).isNotEmpty()) return null
        val results = subjects.map { it to calculate(it, rules) }
        if (results.any { it.second.gradePoint == null || it.first.credits <= 0.0 || it.second.provisional }) return null
        val credits = results.sumOf { it.first.credits }
        if (credits <= 0.0) return null
        return roundTo(results.sumOf { it.first.credits * it.second.gradePoint!! } / credits, 4)
    }

    fun cgpa(terms: List<AcademicTermResult>): Double? {
        if (terms.isEmpty() || terms.any { !it.credits.isFinite() || it.credits <= 0.0 || !it.sgpa.isFinite() || it.sgpa < 0.0 }) return null
        return roundTo(terms.sumOf { it.credits * it.sgpa } / terms.sumOf { it.credits }, 4)
    }

    /** Returns the percentage needed on the still-remaining weight (0..100), or null when impossible/undefined. */
    fun requiredRemainingPercentage(currentWeightedPoints: Double, remainingWeight: Double, targetPercent: Double): Double? {
        if (!currentWeightedPoints.isFinite() || !remainingWeight.isFinite() || remainingWeight <= 0.0 || !targetPercent.isFinite() || targetPercent !in 0.0..100.0) return null
        val required = (targetPercent - currentWeightedPoints) / remainingWeight * 100.0
        return required.takeIf { it in 0.0..100.0 }?.let { roundTo(it, 2) }
    }

    /** Backwards-compatible helper retained for callers from the first implementation. */
    fun requiredMarks(currentWeightedPoints: Double, remainingWeight: Double, targetPercent: Double): Double? =
        requiredRemainingPercentage(currentWeightedPoints, remainingWeight, targetPercent)

    fun roundTo(value: Double, places: Int): Double = BigDecimal.valueOf(value)
        .setScale(places.coerceIn(0, 6), RoundingMode.HALF_UP).toDouble()

    private fun format(value: Double, places: Int): String = BigDecimal.valueOf(value).setScale(places, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
}
