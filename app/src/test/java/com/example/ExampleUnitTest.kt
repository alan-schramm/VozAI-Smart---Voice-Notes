package com.example

import com.example.data.model.FormatStyle
import com.example.data.model.NoteCategory
import com.example.data.local.AudioNote
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun formatStyle_containsRequiredEditorialStyles() {
        val styles = FormatStyle.values().map { it.id }
        assertTrue(styles.contains("classic"))
        assertTrue(styles.contains("email"))
        assertTrue(styles.contains("bullet_memo"))
        assertTrue(styles.contains("social"))
        assertTrue(styles.contains("todo"))
    }

    @Test
    fun noteCategory_validationAndResolution() {
        assertEquals(NoteCategory.IDEA, NoteCategory.fromString("Idea"))
        assertEquals(NoteCategory.WORK, NoteCategory.fromString("Work"))
        assertEquals(NoteCategory.PERSONAL, NoteCategory.fromString("Personal"))
        assertEquals(NoteCategory.PROJECT, NoteCategory.fromString("Project"))
        assertEquals(NoteCategory.STUDY, NoteCategory.fromString("Study"))
        assertEquals(NoteCategory.OTHER, NoteCategory.fromString("Other"))
        assertEquals(NoteCategory.IDEA, NoteCategory.fromString("invalid_category"))
        assertEquals(NoteCategory.IDEA, NoteCategory.fromString(null))
    }

    @Test
    fun audioNote_creationAndWordCount() {
        val raw = "Today we planned the roadmap for the third quarter product launch."
        val polished = "Today, we planned the strategic product launch roadmap for Q3."
        val note = AudioNote(
            title = "Q3 Product Launch Roadmap",
            polishedText = polished,
            rawTranscript = raw,
            summary = "Summary of Q3 planning.",
            keyTakeaways = "Launch planned for Q3\nRoadmap finalized",
            formatStyle = "classic",
            category = "Work",
            audioDurationSeconds = 14,
            createdAt = System.currentTimeMillis(),
            wordCount = 10
        )

        assertEquals("Q3 Product Launch Roadmap", note.title)
        assertEquals("Work", note.category)
        assertEquals(10, note.wordCount)
        assertEquals(14, note.audioDurationSeconds)
        assertFalse(note.isFavorite)
        assertFalse(note.isArchived)
    }
}
