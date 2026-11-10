package com.example.data.repository

import android.util.Log
import com.example.data.model.ChatTask
import com.example.data.model.TaskPriority
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

class ChatsRepository {

    private val db = Firebase.firestore
    private val chatsCollection = db.collection("chats")
    private val usersCollection = db.collection("users")

    fun currentUserId(): String? = Firebase.auth.currentUser?.uid

    /**
     * Fetches the current user's official role from Firestore (/users/{uid}.role).
     * Returns "Student" if not set or unauthenticated.
     */
    suspend fun getCurrentUserRole(): String {
        val uid = currentUserId() ?: return "Student"
        return try {
            val doc = usersCollection.document(uid).get().await()
            doc.getString("role")?.takeIf { it.isNotBlank() } ?: "Student"
        } catch (e: Exception) {
            Log.w("ChatsRepository", "Failed to fetch user role: ${e.message}")
            "Student"
        }
    }

    /**
     * Updates the current user's role in Firestore (/users/{uid}.role) so no manual Console edit is needed.
     */
    suspend fun setCurrentUserRole(role: String): Result<Unit> {
        val uid = currentUserId() ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            usersCollection.document(uid)
                .set(
                    mapOf("role" to role, "updatedAt" to FieldValue.serverTimestamp()),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Real-time stream of official CHATS tasks/announcements ordered by deadline.
     */
    fun observeChats(): Flow<List<ChatTask>> = callbackFlow {
        val registration = chatsCollection
            .orderBy("deadline", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("ChatsRepository", "Chats listener error: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val tasks = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val deadlineTs = doc.getTimestamp("deadline")
                        val createdTs = doc.getTimestamp("createdAt")
                        @Suppress("UNCHECKED_CAST")
                        val rawGroups = doc.get("targetGroup") as? List<String> ?: emptyList()
                        @Suppress("UNCHECKED_CAST")
                        val rawCompletions = doc.get("completions") as? Map<String, Boolean> ?: emptyMap()

                        ChatTask(
                            id = doc.id,
                            title = doc.getString("title").orEmpty(),
                            description = doc.getString("description").orEmpty(),
                            senderId = doc.getString("senderId").orEmpty(),
                            senderRole = doc.getString("senderRole") ?: "Class Rep",
                            targetGroup = rawGroups,
                            deadlineMillis = deadlineTs?.toDate()?.time ?: System.currentTimeMillis(),
                            priority = TaskPriority.fromString(doc.getString("priority")),
                            createdAtMillis = createdTs?.toDate()?.time ?: System.currentTimeMillis(),
                            completions = rawCompletions
                        )
                    } catch (e: Exception) {
                        Log.w("ChatsRepository", "Skipping malformed chat doc ${doc.id}: ${e.message}")
                        null
                    }
                } ?: emptyList()
                trySend(tasks)
            }

        awaitClose { registration.remove() }
    }

    /**
     * Creates a new official task/message in /chats/{taskId}.
     * Enforced by Firestore security rules to only succeed for Class Rep or Club Leader.
     */
    suspend fun createChatTask(
        title: String,
        description: String,
        senderRole: String,
        targetGroup: List<String>,
        deadlineMillis: Long,
        priority: TaskPriority
    ): Result<String> {
        val uid = currentUserId()
            ?: return Result.failure(IllegalStateException("Please sign in with Google to post official tasks."))

        return try {
            val docRef = chatsCollection.document()
            val payload = hashMapOf(
                "title" to title.trim(),
                "description" to description.trim(),
                "senderId" to uid,
                "senderRole" to senderRole,
                "targetGroup" to targetGroup,
                "deadline" to Timestamp(Date(deadlineMillis)),
                "priority" to priority.name,
                "createdAt" to FieldValue.serverTimestamp(),
                "completions" to hashMapOf(uid to false)
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("ChatsRepository", "Failed to create chat task", e)
            Result.failure(e)
        }
    }

    /**
     * Immediately toggles the user's completion status flag (completions.<userId> = true/false) in Firestore.
     */
    suspend fun toggleTaskCompletion(taskId: String, completed: Boolean): Result<Unit> {
        val uid = currentUserId()
            ?: return Result.failure(IllegalStateException("Sign in with Google to track task completion."))

        return try {
            chatsCollection.document(taskId)
                .update("completions.$uid", completed)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ChatsRepository", "Failed to toggle completion for $taskId", e)
            Result.failure(e)
        }
    }
}
