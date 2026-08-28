package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM audio_notes WHERE isArchived = 0 ORDER BY isPinned DESC, createdAt DESC")
    fun getActiveNotes(): Flow<List<AudioNote>>

    @Query("SELECT * FROM audio_notes WHERE isArchived = 1 ORDER BY isPinned DESC, createdAt DESC")
    fun getArchivedNotes(): Flow<List<AudioNote>>

    @Query("SELECT * FROM audio_notes ORDER BY isPinned DESC, createdAt DESC")
    fun getAllNotes(): Flow<List<AudioNote>>

    @Query("SELECT * FROM audio_notes WHERE isFavorite = 1 AND isArchived = 0 ORDER BY isPinned DESC, createdAt DESC")
    fun getFavoriteNotes(): Flow<List<AudioNote>>

    @Query("SELECT * FROM audio_notes WHERE id = :id")
    fun getNoteById(id: Long): Flow<AudioNote?>

    @Query("SELECT * FROM audio_notes WHERE isArchived = 0 AND (title LIKE '%' || :query || '%' OR polishedText LIKE '%' || :query || '%' OR rawTranscript LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%') ORDER BY isPinned DESC, createdAt DESC")
    fun searchActiveNotes(query: String): Flow<List<AudioNote>>

    @Query("SELECT * FROM audio_notes WHERE isArchived = 1 AND (title LIKE '%' || :query || '%' OR polishedText LIKE '%' || :query || '%' OR rawTranscript LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%') ORDER BY isPinned DESC, createdAt DESC")
    fun searchArchivedNotes(query: String): Flow<List<AudioNote>>

    @Query("SELECT * FROM audio_notes WHERE (title LIKE '%' || :query || '%' OR polishedText LIKE '%' || :query || '%' OR rawTranscript LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%') ORDER BY isPinned DESC, createdAt DESC")
    fun searchNotes(query: String): Flow<List<AudioNote>>

    @Query("SELECT * FROM audio_notes WHERE isArchived = 0 AND tags LIKE '%' || :tag || '%' ORDER BY isPinned DESC, createdAt DESC")
    fun getNotesByTag(tag: String): Flow<List<AudioNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: AudioNote): Long

    @Update
    suspend fun updateNote(note: AudioNote)

    @Delete
    suspend fun deleteNote(note: AudioNote)

    @Query("DELETE FROM audio_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("DELETE FROM audio_notes WHERE isArchived = 1")
    suspend fun deleteAllArchivedNotes()

    @Query("UPDATE audio_notes SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE audio_notes SET isArchived = :isArchived WHERE id = :id")
    suspend fun setArchived(id: Long, isArchived: Boolean)

    @Query("UPDATE audio_notes SET isPinned = :isPinned WHERE id = :id")
    suspend fun setPinned(id: Long, isPinned: Boolean)

    @Query("UPDATE audio_notes SET tags = :tags WHERE id = :id")
    suspend fun updateTags(id: Long, tags: String)

    @Query("UPDATE audio_notes SET category = :category WHERE id = :id")
    suspend fun updateCategory(id: Long, category: String)

    @Query("UPDATE audio_notes SET actionChecklist = :actionChecklist WHERE id = :id")
    suspend fun updateActionChecklist(id: Long, actionChecklist: String)

    @Query("UPDATE audio_notes SET title = :title, polishedText = :polishedText, formatStyle = :formatStyle WHERE id = :id")
    suspend fun updatePolishedContent(id: Long, title: String, polishedText: String, formatStyle: String)

    // Bulk actions
    @Query("DELETE FROM audio_notes WHERE id IN (:ids)")
    suspend fun bulkDelete(ids: List<Long>)

    @Query("UPDATE audio_notes SET isArchived = :isArchived WHERE id IN (:ids)")
    suspend fun bulkSetArchived(ids: List<Long>, isArchived: Boolean)

    @Query("UPDATE audio_notes SET category = :category WHERE id IN (:ids)")
    suspend fun bulkSetCategory(ids: List<Long>, category: String)

    @Query("UPDATE audio_notes SET isPinned = :isPinned WHERE id IN (:ids)")
    suspend fun bulkSetPinned(ids: List<Long>, isPinned: Boolean)
}
