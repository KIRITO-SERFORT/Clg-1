package com.example.data.analytics

import android.util.Log
import com.example.data.model.AttendanceSummary
import com.example.data.model.SubjectAttendance
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

private const val TAG = "AiAcademicAdvisor"

data class AiAnalyticsAdvice(
    val summary: String,
    val riskLevel: String, // "Low Risk", "Moderate", "Critical Alert"
    val safeSkipsRemaining: Int,
    val recoveryPlan: String,
    val subjectTips: List<SubjectTip>,
    val motivationalInsight: String
)

data class SubjectTip(
    val subjectCode: String,
    val title: String,
    val advice: String,
    val priority: String // "Urgent", "Watch", "Safe"
)

sealed interface AiAnalyticsUiState {
    data object Idle : AiAnalyticsUiState
    data object AdRequired : AiAnalyticsUiState
    data object AdPlaying : AiAnalyticsUiState
    data object Loading : AiAnalyticsUiState
    data class Success(val adviceText: String, val timestamp: Long = System.currentTimeMillis()) : AiAnalyticsUiState
    data class Error(val message: String) : AiAnalyticsUiState
}

class AiAcademicAdvisorService {

    // Lazy initialization of Firebase AI Logic model as mandated by guidelines
    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.4f
                topK = 40
            }
        )
    }

    suspend fun generateAcademicStrategy(
        summary: AttendanceSummary,
        subjects: List<SubjectAttendance>,
        targetThreshold: Double,
        studentName: String,
        collegeName: String
    ): String {
        return try {
            val prompt = buildAdvisorPrompt(summary, subjects, targetThreshold, studentName, collegeName)
            // Mandated: realistic coroutine timeout between 30 and 60 seconds
            val response = withTimeout(55_000L) {
                model.generateContent(prompt)
            }
            val text = response.text
            if (!text.isNullOrBlank()) {
                text.trim()
            } else {
                throw IllegalStateException("Gemini returned an empty academic recommendation.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini AI analysis timed out", e)
            throw IllegalStateException("AI analysis timed out after 55 seconds. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini AI generation failed: ${e.message}", e)
            throw e
        }
    }

    private fun buildAdvisorPrompt(
        summary: AttendanceSummary,
        subjects: List<SubjectAttendance>,
        targetThreshold: Double,
        studentName: String,
        collegeName: String
    ): String {
        val subjectDetails = if (subjects.isEmpty()) {
            "No individual subjects recorded yet. General academic term."
        } else {
            subjects.joinToString("\n") { s ->
                "- [${s.code}] ${s.title}: Attended ${s.presentHours}/${s.totalWorking} hrs (${String.format("%.1f", s.percentage)}%). Faculty: ${s.facultyName.ifBlank { "N/A" }}"
            }
        }

        return """
            You are the Chief Academic Strategist and AI Mentor for "Clg 1%" — a high-performance student app.
            Analyze the following student's attendance records and deliver a tactical, actionable, and encouraging Academic & Attendance Strategy Report.

            Student Details:
            - Name: $studentName
            - College: $collegeName
            - Overall Attendance: ${summary.present}/${summary.marked} (${String.format("%.1f", summary.percentage)}%)
            - Target Critical Threshold: ${String.format("%.1f", targetThreshold)}%
            - Total Working Hours Till Date: ${summary.totalWorking}
            - Current Buffer / Deficit: ${if (summary.percentage >= targetThreshold) "Safe buffer above target" else "BELOW target threshold"}

            Subject Schedule Breakdown:
            $subjectDetails

            Please structure your response with clear, visually appealing sections:
            1. 📊 Executive Academic Diagnosis (Current standing, safety status, and margin of safety)
            2. 🎯 Tactical Attendance Strategy (Which classes are safe to skip if needed, and which classes require zero-absence discipline)
            3. ⚡ Exam & Recovery Action Plan (Practical steps to maximize internal marks and prevent condonation / debarment)
            4. 🚀 "Top 1%" Mindset Advice (Concise motivational insight for peak academic performance)

            Keep it concise, clear, and high-impact. Use bullet points and clear formatting.
        """.trimIndent()
    }
}
