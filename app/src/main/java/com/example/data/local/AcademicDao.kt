package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AcademicDao {
    @Query("SELECT * FROM academic_profile WHERE id = 1")
    suspend fun getProfile(): AcademicProfileEntity?

    @Query("SELECT * FROM academic_periods ORDER BY orderIndex")
    suspend fun getPeriods(): List<AcademicPeriodEntity>

    @Query("SELECT * FROM academic_subjects ORDER BY name")
    suspend fun getSubjects(): List<AcademicSubjectEntity>

    @Query("SELECT * FROM assessment_marks ORDER BY subjectId, sequence")
    suspend fun getAssessmentMarks(): List<AssessmentMarkEntity>

    @Query("SELECT * FROM academic_exams ORDER BY dateMillis")
    suspend fun getExams(): List<AcademicExamEntity>

    @Query("SELECT * FROM academic_term_snapshots ORDER BY capturedAtMillis DESC")
    suspend fun getSnapshots(): List<AcademicSnapshotEntity>

    @Query("SELECT * FROM academic_audit ORDER BY createdAtMillis DESC")
    suspend fun getAudits(): List<AcademicAuditEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putSnapshots(values: List<AcademicSnapshotEntity>)

    @Query("SELECT * FROM academic_term_snapshots ORDER BY capturedAtMillis DESC")
    fun observeSnapshots(): Flow<List<AcademicSnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSnapshot(snapshot: AcademicSnapshotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudit(audit: AcademicAuditEntity)

    @Query("DELETE FROM academic_periods") suspend fun clearPeriods()
    @Query("DELETE FROM academic_subjects") suspend fun clearSubjects()
    @Query("DELETE FROM assessment_marks") suspend fun clearMarks()
    @Query("DELETE FROM academic_exams") suspend fun clearExams()
    @Query("DELETE FROM academic_profile") suspend fun clearProfile()
    @Query("DELETE FROM academic_term_snapshots") suspend fun clearSnapshots()
    @Query("DELETE FROM academic_audit") suspend fun clearAudits()

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putProfile(value: AcademicProfileEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putPeriods(values: List<AcademicPeriodEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putSubjects(values: List<AcademicSubjectEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putMarks(values: List<AssessmentMarkEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putExams(values: List<AcademicExamEntity>)

    /** All records are replaced in one transaction so the database cannot show a half-written workspace. */
    @Transaction
    suspend fun replaceSnapshots(values: List<AcademicSnapshotEntity>) {
        clearSnapshots()
        if (values.isNotEmpty()) putSnapshots(values)
    }

    @Transaction
    suspend fun clearWorkspace() {
        clearMarks(); clearSubjects(); clearPeriods(); clearExams(); clearProfile(); clearSnapshots(); clearAudits()
    }

    @Transaction
    suspend fun replaceWorkspace(
        profile: AcademicProfileEntity,
        periods: List<AcademicPeriodEntity>,
        subjects: List<AcademicSubjectEntity>,
        marks: List<AssessmentMarkEntity>,
        exams: List<AcademicExamEntity>,
        audit: AcademicAuditEntity
    ) {
        clearMarks(); clearSubjects(); clearPeriods(); clearExams(); clearProfile()
        putProfile(profile)
        if (periods.isNotEmpty()) putPeriods(periods)
        if (subjects.isNotEmpty()) putSubjects(subjects)
        if (marks.isNotEmpty()) putMarks(marks)
        if (exams.isNotEmpty()) putExams(exams)
        insertAudit(audit)
    }
}
