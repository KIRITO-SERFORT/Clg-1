package com.example.data.model

enum class TaskPriority(val label: String) {
    HIGH("HIGH"),
    MEDIUM("MEDIUM"),
    LOW("LOW");

    companion object {
        fun fromString(value: String?): TaskPriority {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: LOW
        }
    }
}

data class ChatTask(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val senderId: String = "",
    val senderRole: String = "Class Rep",
    val targetGroup: List<String> = emptyList(),
    val deadlineMillis: Long = System.currentTimeMillis(),
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val completions: Map<String, Boolean> = emptyMap()
) {
    fun isCompletedBy(userId: String): Boolean {
        return completions[userId] == true
    }
}
