package com.example.domain

import java.math.BigDecimal

/** Editable state of the CGPA calculator screen (all numbers are the raw text the student typed). Pure Kotlin. */
data class SubjectRow(val id: String, val name: String = "", val credits: String = "", val grade: String = "", val counts: Boolean = true)
data class SemesterBlock(val id: String, val name: String, val subjects: List<SubjectRow>)
data class QuickRow(val id: String, val name: String, val sgpa: String = "", val credits: String = "")
data class GradeRow(val id: String, val label: String, val point: String)
data class TargetInputs(val current: String = "", val done: String = "", val remaining: String = "", val target: String = "")

data class CgpaUiState(
    val mode: Int = 0,                                   // 0 = by subjects, 1 = quick (SGPAs), 2 = target
    val scaleName: String = GradeScales.TEN_POINT_U.name,
    val grades: List<GradeRow> = gradeRowsOf(GradeScales.TEN_POINT_U),
    val semesters: List<SemesterBlock> = listOf(SemesterBlock("s1", "Semester 1", listOf(SubjectRow("r1"), SubjectRow("r2"), SubjectRow("r3")))),
    val quick: List<QuickRow> = listOf(QuickRow("q1", "Semester 1")),
    val equalWeights: Boolean = false,
    val roundSgpaFirst: Boolean = false,
    val rounding: CgpaRounding = CgpaRounding.HALF_UP,
    val places: Int = 2,
    val factor: String = "",
    val target: TargetInputs = TargetInputs()
)

fun gradeRowsOf(scale: GradeScale) = scale.entries.mapIndexed { i, e -> GradeRow("g$i", e.label, e.point.stripTrailingZeros().toPlainString()) }

data class Evaluation(
    val scale: GradeScale,
    val subjects: CgpaResult,
    val quick: CgpaResult,
    val target: TargetResult?,
    val percent: BigDecimal?,
    /** Row id -> why that row is NOT counted. Rows are never dropped silently. */
    val rowIssues: Map<String, String>,
    val warnings: List<String>,
    val prefillCurrent: BigDecimal?,
    val prefillCredits: BigDecimal?
)

object CgpaEvaluator {
    fun scaleOf(s: CgpaUiState): Pair<GradeScale, List<String>> {
        val warns = ArrayList<String>()
        val entries = s.grades.mapNotNull { g ->
            val p = CgpaCalc.parseNumber(g.point)
            if (g.label.isBlank() && g.point.isBlank()) null
            else if (g.label.isBlank() || p == null) { warns += "A grade in your scale is incomplete (needs a label and a point), so it is ignored."; null }
            else GradeEntry(g.label.trim(), p)
        }
        val dupes = entries.map { it.label.lowercase() }.let { it.size - it.toSet().size }
        if (dupes > 0) warns += "Two grades share a label; the first one is used."
        return GradeScale(s.scaleName, entries) to warns
    }

    private fun isBlank(r: SubjectRow) = r.name.isBlank() && r.credits.isBlank() && r.grade.isBlank()

    fun evaluate(s: CgpaUiState): Evaluation {
        val (scale, scaleWarns) = scaleOf(s)
        val warnings = ArrayList(scaleWarns)
        val issues = HashMap<String, String>()      // rows of the tab the student is looking at
        val hiddenIssues = HashMap<String, String>()  // rows of the other tab: kept out of the warnings

        val sems = s.semesters.map { sem ->
            val subs = sem.subjects.mapNotNull { r ->
                if (isBlank(r)) return@mapNotNull null
                val credits = CgpaCalc.parseNumber(r.credits)
                val gp = scale.pointOf(r.grade)
                when {
                    !CgpaCalc.validCredits(credits) -> { (if (s.mode == 1) hiddenIssues else issues)[r.id] = "Credits must be a number above 0 and at most ${CgpaCalc.MAX_CREDITS_PER_SUBJECT}."; null }
                    gp == null -> { (if (s.mode == 1) hiddenIssues else issues)[r.id] = if (r.grade.isBlank()) "Pick a grade." else "Grade \"${r.grade}\" is not in your scale."; null }
                    else -> CalcSubject(r.name.ifBlank { "Subject" }, credits!!, gp, r.counts)
                }
            }
            CalcSemester(sem.name.ifBlank { "Semester" }, subs)
        }
        val subjectsResult = CgpaCalc.cgpaFromSubjects(sems, s.places, s.rounding, s.roundSgpaFirst)

        val quickItems = s.quick.mapNotNull { r ->
            // a name alone (the pre-filled "Semester 1") is not data
            if (r.sgpa.isBlank() && r.credits.isBlank()) return@mapNotNull null
            val sg = CgpaCalc.parseNumber(r.sgpa)
            val cr = if (r.credits.isBlank()) null else CgpaCalc.parseNumber(r.credits)
            when {
                sg == null || sg > scale.maxPoint -> { (if (s.mode == 1) issues else hiddenIssues)[r.id] = "SGPA must be a number from 0 to ${scale.maxPoint.stripTrailingZeros().toPlainString()}."; null }
                r.credits.isNotBlank() && !(cr != null && cr.signum() > 0 && cr <= BigDecimal(2000)) -> { (if (s.mode == 1) issues else hiddenIssues)[r.id] = "Credits must be a number above 0, or empty."; null }
                else -> QuickSemester(r.name.ifBlank { "Semester" }, sg, cr)
            }
        }
        val quickResult = CgpaCalc.cgpaFromSgpa(quickItems, s.equalWeights, s.places, s.rounding)

        val skipped = issues.size
        if (skipped > 0) warnings += "$skipped row(s) are incomplete or invalid and are NOT counted. Fix them to include them."

        val factor = CgpaCalc.parseNumber(s.factor)
        if (s.factor.isNotBlank() && factor == null) warnings += "The percentage factor is not a valid number, so no percentage is shown."
        val shown = if (s.mode == 1) quickResult else subjectsResult
        val percent = CgpaCalc.percentage(shown.cgpa, factor, 2, s.rounding)

        val t = s.target
        val target = run {
            val cur = CgpaCalc.parseNumber(t.current); val done = CgpaCalc.parseNumber(t.done)
            val rem = CgpaCalc.parseNumber(t.remaining); val goal = CgpaCalc.parseNumber(t.target)
            if (cur == null || done == null || rem == null || goal == null) null
            else if (cur > scale.maxPoint || goal > scale.maxPoint) TargetResult(false, null, null, null, "Current and target CGPA cannot be higher than the top grade point (${scale.maxPoint.stripTrailingZeros().toPlainString()}).")
            else CgpaCalc.requiredAverage(cur, done, rem, goal, scale, s.places)
        }
        return Evaluation(scale, subjectsResult, quickResult, target, percent, issues, warnings, shown.cgpa, shown.totalCredits.takeIf { it.signum() > 0 })
    }
}
