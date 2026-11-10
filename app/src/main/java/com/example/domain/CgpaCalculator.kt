package com.example.domain

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Standalone SGPA / CGPA maths. Pure Kotlin (no Android types) so it is unit tested on a JVM.
 * All arithmetic is exact BigDecimal; rounding happens ONCE at the end, with the rounding rule the student picks,
 * because universities differ (round half up, round half even, or simply cut the digits off).
 * Nothing here assumes one university's formula: grade scales, the percentage factor and the
 * "round SGPA first?" behaviour are all inputs.
 */

enum class CgpaRounding(val label: String, val mode: RoundingMode) {
    HALF_UP("Round half up (8.125 becomes 8.13)", RoundingMode.HALF_UP),
    HALF_EVEN("Round half to even (8.125 becomes 8.12)", RoundingMode.HALF_EVEN),
    TRUNCATE("Cut off the extra digits (8.129 becomes 8.12)", RoundingMode.DOWN)
}

data class GradeEntry(val label: String, val point: BigDecimal)

data class GradeScale(val name: String, val entries: List<GradeEntry>) {
    val maxPoint: BigDecimal get() = entries.maxOfOrNull { it.point } ?: BigDecimal.ZERO
    fun pointOf(label: String): BigDecimal? = entries.firstOrNull { it.label.equals(label.trim(), ignoreCase = true) }?.point
}

/** Common examples only. Every college differs: the student can edit or build their own scale. */
object GradeScales {
    private fun e(l: String, p: String) = GradeEntry(l, BigDecimal(p))
    val TEN_POINT_U = GradeScale("O / A+ / A / B+ / B / C / U (0-10)", listOf(e("O", "10"), e("A+", "9"), e("A", "8"), e("B+", "7"), e("B", "6"), e("C", "5"), e("U", "0")))
    val TEN_POINT_PF = GradeScale("O / A+ / A / B+ / B / C / P / F (0-10)", listOf(e("O", "10"), e("A+", "9"), e("A", "8"), e("B+", "7"), e("B", "6"), e("C", "5"), e("P", "4"), e("F", "0")))
    val FOUR_POINT = GradeScale("A / B / C / D / F (0-4)", listOf(e("A", "4"), e("B", "3"), e("C", "2"), e("D", "1"), e("F", "0")))
    val PRESETS = listOf(TEN_POINT_U, TEN_POINT_PF, FOUR_POINT)
}

data class CalcSubject(val name: String, val credits: BigDecimal, val gradePoint: BigDecimal, val counts: Boolean = true)
data class CalcSemester(val name: String, val subjects: List<CalcSubject>)
data class QuickSemester(val name: String, val sgpa: BigDecimal, val credits: BigDecimal?)

data class SemesterResult(val name: String, val sgpa: BigDecimal?, val credits: BigDecimal, val points: BigDecimal)

data class CgpaResult(
    val cgpa: BigDecimal?, val totalCredits: BigDecimal, val semesters: List<SemesterResult>, val note: String?
)

data class TargetResult(val feasible: Boolean, val requiredAverage: BigDecimal?, val bestReachableCgpa: BigDecimal?, val closestGrade: String?, val message: String)

object CgpaCalc {
    const val MAX_CREDITS_PER_SUBJECT = 40

    private fun div(a: BigDecimal, b: BigDecimal, places: Int, r: CgpaRounding): BigDecimal =
        a.divide(b, places, r.mode)

    /** Credit-weighted SGPA of one semester: sum(credit x point) / sum(credit). Subjects with counts=false are skipped. */
    fun sgpa(subjects: List<CalcSubject>, places: Int = 2, rounding: CgpaRounding = CgpaRounding.HALF_UP): SemesterResult {
        val used = subjects.filter { it.counts }
        val credits = used.fold(BigDecimal.ZERO) { a, s -> a + s.credits }
        val points = used.fold(BigDecimal.ZERO) { a, s -> a + s.credits * s.gradePoint }
        val value = if (credits.signum() == 0) null else div(points, credits, places, rounding)
        return SemesterResult("", value, credits, points)
    }

    /**
     * CGPA from subject-level data: sum(credit x point) over ALL semesters / sum(credits).
     * [roundSgpaFirst] mimics universities that publish a rounded SGPA per semester and then weight THOSE by the
     * semester's credits; the answer can differ slightly from the exact method, so it is a choice, not a default.
     */
    fun cgpaFromSubjects(
        semesters: List<CalcSemester>, places: Int = 2, rounding: CgpaRounding = CgpaRounding.HALF_UP, roundSgpaFirst: Boolean = false
    ): CgpaResult {
        val per = semesters.map { sem -> sgpa(sem.subjects, places, rounding).copy(name = sem.name) }
        val totalCredits = per.fold(BigDecimal.ZERO) { a, s -> a + s.credits }
        if (totalCredits.signum() == 0) return CgpaResult(null, BigDecimal.ZERO, per, "Add at least one subject with credits.")
        val points = if (roundSgpaFirst) per.fold(BigDecimal.ZERO) { a, s -> a + (s.sgpa ?: BigDecimal.ZERO) * s.credits }
        else per.fold(BigDecimal.ZERO) { a, s -> a + s.points }
        return CgpaResult(div(points, totalCredits, places, rounding), totalCredits, per, null)
    }

    /**
     * CGPA from published SGPAs. With credits: credit-weighted. Without credits (or if [equalWeights]): plain average,
     * which is only right when every semester has the same credits, so the note says so.
     */
    fun cgpaFromSgpa(items: List<QuickSemester>, equalWeights: Boolean, places: Int = 2, rounding: CgpaRounding = CgpaRounding.HALF_UP): CgpaResult {
        if (items.isEmpty()) return CgpaResult(null, BigDecimal.ZERO, emptyList(), "Add at least one semester.")
        val useCredits = !equalWeights && items.all { it.credits != null && it.credits.signum() > 0 }
        val per = items.map { SemesterResult(it.name, it.sgpa, it.credits ?: BigDecimal.ZERO, it.sgpa * (it.credits ?: BigDecimal.ZERO)) }
        return if (useCredits) {
            val credits = per.fold(BigDecimal.ZERO) { a, s -> a + s.credits }
            val points = per.fold(BigDecimal.ZERO) { a, s -> a + s.points }
            CgpaResult(div(points, credits, places, rounding), credits, per, null)
        } else {
            val sum = items.fold(BigDecimal.ZERO) { a, s -> a + s.sgpa }
            CgpaResult(div(sum, BigDecimal(items.size), places, rounding), per.fold(BigDecimal.ZERO) { a, s -> a + s.credits }, per,
                "Plain average of the SGPAs. This matches your university only if every semester has the same credits.")
        }
    }

    /** Percentage = CGPA x factor, using the factor the student's university publishes. No factor means no percentage. */
    fun percentage(cgpa: BigDecimal?, factor: BigDecimal?, places: Int = 2, rounding: CgpaRounding = CgpaRounding.HALF_UP): BigDecimal? =
        if (cgpa == null || factor == null || factor.signum() <= 0) null else (cgpa * factor).setScale(places, rounding.mode)

    /**
     * Average grade point you need over the remaining credits to reach [target].
     * required = (target x (done + remaining) - current x done) / remaining
     */
    fun requiredAverage(
        currentCgpa: BigDecimal, creditsDone: BigDecimal, creditsRemaining: BigDecimal, target: BigDecimal,
        scale: GradeScale, places: Int = 2
    ): TargetResult {
        if (creditsRemaining.signum() <= 0) return TargetResult(false, null, null, null, "Enter the credits you still have to take.")
        if (creditsDone.signum() < 0) return TargetResult(false, null, null, null, "Credits done cannot be negative.")
        val max = scale.maxPoint
        val total = creditsDone + creditsRemaining
        val needed = (target * total - currentCgpa * creditsDone).divide(creditsRemaining, 10, RoundingMode.HALF_UP)
        val best = (currentCgpa * creditsDone + max * creditsRemaining).divide(total, places, RoundingMode.DOWN)
        if (needed.signum() <= 0) return TargetResult(true, BigDecimal.ZERO, best, null, "Already secured: even a 0 average on the remaining credits keeps you at or above ${target.stripTrailingZeros().toPlainString()}.")
        if (needed > max) return TargetResult(false, needed.setScale(places, RoundingMode.UP), best, null,
            "Not reachable: you would need an average of ${needed.setScale(places, RoundingMode.UP).toPlainString()} but the highest grade point is ${max.stripTrailingZeros().toPlainString()}. The best you can reach is ${best.toPlainString()}.")
        val grade = scale.entries.filter { it.point >= needed }.minByOrNull { it.point }
        val req = needed.setScale(places, RoundingMode.UP)
        return TargetResult(true, req, best, grade?.label,
            "You need an average grade point of at least ${req.toPlainString()} on the remaining ${creditsRemaining.stripTrailingZeros().toPlainString()} credits" +
                (grade?.let { " (about ${it.label} in every subject)" } ?: "") + ". This is arithmetic, not a prediction.")
    }

    /** Parses a credit or point typed by the student; null when empty or invalid. Accepts a comma as the decimal mark. */
    fun parseNumber(text: String): BigDecimal? {
        val t = text.trim().replace(',', '.')
        if (t.isEmpty() || t.length > 8 || !t.all { it.isDigit() || it == '.' } || t.count { it == '.' } > 1) return null
        return t.toBigDecimalOrNull()
    }

    fun validCredits(c: BigDecimal?) = c != null && c.signum() > 0 && c <= BigDecimal(MAX_CREDITS_PER_SUBJECT)
}
