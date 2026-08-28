package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio_notes")
data class AudioNote(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val polishedText: String,
    val rawTranscript: String,
    val summary: String,
    val keyTakeaways: String, // Stored as newline or bullet separated string
    val formatStyle: String = "classic",
    val audioDurationSeconds: Int = 0,
    val audioFilePath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isPinned: Boolean = false,
    val tags: String = "",
    val category: String = "Idea",
    val actionChecklist: String = "", // Stored as newline-delimited "[ ] item" or "[x] item"
    val wordCount: Int = 0
) {
    fun getTagList(): List<String> {
        if (tags.isBlank()) return emptyList()
        return tags.split(",", ";", "\n")
            .map { it.trim().removePrefix("#").trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    fun hasTag(tagQuery: String): Boolean {
        val cleanQuery = tagQuery.trim().removePrefix("#").trim()
        return getTagList().any { it.equals(cleanQuery, ignoreCase = true) }
    }

    fun getChecklistItems(): List<ChecklistItem> {
        if (actionChecklist.isBlank()) return emptyList()
        return actionChecklist.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { line ->
                val isCompleted = line.startsWith("[x]", ignoreCase = true)
                val text = line.removePrefix("[x]").removePrefix("[X]").removePrefix("[ ]").trim()
                ChecklistItem(text = text, isCompleted = isCompleted)
            }
    }
}

data class ChecklistItem(
    val text: String,
    val isCompleted: Boolean = false
)
