package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Normalized local academic workspace. No student identity or ERP credential columns exist here. */
@Entity(tableName = "academic_profile")
data class AcademicProfileEntity(
    @PrimaryKey val id: Int = 1,
    val college: String,
    val termName: String,
    val academicYear: String,
    val configurationJson: String,
    val updatedAtMillis: Long
)

@Entity(tableName = "academic_periods", indices = [Index("profileId")])
data class AcademicPeriodEntity(
    @PrimaryKey val id: String,
    val profileId: Int = 1,
    val name: String,
    val orderIndex: Int,
    val startsAtMillis: Long?,
    val endsAtMillis: Long?,
    val examAtMillis: Long?
)

@Entity(tableName = "academic_subjects", indices = [Index("termId")])
data class AcademicSubjectEntity(
    @PrimaryKey val id: String,
    val code: String,
    val name: String,
    val credits: Double,
    val termId: String,
    val subjectPassPercent: Double?
)

@Entity(tableName = "assessment_marks", indices = [Index("subjectId")])
data class AssessmentMarkEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val name: String,
    val obtained: Double?,
    val maximum: Double,
    val weightPercent: Double,
    val status: String,
    val minimumPassPercent: Double?,
    val category: String,
    val dateEpochMillis: Long?,
    val source: String = "manual",
    val sequence: Int
)

@Entity(tableName = "academic_exams", indices = [Index("dateMillis")])
data class AcademicExamEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subject: String,
    val dateMillis: Long,
    val notes: String,
    val termId: String
)

@Entity(tableName = "academic_term_snapshots", indices = [Index("capturedAtMillis")])
data class AcademicSnapshotEntity(
    @PrimaryKey val id: String,
    val academicYear: String,
    val termName: String,
    val capturedAtMillis: Long,
    val subjectCount: Int,
    val completedSubjectCount: Int,
    val averagePercentage: Double?,
    val sgpa: Double?,
    val credits: Double,
    /** Only calculated summary data is stored; never the original ERP payload. */
    val subjectSummariesJson: String
)

@Entity(tableName = "academic_audit", indices = [Index("createdAtMillis")])
data class AcademicAuditEntity(
    @PrimaryKey val id: String,
    val recordId: String,
    val action: String,
    val createdAtMillis: Long,
    val detail: String
)
