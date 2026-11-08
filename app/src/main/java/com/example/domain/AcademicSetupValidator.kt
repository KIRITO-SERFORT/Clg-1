package com.example.domain

import com.example.data.model.AcademicProfile
import com.example.data.model.AssessmentScheme
import java.util.Locale

/** Pure validation for the academic setup wizard and imported configuration. */
object AcademicSetupValidator {
    fun warnings(profile: AcademicProfile): List<String> = buildList {
        val lowest = profile.grading.bands.minOfOrNull { it.minimumPercent }
        if (lowest != null && lowest > 0.0) add("Scores below ${lowest}% have no mapped grade. Add a fail/zero-point band at 0% if your institution uses one.")
        if (profile.periods.any { it.startsAtEpochMillis == null || it.endsAtEpochMillis == null }) add("Some academic periods have no start/end dates; calendar reminders and overlap checks will be limited.")
        val categories = profile.defaultScheme.components.map { it.category.trim().lowercase(Locale.ROOT) }.toSet()
        if (categories.none { it in setOf("internal", "internal assessment", "internal_assessment", "test", "assignment", "quiz") }) add("No internal assessment category is configured; internal subtotal will be unavailable.")
        if (categories.none { it in setOf("practical", "lab", "laboratory") }) add("No practical/lab category is configured; practical subtotal will be unavailable.")
        if (categories.none { it in setOf("external", "end semester", "end_semester", "final", "theory", "university exam") }) add("No external/theory category is configured; external subtotal will be unavailable.")
    }

    fun validate(profile: AcademicProfile): List<String> = buildList {
        if (profile.schemaVersion != 1) add("Unsupported academic profile version")
        if (profile.institution.isBlank()) add("Institution name is required")
        if (profile.institution.length > 160) add("Institution name is too long")
        if (profile.program.length > 120 || profile.department.length > 120) add("Program or department name is too long")
        if (profile.regulation.length > 80 || profile.academicYear.length > 40) add("Regulation or academic year is too long")
        if (profile.academicYear.isBlank()) add("Academic year is required for reliable historical records")
        if (profile.periods.isEmpty()) add("At least one academic period is required")
        if (profile.periods.size > 40) add("A profile cannot contain more than 40 academic periods")
        val ids = profile.periods.map { it.id }
        if (ids.any { it.isBlank() } || ids.distinct().size != ids.size) add("Academic period IDs must be present and unique")
        val names = profile.periods.map { it.name.trim().lowercase(Locale.ROOT) }
        if (names.any { it.isBlank() } || names.distinct().size != names.size) add("Academic period names must be present and unique")
        if (profile.periods.map { it.order }.distinct().size != profile.periods.size || profile.periods.any { it.order < 0 }) add("Academic period order values must be unique and non-negative")
        profile.periods.forEachIndexed { index, period ->
            val start = period.startsAtEpochMillis
            val end = period.endsAtEpochMillis
            if (start != null && end != null && end < start) add("${period.name.ifBlank { "Period ${index + 1}" }} ends before it starts")
            if (period.examAtEpochMillis != null && start != null && period.examAtEpochMillis < start) add("${period.name} exam date is before the period starts")
        }
        val datedPeriods = profile.periods.filter { it.startsAtEpochMillis != null && it.endsAtEpochMillis != null }
        for (i in datedPeriods.indices) for (j in i + 1 until datedPeriods.size) {
            val first = datedPeriods[i]
            val second = datedPeriods[j]
            if (first.startsAtEpochMillis!! <= second.endsAtEpochMillis!! && second.startsAtEpochMillis!! <= first.endsAtEpochMillis!!) {
                add("Academic periods '${first.name}' and '${second.name}' overlap")
            }
        }
        addAll(validate(profile.defaultScheme))
        if (!profile.grading.passingPercent.isFinite() || profile.grading.passingPercent !in 0.0..100.0) add("Passing percentage must be between 0 and 100")
        if (profile.grading.decimalPlaces !in 0..4) add("Rounding precision must be between 0 and 4 decimal places")
        if (profile.grading.attendanceMarksMaximum != null && (!profile.grading.attendanceMarksMaximum.isFinite() || profile.grading.attendanceMarksMaximum <= 0.0)) add("Attendance marks maximum must be positive")
        if (profile.grading.relativeGradingEnabled) add("Relative grading requires verified cohort data and is not calculated from one student's marks")
        if (profile.grading.bands.size > 30) add("No more than 30 grade bands are supported")
        if (profile.grading.bands.map { it.minimumPercent }.distinct().size != profile.grading.bands.size) add("Grade-band minimum percentages must be unique")
        if (profile.grading.bands.map { it.label.trim().lowercase(Locale.ROOT) }.distinct().size != profile.grading.bands.size) add("Grade-band labels must be unique")
        profile.grading.bands.forEachIndexed { index, band ->
            if (!band.minimumPercent.isFinite() || band.minimumPercent !in 0.0..100.0) add("Grade band ${index + 1}: minimum percentage must be between 0 and 100")
            if (!band.gradePoint.isFinite() || band.gradePoint < 0.0) add("Grade band ${index + 1}: grade point must be finite and non-negative")
            if (band.label.isBlank()) add("Grade band ${index + 1}: label is required")
        }
    }

    fun validate(scheme: AssessmentScheme): List<String> = buildList {
        if (scheme.components.isEmpty()) add("At least one assessment component is required")
        if (scheme.components.size > 100) add("An assessment scheme cannot contain more than 100 components")
        if (scheme.bestN < 1) add("Best-N must be at least one")
        val ids = scheme.components.map { it.id }
        if (ids.any { it.isBlank() } || ids.distinct().size != ids.size) add("Assessment component IDs must be present and unique")
        val names = scheme.components.map { it.name.trim().lowercase(Locale.ROOT) }
        if (names.any { it.isBlank() } || names.distinct().size != names.size) add("Assessment component names must be present and unique")
        scheme.components.forEachIndexed { index, component ->
            if (component.category.isBlank() || component.category.length > 40) add("Component ${index + 1}: category is required and must be 40 characters or fewer")
            if (component.name.length > 100) add("Component ${index + 1}: name is too long")
            if (!component.maximumMarks.isFinite() || component.maximumMarks <= 0.0) add("Component ${index + 1}: maximum marks must be positive")
            if (!component.weightPercent.isFinite() || component.weightPercent !in 0.0..100.0) add("Component ${index + 1}: weight must be between 0 and 100")
            if (component.minimumPassPercent != null && (!component.minimumPassPercent.isFinite() || component.minimumPassPercent !in 0.0..100.0)) add("Component ${index + 1}: minimum pass percentage must be between 0 and 100")
        }
        val total = scheme.components.sumOf { it.weightPercent }
        if (total > 100.000001) add("Assessment weights exceed 100%")
        if (scheme.requireWeightsToTotal100 && scheme.components.isNotEmpty() && kotlin.math.abs(total - 100.0) > 0.000001) add("Assessment weights must total 100% (currently ${"%.2f".format(Locale.ROOT, total)}%)")
        if (scheme.combinationMethod.name == "BEST_N_OF_M" && scheme.bestN > scheme.components.size && scheme.components.isNotEmpty()) add("Best-N cannot exceed the number of configured components")
    }
}
