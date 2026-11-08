package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreUserRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun currentUserId(): String? = auth.currentUser?.uid

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing cloud Firestore.")
    }

    fun observeProfile(): Flow<UserProfile?> = flow {
        val uid = currentUserId() ?: return@flow
        val docRef = db.collection("users").document(uid)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) snapshot.toObject(UserProfile::class.java) else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w("FirestoreUserRepo", "Failed to observe profile: ${error.message}")
                    throw error
                }
        )
    }

    suspend fun getProfile(): UserProfile? {
        val uid = currentUserId() ?: return null
        val docRef = db.collection("users").document(uid)
        return try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) snapshot.toObject(UserProfile::class.java) else null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("FirestoreUserRepo", "Failed to fetch profile: ${e.message}")
            null
        }
    }

    suspend fun saveProfile(name: String, collegeName: String, batch: String, isPlus: Boolean = false) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val snapshot = try { docRef.get().await() } catch (_: Exception) { null }
        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "name" to name.trim(),
            "collegeName" to collegeName.trim(),
            "batch" to batch.trim(),
            "isPlus" to isPlus,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (snapshot == null || !snapshot.exists()) {
            payload["createdAt"] = FieldValue.serverTimestamp()
        } else {
            val existingCreatedAt = snapshot.getTimestamp("createdAt")
            if (existingCreatedAt != null) {
                payload["createdAt"] = existingCreatedAt
            } else {
                payload["createdAt"] = FieldValue.serverTimestamp()
            }
        }

        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("FirestoreUserRepo", "Error saving profile to Firestore: ${e.message}", e)
            throw e
        }
    }
}
