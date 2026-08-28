package com.example.data.model

enum class NoteCategory(
    val id: String,
    val displayName: String,
    val emoji: String
) {
    ALL("ALL", "Todas", "📋"),
    IDEA("Idea", "Ideias", "💡"),
    WORK("Work", "Trabalho", "💼"),
    PERSONAL("Personal", "Pessoal", "👤"),
    PROJECT("Project", "Projetos", "🎯"),
    STUDY("Study", "Estudos", "📚"),
    OTHER("Other", "Outros", "🏷️");

    companion object {
        fun fromId(id: String?): NoteCategory {
            if (id.isNullOrBlank()) return IDEA
            return entries.find { it.id.equals(id, ignoreCase = true) || it.name.equals(id, ignoreCase = true) } ?: IDEA
        }

        fun fromString(str: String?): NoteCategory = fromId(str)
    }
}
