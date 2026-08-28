package com.example.data.repository

import com.example.data.local.AudioNote
import com.example.data.local.NoteDao
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {
    val allNotes: Flow<List<AudioNote>> = noteDao.getAllNotes()
    val activeNotes: Flow<List<AudioNote>> = noteDao.getActiveNotes()
    val archivedNotes: Flow<List<AudioNote>> = noteDao.getArchivedNotes()
    val favoriteNotes: Flow<List<AudioNote>> = noteDao.getFavoriteNotes()

    fun getNoteById(id: Long): Flow<AudioNote?> = noteDao.getNoteById(id)

    fun searchNotes(query: String, searchArchivedOnly: Boolean = false): Flow<List<AudioNote>> {
        return if (searchArchivedOnly) {
            noteDao.searchArchivedNotes(query)
        } else {
            noteDao.searchActiveNotes(query)
        }
    }

    suspend fun insertNote(note: AudioNote): Long = noteDao.insertNote(note)

    suspend fun updateNote(note: AudioNote) = noteDao.updateNote(note)

    suspend fun deleteNote(note: AudioNote) = noteDao.deleteNote(note)

    suspend fun deleteNoteById(id: Long) = noteDao.deleteNoteById(id)

    suspend fun deleteAllArchived() = noteDao.deleteAllArchivedNotes()

    suspend fun toggleFavorite(id: Long, currentStatus: Boolean) {
        noteDao.setFavorite(id, !currentStatus)
    }

    suspend fun setFavorite(id: Long, isFavorite: Boolean) {
        noteDao.setFavorite(id, isFavorite)
    }

    suspend fun toggleArchive(id: Long, currentStatus: Boolean) {
        noteDao.setArchived(id, !currentStatus)
    }

    suspend fun setArchived(id: Long, isArchived: Boolean) {
        noteDao.setArchived(id, isArchived)
    }

    suspend fun togglePinned(id: Long, currentStatus: Boolean) {
        noteDao.setPinned(id, !currentStatus)
    }

    suspend fun setPinned(id: Long, isPinned: Boolean) {
        noteDao.setPinned(id, isPinned)
    }

    suspend fun updateTags(id: Long, tags: String) {
        noteDao.updateTags(id, tags)
    }

    suspend fun updateCategory(id: Long, category: String) {
        noteDao.updateCategory(id, category)
    }

    suspend fun updateActionChecklist(id: Long, actionChecklist: String) {
        noteDao.updateActionChecklist(id, actionChecklist)
    }

    fun getNotesByTag(tag: String): Flow<List<AudioNote>> {
        return noteDao.getNotesByTag(tag)
    }

    suspend fun updatePolishedContent(id: Long, title: String, polishedText: String, formatStyle: String) {
        noteDao.updatePolishedContent(id, title, polishedText, formatStyle)
    }

    // Bulk actions
    suspend fun bulkDelete(ids: List<Long>) {
        if (ids.isNotEmpty()) {
            noteDao.bulkDelete(ids)
        }
    }

    suspend fun bulkSetArchived(ids: List<Long>, isArchived: Boolean) {
        if (ids.isNotEmpty()) {
            noteDao.bulkSetArchived(ids, isArchived)
        }
    }

    suspend fun bulkSetCategory(ids: List<Long>, category: String) {
        if (ids.isNotEmpty()) {
            noteDao.bulkSetCategory(ids, category)
        }
    }

    suspend fun bulkSetPinned(ids: List<Long>, isPinned: Boolean) {
        if (ids.isNotEmpty()) {
            noteDao.bulkSetPinned(ids, isPinned)
        }
    }
}
