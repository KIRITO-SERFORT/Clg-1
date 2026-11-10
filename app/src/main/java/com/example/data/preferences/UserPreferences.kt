package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AttendanceSummary
import com.example.data.model.SubjectAttendance
import com.example.data.model.Timetable
import com.example.data.security.SecretCipher
import com.example.util.DayOrderAnchor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "acgcet_widtz_prefs")

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

class UserPreferences(private val context: Context) {

    companion object {
        private val KEY_USERNAME = stringPreferencesKey("key_student_username")
        // LEGACY plaintext password (older versions). Read once for migration, then removed.
        private val KEY_PASSWORD = stringPreferencesKey("key_student_password")
        // Keystore-encrypted password (see SecretCipher).
        private val KEY_PASSWORD_ENC = stringPreferencesKey("key_student_password_enc")
        private val KEY_STUDENT_NAME = stringPreferencesKey("key_student_name")
        private val KEY_THRESHOLD = doublePreferencesKey("key_target_threshold")
        private val KEY_CACHED_JSON = stringPreferencesKey("key_cached_attendance_json")
        private val KEY_CACHED_SUBJECTS_JSON = stringPreferencesKey("key_cached_subjects_json")
        private val KEY_SYNC_INTERVAL = intPreferencesKey("key_sync_interval_hours")
        private val KEY_THEME_MODE = stringPreferencesKey("key_theme_mode")
        private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("key_notifications_enabled")
        private val KEY_TIMETABLE_JSON = stringPreferencesKey("key_cached_timetable_json")
        private val KEY_DAY_ORDER_ANCHOR = stringPreferencesKey("key_day_order_anchor")
        private val KEY_COLLEGE_NAME = stringPreferencesKey("key_college_name")
        private val KEY_BATCH = stringPreferencesKey("key_student_batch")
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("key_onboarding_completed")
    }

    val usernameFlow: Flow<String?> = context.userPreferencesDataStore.data.map { prefs ->
        prefs[KEY_USERNAME]
    }

    val studentNameFlow: Flow<String> = context.userPreferencesDataStore.data.map { prefs ->
        prefs[KEY_STUDENT_NAME] ?: ""
    }

    val collegeNameFlow: Flow<String> = context.userPreferencesDataStore.data.map { prefs ->
        prefs[KEY_COLLEGE_NAME] ?: ""
    }

    val batchFlow: Flow<String> = context.userPreferencesDataStore.data.map { prefs ->
        prefs[KEY_BATCH] ?: ""
    }

    val onboardingCompletedFlow: Flow<Boolean> = context.userPreferencesDataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] ?: false
    }

    val targetThresholdFlow: Flow<Double> = context.userPreferencesDataStore.data.map { prefs ->
        prefs[KEY_THRESHOLD] ?: 0.75
    }

    val cachedAttendanceFlow: Flow<AttendanceSummary?> = context.userPreferencesDataStore.data.map { prefs ->
        val json = prefs[KEY_CACHED_JSON]
        if (!json.isNullOrEmpty()) AttendanceSummary.fromJson(json) else null
    }

    val cachedSubjectsFlow: Flow<List<SubjectAttendance>> = context.userPreferencesDataStore.data.map { prefs ->
        val json = prefs[KEY_CACHED_SUBJECTS_JSON]
        if (!json.isNullOrEmpty()) SubjectAttendance.listFromJson(json) else emptyList()
    }

    val syncIntervalHoursFlow: Flow<Int> = context.userPreferencesDataStore.data.map { prefs ->
        prefs[KEY_SYNC_INTERVAL] ?: 4
    }

    val themeModeFlow: Flow<ThemeMode> = context.userPreferencesDataStore.data.map { prefs ->
        when (prefs[KEY_THEME_MODE]) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            "SYSTEM" -> ThemeMode.SYSTEM
            else -> ThemeMode.DARK
        }
    }

    val notificationsEnabledFlow: Flow<Boolean> = context.userPreferencesDataStore.data.map { prefs ->
        prefs[KEY_NOTIFICATIONS_ENABLED] ?: true
    }

    val cachedTimetableFlow: Flow<Timetable?> = context.userPreferencesDataStore.data.map { prefs ->
        val json = prefs[KEY_TIMETABLE_JSON]
        if (!json.isNullOrEmpty()) Timetable.fromJson(json) else null
    }

    val dayOrderAnchorFlow: Flow<DayOrderAnchor?> = context.userPreferencesDataStore.data.map { prefs ->
        val raw = prefs[KEY_DAY_ORDER_ANCHOR]
        if (!raw.isNullOrEmpty()) DayOrderAnchor.parse(raw) else null
    }

    /**
     * Returns (registerNumber, password), or null if not logged in or the secret cannot be read
     * (the caller then asks the user to log in again).
     *
     * MIGRATION: versions before Clg 1% stored the password as plaintext. If only the legacy value
     * exists, it is encrypted, verified by decrypting it again, and only THEN is the plaintext removed.
     * If the Keystore is unavailable, the legacy value is kept so the user is not logged out unexpectedly.
     */
    suspend fun getCredentials(): Pair<String, String>? {
        val prefs = context.userPreferencesDataStore.data.first()
        val user = prefs[KEY_USERNAME]
        if (user.isNullOrEmpty()) return null

        val encrypted = prefs[KEY_PASSWORD_ENC]
        if (!encrypted.isNullOrEmpty()) {
            val pass = SecretCipher.decrypt(encrypted)
            return if (!pass.isNullOrEmpty()) Pair(user, pass) else null
        }

        val legacy = prefs[KEY_PASSWORD]
        if (legacy.isNullOrEmpty()) return null

        val blob = runCatching { SecretCipher.encrypt(legacy) }.getOrNull()
        if (blob != null && SecretCipher.decrypt(blob) == legacy) {
            context.userPreferencesDataStore.edit { p ->
                p[KEY_PASSWORD_ENC] = blob
                p.remove(KEY_PASSWORD)
            }
        }
        return Pair(user, legacy)
    }

    /** Fails closed: if secure storage is unavailable the password is NOT stored in plaintext. */
    suspend fun saveCredentials(username: String, password: String) {
        val blob = try {
            SecretCipher.encrypt(password)
        } catch (e: Exception) {
            throw IllegalStateException("Secure storage is unavailable on this device, so the password was not saved.")
        }
        context.userPreferencesDataStore.edit { prefs ->
            prefs[KEY_USERNAME] = username.trim()
            prefs[KEY_PASSWORD_ENC] = blob
            prefs.remove(KEY_PASSWORD)
        }
    }

    suspend fun saveStudentName(name: String) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[KEY_STUDENT_NAME] = name
        }
    }

    suspend fun saveCollegeName(college: String) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[KEY_COLLEGE_NAME] = college.trim()
        }
    }

    suspend fun saveBatch(batch: String) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[KEY_BATCH] = batch.trim()
        }
    }

    suspend fun saveUserProfile(name: String, college: String, batch: String) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[KEY_STUDENT_NAME] = name.trim()
            prefs[KEY_COLLEGE_NAME] = college.trim()
            prefs[KEY_BATCH] = batch.trim()
            prefs[KEY_ONBOARDING_COMPLETED] = true
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean = true) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun saveCachedAttendance(summary: AttendanceSummary) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[KEY_CACHED_JSON] = summary.toJson()
        }
    }

    suspend fun saveCachedSubjects(subjects: List<SubjectAttendance>) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[KEY_CACHED_SUBJECTS_JSON] = SubjectAttendance.listToJson(subjects)
        }
    }

    suspend fun saveTargetThreshold(threshold: Double) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[KEY_THRESHOLD] = threshold
        }
    }

    suspend fun saveSyncInterval(hours: Int) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[KEY_SYNC_INTERVAL] = hours
        }
    }

    suspend fun saveThemeMode(mode: ThemeMode) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
        }
    }

    suspend fun saveNotificationsEnabled(enabled: Boolean) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun saveCachedTimetable(timetable: Timetable) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[KEY_TIMETABLE_JSON] = timetable.toJson()
        }
    }

    suspend fun saveDayOrderAnchor(anchor: DayOrderAnchor) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[KEY_DAY_ORDER_ANCHOR] = anchor.serialize()
        }
    }

    suspend fun clearAll() {
        context.userPreferencesDataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
