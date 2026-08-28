package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayerManager
import com.example.audio.AudioRecorderManager
import com.example.audio.RealtimeAudioToTextService
import com.example.audio.RecorderState
import com.example.audio.TextToSpeechManager
import com.example.data.local.AppDatabase
import com.example.data.local.AudioNote
import com.example.data.local.ChecklistItem
import com.example.data.model.ChatMessage
import com.example.data.model.FormatStyle
import com.example.data.model.GroundingMetadata
import com.example.data.model.GroundingSource
import com.example.data.model.MessageSender
import com.example.data.model.NoteEnrichmentResult
import com.example.data.remote.GeminiProcessResult
import com.example.data.remote.GeminiService
import com.example.data.repository.ChatRepository
import com.example.data.repository.NoteRepository
import com.example.util.ExportManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class AppScreen {
    RECORD,
    PROCESSING,
    RESULT,
    HISTORY,
    AGENT
}

data class ActiveNoteState(
    val id: Long = 0,
    val title: String = "",
    val polishedText: String = "",
    val rawTranscript: String = "",
    val summary: String = "",
    val keyTakeaways: List<String> = emptyList(),
    val formatStyle: FormatStyle = FormatStyle.CLASSIC,
    val audioDurationSeconds: Int = 0,
    val audioFilePath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isPinned: Boolean = false,
    val tags: String = "",
    val category: String = "Idea",
    val actionChecklist: List<ChecklistItem> = emptyList(),
    val isSavedToDb: Boolean = false
) {
    fun getTagList(): List<String> {
        if (tags.isBlank()) return emptyList()
        return tags.split(",", ";", "\n")
            .map { it.trim().removePrefix("#").trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    fun toAudioNote(): AudioNote {
        val count = polishedText.split("\\s+".toRegex()).count { it.isNotBlank() }
        val checklistStr = actionChecklist.joinToString("\n") { (if (it.isCompleted) "[x] " else "[ ] ") + it.text }
        return AudioNote(
            id = id,
            title = title,
            polishedText = polishedText,
            rawTranscript = rawTranscript,
            summary = summary,
            keyTakeaways = keyTakeaways.joinToString("\n"),
            formatStyle = formatStyle.id,
            audioDurationSeconds = audioDurationSeconds,
            audioFilePath = audioFilePath,
            createdAt = createdAt,
            isFavorite = isFavorite,
            isArchived = isArchived,
            isPinned = isPinned,
            tags = tags,
            category = category,
            actionChecklist = checklistStr,
            wordCount = count
        )
    }
}

class AudioPenViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = NoteRepository(database.noteDao())
    private val chatRepository = ChatRepository(database.chatDao())
    val recorderManager = AudioRecorderManager(application)
    val audioPlayerManager = AudioPlayerManager(application)
    val ttsManager = TextToSpeechManager(application)
    private val sharedPrefs = application.getSharedPreferences("audiopen_prefs", Context.MODE_PRIVATE)

    private val _customApiKey = MutableStateFlow(sharedPrefs.getString("custom_api_key", "") ?: "")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val geminiService = GeminiService(customApiKeyProvider = { _customApiKey.value })
    val audioToTextService = RealtimeAudioToTextService(application, geminiService)

    val liveTranscript: StateFlow<String> = audioToTextService.liveTranscript
    val isLiveListening: StateFlow<Boolean> = audioToTextService.isLiveListening

    val isSpeaking: StateFlow<Boolean> = ttsManager.isSpeaking
    val currentlySpeakingId: StateFlow<String?> = ttsManager.currentlySpeakingId
    val speechRate: StateFlow<Float> = ttsManager.speechRate

    private val _currentScreen = MutableStateFlow(AppScreen.RECORD)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _selectedFormat = MutableStateFlow(FormatStyle.CLASSIC)
    val selectedFormat: StateFlow<FormatStyle> = _selectedFormat.asStateFlow()

    private val _activeNote = MutableStateFlow<ActiveNoteState?>(null)
    val activeNote: StateFlow<ActiveNoteState?> = _activeNote.asStateFlow()

    private val _processingPhase = MutableStateFlow("Preparing audio stream...")
    val processingPhase: StateFlow<String> = _processingPhase.asStateFlow()

    private val _isReformatting = MutableStateFlow(false)
    val isReformatting: StateFlow<Boolean> = _isReformatting.asStateFlow()

    private val _isRepurposing = MutableStateFlow(false)
    val isRepurposing: StateFlow<Boolean> = _isRepurposing.asStateFlow()

    private val _isTranslating = MutableStateFlow(false)
    val isTranslating: StateFlow<Boolean> = _isTranslating.asStateFlow()

    private val _isGeneratingChecklist = MutableStateFlow(false)
    val isGeneratingChecklist: StateFlow<Boolean> = _isGeneratingChecklist.asStateFlow()

    private val _isSummarizingWithGemini = MutableStateFlow(false)
    val isSummarizingWithGemini: StateFlow<Boolean> = _isSummarizingWithGemini.asStateFlow()

    // --- BULK SELECTION MODE IN HISTORY ---
    private val _isBulkSelectionMode = MutableStateFlow(false)
    val isBulkSelectionMode: StateFlow<Boolean> = _isBulkSelectionMode.asStateFlow()

    private val _selectedNoteIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedNoteIds: StateFlow<Set<Long>> = _selectedNoteIds.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _historyFilter = MutableStateFlow<String>("ALL") // ALL, FAVORITES, ARCHIVED, CLASSIC, EMAIL, etc.
    val historyFilter: StateFlow<String> = _historyFilter.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String>("ALL") // ALL, Idea, Work, Personal, Project, Study, Other
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    val availableTags: StateFlow<List<String>> = repository.allNotes
        .map { notes ->
            notes.filter { !it.isArchived }
                .flatMap { it.getTagList() }
                .distinct()
                .sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    private val _showCustomPromptDialog = MutableStateFlow(false)
    val showCustomPromptDialog: StateFlow<Boolean> = _showCustomPromptDialog.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // --- AI AGENT CHAT & VOICE STATE ---
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = MessageSender.AGENT,
                text = "Olá! Sou o seu parceiro de ideias do AudioPen. Compartilhe pensamentos brutos, ideias de negócios, projetos ou dúvidas por texto ou áudio, e eu vou estruturar, questionar e expandir seu raciocínio com você!",
                suggestedFollowUps = listOf(
                    "💡 Validar uma ideia de negócio",
                    "🎯 Estruturar um projeto do zero",
                    "📝 Como transformar pensamentos em plano?"
                )
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAgentThinking = MutableStateFlow(false)
    val isAgentThinking: StateFlow<Boolean> = _isAgentThinking.asStateFlow()

    private val _isVoiceResponseEnabled = MutableStateFlow(true)
    val isVoiceResponseEnabled: StateFlow<Boolean> = _isVoiceResponseEnabled.asStateFlow()

    private val _isChatRecording = MutableStateFlow(false)
    val isChatRecording: StateFlow<Boolean> = _isChatRecording.asStateFlow()

    // --- GOOGLE SEARCH GROUNDING & NOTES CONTEXT STATE ---
    private val _isGoogleSearchEnabled = MutableStateFlow(true)
    val isGoogleSearchEnabled: StateFlow<Boolean> = _isGoogleSearchEnabled.asStateFlow()

    private val _isNotesContextEnabled = MutableStateFlow(false)
    val isNotesContextEnabled: StateFlow<Boolean> = _isNotesContextEnabled.asStateFlow()

    private val _noteEnrichments = MutableStateFlow<Map<Long, NoteEnrichmentResult>>(emptyMap())
    val noteEnrichments: StateFlow<Map<Long, NoteEnrichmentResult>> = _noteEnrichments.asStateFlow()

    private val _isEnrichingNote = MutableStateFlow(false)
    val isEnrichingNote: StateFlow<Boolean> = _isEnrichingNote.asStateFlow()

    private val _enrichingNoteId = MutableStateFlow<Long?>(null)
    val enrichingNoteId: StateFlow<Long?> = _enrichingNoteId.asStateFlow()

    init {
        viewModelScope.launch {
            chatRepository.allMessages.collect { list ->
                if (list.isEmpty()) {
                    val defaultWelcome = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        sender = MessageSender.AGENT,
                        text = "Olá! Sou o seu parceiro de ideias do AudioPen. Compartilhe pensamentos brutos, ideias de negócios, projetos ou dúvidas por texto ou áudio, e eu vou estruturar, questionar e expandir seu raciocínio com você!",
                        suggestedFollowUps = listOf(
                            "💡 Validar uma ideia de negócio",
                            "🎯 Estruturar um projeto do zero",
                            "📝 Como transformar pensamentos em plano?"
                        )
                    )
                    chatRepository.insertMessage(defaultWelcome)
                } else {
                    _chatMessages.value = list
                }
            }
        }
    }

    val allNotes: StateFlow<List<AudioNote>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeNotesCount: StateFlow<Int> = repository.activeNotes
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val archivedNotesCount: StateFlow<Int> = repository.archivedNotes
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val filteredNotes: StateFlow<List<AudioNote>> = combine(
        repository.allNotes,
        _searchQuery,
        _historyFilter,
        _selectedCategory,
        _selectedTag
    ) { notes, query, filter, category, tag ->
        val queryTrimmed = query.trim()
        val queriedList = if (queryTrimmed.isBlank()) {
            notes
        } else {
            val keywords = queryTrimmed.split("\\s+".toRegex()).filter { it.isNotBlank() }
            notes.filter { note ->
                val searchableText = buildString {
                    append(note.title).append(" ")
                    append(note.polishedText).append(" ")
                    append(note.rawTranscript).append(" ")
                    append(note.summary).append(" ")
                    append(note.keyTakeaways).append(" ")
                    append(note.category).append(" ")
                    append(note.tags)
                }
                keywords.all { kw -> searchableText.contains(kw, ignoreCase = true) }
            }
        }

        // Category filter
        val categoryFilteredList = if (category.equals("ALL", ignoreCase = true) || category.isBlank()) {
            queriedList
        } else {
            queriedList.filter { it.category.equals(category, ignoreCase = true) }
        }

        val tagFilteredList = if (tag.isNullOrBlank()) {
            categoryFilteredList
        } else {
            categoryFilteredList.filter { it.hasTag(tag) }
        }

        when (filter) {
            "ARCHIVED" -> tagFilteredList.filter { it.isArchived }
            "FAVORITES" -> tagFilteredList.filter { it.isFavorite && !it.isArchived }
            "ALL" -> tagFilteredList.filter { !it.isArchived }
            else -> tagFilteredList.filter { !it.isArchived && it.formatStyle.equals(filter, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun selectFormatStyle(style: FormatStyle) {
        _selectedFormat.value = style
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setHistoryFilter(filter: String) {
        _historyFilter.value = filter
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSelectedTag(tag: String?) {
        _selectedTag.value = if (_selectedTag.value == tag) null else tag
    }

    fun clearSelectedTag() {
        _selectedTag.value = null
    }

    fun updateNoteCategory(noteId: Long, newCategory: String) {
        viewModelScope.launch {
            repository.updateCategory(noteId, newCategory)
            if (_activeNote.value?.id == noteId) {
                _activeNote.value = _activeNote.value?.copy(category = newCategory)
            }
        }
    }

    fun updateActiveNoteCategory(newCategory: String) {
        val current = _activeNote.value ?: return
        _activeNote.value = current.copy(category = newCategory)
        if (current.id > 0) {
            viewModelScope.launch {
                repository.updateCategory(current.id, newCategory)
            }
        }
    }

    fun updateNoteTags(noteId: Long, newTags: String) {
        viewModelScope.launch {
            repository.updateTags(noteId, newTags)
            if (_activeNote.value?.id == noteId) {
                _activeNote.value = _activeNote.value?.copy(tags = newTags)
            }
        }
    }

    fun updateActiveNoteTags(newTags: String) {
        val current = _activeNote.value ?: return
        _activeNote.value = current.copy(tags = newTags)
        if (current.id > 0) {
            viewModelScope.launch {
                repository.updateTags(current.id, newTags)
            }
        }
    }

    fun addTagToActiveNote(newTag: String) {
        val current = _activeNote.value ?: return
        val clean = newTag.trim().removePrefix("#").trim()
        if (clean.isBlank()) return
        val currentTags = current.getTagList().toMutableList()
        if (!currentTags.any { it.equals(clean, ignoreCase = true) }) {
            currentTags.add(clean)
            val updatedTagStr = currentTags.joinToString(", ")
            _activeNote.value = current.copy(tags = updatedTagStr)
            if (current.id > 0) {
                viewModelScope.launch {
                    repository.updateTags(current.id, updatedTagStr)
                }
            }
        }
    }

    fun removeTagFromActiveNote(tagToRemove: String) {
        val current = _activeNote.value ?: return
        val currentTags = current.getTagList().filter { !it.equals(tagToRemove.trim(), ignoreCase = true) }
        val updatedTagStr = currentTags.joinToString(", ")
        _activeNote.value = current.copy(tags = updatedTagStr)
        if (current.id > 0) {
            viewModelScope.launch {
                repository.updateTags(current.id, updatedTagStr)
            }
        }
    }

    fun setSettingsDialogVisible(visible: Boolean) {
        _showSettingsDialog.value = visible
    }

    fun setCustomPromptDialogVisible(visible: Boolean) {
        _showCustomPromptDialog.value = visible
    }

    fun saveCustomApiKey(key: String) {
        _customApiKey.value = key.trim()
        sharedPrefs.edit().putString("custom_api_key", key.trim()).apply()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // Audio Recording Flow
    fun startRecording() {
        clearError()
        audioToTextService.resetLiveTranscript()
        val result = recorderManager.startRecording()
        if (result.isFailure) {
            _errorMessage.value = "Failed to start recording. Please check microphone permissions."
        } else {
            audioToTextService.startLiveSpeechRecognition()
        }
    }

    fun pauseRecording() {
        recorderManager.pauseRecording()
        audioToTextService.stopLiveSpeechRecognition()
    }

    fun resumeRecording() {
        recorderManager.resumeRecording()
        audioToTextService.startLiveSpeechRecognition()
    }

    fun cancelRecording() {
        recorderManager.cancelRecording()
        audioToTextService.stopLiveSpeechRecognition()
        audioToTextService.resetLiveTranscript()
    }

    fun finishRecordingAndProcess(customInstruction: String? = null) {
        audioToTextService.stopLiveSpeechRecognition()
        val duration = recorderManager.durationSeconds.value
        val audioFile = recorderManager.stopRecording()

        if (audioFile == null || !audioFile.exists() || audioFile.length() == 0L) {
            _errorMessage.value = "No audio recorded. Please try speaking into the microphone."
            return
        }

        processRecordedAudio(audioFile, duration, _selectedFormat.value, customInstruction)
    }

    private fun processRecordedAudio(
        audioFile: File,
        durationSeconds: Int,
        formatStyle: FormatStyle,
        customInstruction: String?
    ) {
        _currentScreen.value = AppScreen.PROCESSING
        _processingPhase.value = "Listening to audio waveforms..."

        viewModelScope.launch {
            try {
                delay(300)
                _processingPhase.value = "Transcribing audio to text in real-time..."
                delay(400)
                _processingPhase.value = "Structuring transcribed note content with Gemini..."

                // Trigger real-time audio-to-text processing service
                val result = audioToTextService.transcribeRecordedAudio(
                    audioFile = audioFile,
                    formatStyle = formatStyle,
                    customInstruction = customInstruction
                )

                if (result.isSuccess) {
                    val data = result.getOrThrow()
                    _processingPhase.value = "Populating note content field (${formatStyle.title})..."
                    delay(250)

                    val wordCount = data.polishedText.split("\\s+".toRegex()).count { it.isNotBlank() }
                    val tagsStr = if (data.tags.isNotEmpty()) data.tags.joinToString(", ") else "AudioNote, Ideas"

                    // Automatically populate and persist note content into Room DB
                    val newNote = AudioNote(
                        title = data.title,
                        polishedText = data.polishedText,
                        rawTranscript = data.rawTranscript,
                        summary = data.summary,
                        keyTakeaways = data.keyTakeaways.joinToString("\n"),
                        formatStyle = formatStyle.id,
                        audioDurationSeconds = durationSeconds,
                        audioFilePath = audioFile.absolutePath,
                        createdAt = System.currentTimeMillis(),
                        isFavorite = false,
                        tags = tagsStr,
                        wordCount = wordCount
                    )

                    val noteId = repository.insertNote(newNote)

                    _activeNote.value = ActiveNoteState(
                        id = noteId,
                        title = data.title,
                        polishedText = data.polishedText,
                        rawTranscript = data.rawTranscript,
                        summary = data.summary,
                        keyTakeaways = data.keyTakeaways,
                        formatStyle = formatStyle,
                        audioDurationSeconds = durationSeconds,
                        audioFilePath = audioFile.absolutePath,
                        createdAt = System.currentTimeMillis(),
                        isFavorite = false,
                        tags = tagsStr,
                        isSavedToDb = true
                    )

                    _currentScreen.value = AppScreen.RESULT
                } else {
                    val ex = result.exceptionOrNull()
                    _errorMessage.value = ex?.message ?: "Failed to transcribe audio into note content"
                    _currentScreen.value = AppScreen.RECORD
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.localizedMessage}"
                _currentScreen.value = AppScreen.RECORD
            }
        }
    }

    /**
     * Reformat active note into another style instantly with Gemini
     */
    fun reformatActiveNote(targetStyle: FormatStyle, customPrompt: String? = null) {
        val current = _activeNote.value ?: return
        val baseText = current.rawTranscript.ifBlank { current.polishedText }

        _isReformatting.value = true
        viewModelScope.launch {
            try {
                val result = geminiService.reformatText(
                    rawOrPolishedText = baseText,
                    targetFormatStyle = targetStyle,
                    customInstruction = customPrompt
                )

                if (result.isSuccess) {
                    val data = result.getOrThrow()
                    val newTags = if (data.tags.isNotEmpty()) data.tags.joinToString(", ") else current.tags
                    val updatedState = current.copy(
                        title = data.title.ifBlank { current.title },
                        polishedText = data.polishedText,
                        summary = data.summary.ifBlank { current.summary },
                        keyTakeaways = if (data.keyTakeaways.isNotEmpty()) data.keyTakeaways else current.keyTakeaways,
                        formatStyle = targetStyle,
                        tags = newTags
                    )
                    _activeNote.value = updatedState

                    // Update in DB if existing
                    if (current.id > 0) {
                        repository.updatePolishedContent(
                            id = current.id,
                            title = updatedState.title,
                            polishedText = updatedState.polishedText,
                            formatStyle = targetStyle.id
                        )
                    }
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Reformatting failed"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error reformatting: ${e.localizedMessage}"
            } finally {
                _isReformatting.value = false
            }
        }
    }

    /**
     * Send the selected note content to the Gemini API and generate a fresh concise summary
     */
    fun summarizeActiveNoteWithGemini() {
        val current = _activeNote.value ?: return
        val content = current.polishedText.ifBlank { current.rawTranscript }
        if (content.isBlank()) {
            _errorMessage.value = "Note content is empty. Nothing to summarize."
            return
        }

        _isSummarizingWithGemini.value = true
        clearError()

        viewModelScope.launch {
            try {
                val result = geminiService.generateConciseSummary(
                    noteTitle = current.title,
                    noteContent = content
                )

                if (result.isSuccess) {
                    val summaryData = result.getOrThrow()
                    val existingTagsList = current.getTagList().toMutableList()
                    summaryData.tags.forEach { t ->
                        if (t.isNotBlank() && !existingTagsList.any { it.equals(t, ignoreCase = true) }) {
                            existingTagsList.add(t)
                        }
                    }
                    val updatedTags = existingTagsList.joinToString(", ")

                    val updatedState = current.copy(
                        summary = summaryData.summary,
                        keyTakeaways = if (summaryData.keyTakeaways.isNotEmpty()) summaryData.keyTakeaways else current.keyTakeaways,
                        tags = updatedTags
                    )
                    _activeNote.value = updatedState

                    // Persist updated summary and takeaways to DB if existing note
                    if (current.id > 0) {
                        val entity = updatedState.toAudioNote()
                        repository.updateNote(entity)
                    }
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to generate summary with Gemini."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error generating summary: ${e.localizedMessage}"
            } finally {
                _isSummarizingWithGemini.value = false
            }
        }
    }

    /**
     * Edit note title or text manually
     */
    fun updateActiveNoteContent(newTitle: String, newText: String) {
        val current = _activeNote.value ?: return
        val updated = current.copy(title = newTitle, polishedText = newText)
        _activeNote.value = updated
        if (current.id > 0) {
            viewModelScope.launch {
                repository.updatePolishedContent(
                    id = current.id,
                    title = newTitle,
                    polishedText = newText,
                    formatStyle = current.formatStyle.id
                )
            }
        }
    }

    fun toggleFavoriteActiveNote() {
        val current = _activeNote.value ?: return
        val newFav = !current.isFavorite
        _activeNote.value = current.copy(isFavorite = newFav)
        if (current.id > 0) {
            viewModelScope.launch {
                repository.setFavorite(current.id, newFav)
            }
        }
    }

    fun toggleFavoriteNote(noteId: Long, currentFav: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(noteId, currentFav)
            if (_activeNote.value?.id == noteId) {
                _activeNote.value = _activeNote.value?.copy(isFavorite = !currentFav)
            }
        }
    }

    fun toggleArchiveActiveNote() {
        val current = _activeNote.value ?: return
        val newArchived = !current.isArchived
        _activeNote.value = current.copy(isArchived = newArchived)
        if (current.id > 0) {
            viewModelScope.launch {
                repository.setArchived(current.id, newArchived)
            }
        }
    }

    fun archiveNote(noteId: Long) {
        viewModelScope.launch {
            repository.setArchived(noteId, true)
            if (_activeNote.value?.id == noteId) {
                _activeNote.value = _activeNote.value?.copy(isArchived = true)
            }
        }
    }

    fun unarchiveNote(noteId: Long) {
        viewModelScope.launch {
            repository.setArchived(noteId, false)
            if (_activeNote.value?.id == noteId) {
                _activeNote.value = _activeNote.value?.copy(isArchived = false)
            }
        }
    }

    fun toggleArchive(noteId: Long, currentArchived: Boolean) {
        viewModelScope.launch {
            repository.toggleArchive(noteId, currentArchived)
            if (_activeNote.value?.id == noteId) {
                _activeNote.value = _activeNote.value?.copy(isArchived = !currentArchived)
            }
        }
    }

    fun deleteAllArchivedNotes() {
        viewModelScope.launch {
            repository.deleteAllArchived()
            if (_activeNote.value?.isArchived == true) {
                _activeNote.value = null
                _currentScreen.value = AppScreen.HISTORY
            }
        }
    }

    fun togglePinned(noteId: Long, currentPinned: Boolean) {
        viewModelScope.launch {
            repository.togglePinned(noteId, currentPinned)
            if (_activeNote.value?.id == noteId) {
                _activeNote.value = _activeNote.value?.copy(isPinned = !currentPinned)
            }
        }
    }

    fun togglePinActiveNote() {
        val current = _activeNote.value ?: return
        val newPin = !current.isPinned
        _activeNote.value = current.copy(isPinned = newPin)
        if (current.id > 0) {
            viewModelScope.launch {
                repository.setPinned(current.id, newPin)
            }
        }
    }

    fun setSpeechRate(rate: Float) {
        ttsManager.setSpeechRate(rate)
    }

    fun openNoteDetail(note: AudioNote) {
        val takeaways = note.keyTakeaways.split("\n").filter { it.isNotBlank() }
        _activeNote.value = ActiveNoteState(
            id = note.id,
            title = note.title,
            polishedText = note.polishedText,
            rawTranscript = note.rawTranscript,
            summary = note.summary,
            keyTakeaways = takeaways,
            formatStyle = FormatStyle.fromId(note.formatStyle),
            audioDurationSeconds = note.audioDurationSeconds,
            audioFilePath = note.audioFilePath,
            createdAt = note.createdAt,
            isFavorite = note.isFavorite,
            isArchived = note.isArchived,
            isPinned = note.isPinned,
            tags = note.tags,
            category = note.category,
            actionChecklist = note.getChecklistItems(),
            isSavedToDb = true
        )
        _currentScreen.value = AppScreen.RESULT
    }

    fun importAudioFromUri(uri: Uri, context: Context, formatStyle: FormatStyle = _selectedFormat.value) {
        _currentScreen.value = AppScreen.PROCESSING
        _processingPhase.value = "Importing audio file..."

        viewModelScope.launch {
            try {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(uri) ?: "audio/mp4"
                val inputStream = contentResolver.openInputStream(uri)
                val audioBytes = inputStream?.use { it.readBytes() }

                if (audioBytes == null || audioBytes.isEmpty()) {
                    _errorMessage.value = "Could not read audio file. Please try another file."
                    _currentScreen.value = AppScreen.RECORD
                    return@launch
                }

                _processingPhase.value = "Transcribing & structuring imported audio with Gemini..."
                val result = geminiService.processAudioBytes(
                    audioBytes = audioBytes,
                    mimeType = mimeType,
                    formatStyle = formatStyle
                )

                if (result.isSuccess) {
                    val data = result.getOrThrow()
                    val wordCount = data.polishedText.split("\\s+".toRegex()).count { it.isNotBlank() }
                    val tagsStr = if (data.tags.isNotEmpty()) data.tags.joinToString(", ") else "Imported, Audio"

                    val newNote = AudioNote(
                        title = data.title,
                        polishedText = data.polishedText,
                        rawTranscript = data.rawTranscript,
                        summary = data.summary,
                        keyTakeaways = data.keyTakeaways.joinToString("\n"),
                        formatStyle = formatStyle.id,
                        audioDurationSeconds = 0,
                        audioFilePath = null,
                        createdAt = System.currentTimeMillis(),
                        isFavorite = false,
                        tags = tagsStr,
                        category = "Idea",
                        wordCount = wordCount
                    )

                    val noteId = repository.insertNote(newNote)

                    _activeNote.value = ActiveNoteState(
                        id = noteId,
                        title = data.title,
                        polishedText = data.polishedText,
                        rawTranscript = data.rawTranscript,
                        summary = data.summary,
                        keyTakeaways = data.keyTakeaways,
                        formatStyle = formatStyle,
                        audioDurationSeconds = 0,
                        audioFilePath = null,
                        createdAt = System.currentTimeMillis(),
                        isFavorite = false,
                        tags = tagsStr,
                        category = "Idea",
                        isSavedToDb = true
                    )

                    _currentScreen.value = AppScreen.RESULT
                } else {
                    val ex = result.exceptionOrNull()
                    _errorMessage.value = ex?.message ?: "Failed to process imported audio"
                    _currentScreen.value = AppScreen.RECORD
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error importing audio: ${e.localizedMessage}"
                _currentScreen.value = AppScreen.RECORD
            }
        }
    }

    // --- CONTENT REPURPOSING ---
    fun repurposeActiveNote(targetPurpose: String) {
        val current = _activeNote.value ?: return
        val baseText = current.polishedText.ifBlank { current.rawTranscript }
        if (baseText.isBlank()) return

        _isRepurposing.value = true
        viewModelScope.launch {
            try {
                val result = geminiService.repurposeContent(baseText, targetPurpose)
                if (result.isSuccess) {
                    val repurposed = result.getOrThrow()
                    val updated = current.copy(polishedText = repurposed)
                    _activeNote.value = updated
                    if (current.id > 0) {
                        repository.updatePolishedContent(
                            id = current.id,
                            title = current.title,
                            polishedText = repurposed,
                            formatStyle = current.formatStyle.id
                        )
                    }
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to repurpose content"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error repurposing: ${e.localizedMessage}"
            } finally {
                _isRepurposing.value = false
            }
        }
    }

    // --- INSTANT TRANSLATION ---
    fun translateActiveNote(targetLanguage: String) {
        val current = _activeNote.value ?: return
        val baseText = current.polishedText.ifBlank { current.rawTranscript }
        if (baseText.isBlank()) return

        _isTranslating.value = true
        viewModelScope.launch {
            try {
                val result = geminiService.translateText(baseText, targetLanguage)
                if (result.isSuccess) {
                    val translated = result.getOrThrow()
                    val updated = current.copy(polishedText = translated)
                    _activeNote.value = updated
                    if (current.id > 0) {
                        repository.updatePolishedContent(
                            id = current.id,
                            title = current.title,
                            polishedText = translated,
                            formatStyle = current.formatStyle.id
                        )
                    }
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to translate note"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error translating: ${e.localizedMessage}"
            } finally {
                _isTranslating.value = false
            }
        }
    }

    // --- ACTION CHECKLIST ---
    fun generateChecklistForActiveNote() {
        val current = _activeNote.value ?: return
        val baseText = current.polishedText.ifBlank { current.rawTranscript }
        if (baseText.isBlank()) return

        _isGeneratingChecklist.value = true
        viewModelScope.launch {
            try {
                val result = geminiService.generateActionChecklist(current.title, baseText)
                if (result.isSuccess) {
                    val items = result.getOrThrow().map { ChecklistItem(text = it, isCompleted = false) }
                    val updated = current.copy(actionChecklist = items)
                    _activeNote.value = updated
                    if (current.id > 0) {
                        val checklistStr = items.joinToString("\n") { (if (it.isCompleted) "[x] " else "[ ] ") + it.text }
                        repository.updateActionChecklist(current.id, checklistStr)
                    }
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to generate checklist"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error generating checklist: ${e.localizedMessage}"
            } finally {
                _isGeneratingChecklist.value = false
            }
        }
    }

    fun toggleChecklistItem(index: Int) {
        val current = _activeNote.value ?: return
        val list = current.actionChecklist.toMutableList()
        if (index in list.indices) {
            val item = list[index]
            list[index] = item.copy(isCompleted = !item.isCompleted)
            val updated = current.copy(actionChecklist = list)
            _activeNote.value = updated
            if (current.id > 0) {
                viewModelScope.launch {
                    val checklistStr = list.joinToString("\n") { (if (it.isCompleted) "[x] " else "[ ] ") + it.text }
                    repository.updateActionChecklist(current.id, checklistStr)
                }
            }
        }
    }

    fun addChecklistItem(text: String) {
        if (text.isBlank()) return
        val current = _activeNote.value ?: return
        val list = current.actionChecklist.toMutableList()
        list.add(ChecklistItem(text = text.trim(), isCompleted = false))
        val updated = current.copy(actionChecklist = list)
        _activeNote.value = updated
        if (current.id > 0) {
            viewModelScope.launch {
                val checklistStr = list.joinToString("\n") { (if (it.isCompleted) "[x] " else "[ ] ") + it.text }
                repository.updateActionChecklist(current.id, checklistStr)
            }
        }
    }

    fun removeChecklistItem(index: Int) {
        val current = _activeNote.value ?: return
        val list = current.actionChecklist.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            val updated = current.copy(actionChecklist = list)
            _activeNote.value = updated
            if (current.id > 0) {
                viewModelScope.launch {
                    val checklistStr = list.joinToString("\n") { (if (it.isCompleted) "[x] " else "[ ] ") + it.text }
                    repository.updateActionChecklist(current.id, checklistStr)
                }
            }
        }
    }

    // --- BULK ACTIONS IN HISTORY ---
    fun startBulkSelection(initialId: Long? = null) {
        _isBulkSelectionMode.value = true
        if (initialId != null) {
            _selectedNoteIds.value = setOf(initialId)
        } else {
            _selectedNoteIds.value = emptySet()
        }
    }

    fun toggleNoteSelection(id: Long) {
        val current = _selectedNoteIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _selectedNoteIds.value = current
        if (current.isEmpty() && !_isBulkSelectionMode.value) {
            _isBulkSelectionMode.value = false
        }
    }

    fun selectAllNotes(noteIds: List<Long>) {
        _selectedNoteIds.value = noteIds.toSet()
    }

    fun selectAllNotesFromList(notes: List<AudioNote>) {
        _selectedNoteIds.value = notes.map { it.id }.toSet()
    }

    fun clearSelectedNotes() {
        _selectedNoteIds.value = emptySet()
    }

    fun exitBulkSelection() {
        clearBulkSelection()
    }

    fun toggleBulkSelectionMode() {
        if (_isBulkSelectionMode.value) {
            clearBulkSelection()
        } else {
            startBulkSelection()
        }
    }

    fun clearBulkSelection() {
        _selectedNoteIds.value = emptySet()
        _isBulkSelectionMode.value = false
    }

    fun bulkDeleteSelected() {
        val ids = _selectedNoteIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.bulkDelete(ids)
            clearBulkSelection()
        }
    }

    fun bulkArchiveSelected(archive: Boolean) {
        val ids = _selectedNoteIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.bulkSetArchived(ids, archive)
            clearBulkSelection()
        }
    }

    fun bulkArchiveSelected() {
        bulkArchiveSelected(archive = true)
    }

    fun bulkUnarchiveSelected() {
        bulkArchiveSelected(archive = false)
    }

    fun bulkChangeCategorySelected(category: String) {
        val ids = _selectedNoteIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.bulkSetCategory(ids, category)
            clearBulkSelection()
        }
    }

    fun bulkSetCategory(ids: List<Long>, category: String) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.bulkSetCategory(ids, category)
            clearBulkSelection()
        }
    }

    fun bulkPinSelected(pin: Boolean) {
        val ids = _selectedNoteIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.bulkSetPinned(ids, pin)
            clearBulkSelection()
        }
    }

    fun bulkTogglePin(ids: List<Long>, pin: Boolean) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.bulkSetPinned(ids, pin)
            clearBulkSelection()
        }
    }

    fun togglePinNote(id: Long, currentPin: Boolean) {
        viewModelScope.launch {
            repository.togglePinned(id, currentPin)
        }
    }

    // --- EXPORT & SHARE ---
    fun exportNoteAsMarkdown(note: AudioNote): String {
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(note.createdAt))
        val checklist = note.getChecklistItems()
        return buildString {
            appendLine("# ${note.title}")
            appendLine()
            appendLine("> **Date:** $dateStr | **Category:** ${note.category} | **Format:** ${note.formatStyle}")
            if (note.tags.isNotBlank()) {
                appendLine("> **Tags:** ${note.tags}")
            }
            appendLine()
            if (note.summary.isNotBlank()) {
                appendLine("## 💡 Executive Summary")
                appendLine(note.summary)
                appendLine()
            }
            appendLine("## 📝 Note Content")
            appendLine(note.polishedText)
            appendLine()
            if (note.keyTakeaways.isNotBlank()) {
                appendLine("## 🎯 Key Takeaways")
                note.keyTakeaways.lines().filter { it.isNotBlank() }.forEach {
                    appendLine("- ${it.removePrefix("-").removePrefix("•").trim()}")
                }
                appendLine()
            }
            if (checklist.isNotEmpty()) {
                appendLine("## ✅ Action Checklist")
                checklist.forEach { item ->
                    appendLine("- [${if (item.isCompleted) "x" else " "}] ${item.text}")
                }
                appendLine()
            }
            if (note.rawTranscript.isNotBlank()) {
                appendLine("---")
                appendLine("### 🎙️ Raw Voice Transcript")
                appendLine(note.rawTranscript)
            }
        }
    }

    fun exportNoteAsPlainText(note: AudioNote): String {
        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(note.createdAt))
        val checklist = note.getChecklistItems()
        return buildString {
            appendLine(note.title.uppercase(Locale.getDefault()))
            appendLine("Data: $dateStr | Categoria: ${note.category}")
            appendLine("=".repeat(40))
            if (note.summary.isNotBlank()) {
                appendLine("\nRESUMO EXECUTIVO:")
                appendLine(note.summary)
            }
            appendLine("\nCONTEÚDO:")
            appendLine(note.polishedText)
            if (note.keyTakeaways.isNotBlank()) {
                appendLine("\nPONTOS CHAVE:")
                appendLine(note.keyTakeaways)
            }
            if (checklist.isNotEmpty()) {
                appendLine("\nCHECKLIST DE AÇÕES:")
                checklist.forEach { item ->
                    appendLine("[${if (item.isCompleted) "X" else " "}] ${item.text}")
                }
            }
        }
    }

    fun shareNote(context: Context, note: AudioNote, asMarkdown: Boolean = true) {
        val text = if (asMarkdown) exportNoteAsMarkdown(note) else exportNoteAsPlainText(note)
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_SUBJECT, note.title)
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val chooser = Intent.createChooser(intent, "Compartilhar nota via...")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            repository.deleteNoteById(noteId)
            if (_activeNote.value?.id == noteId) {
                _activeNote.value = null
                _currentScreen.value = AppScreen.RECORD
            }
        }
    }

    /**
     * Fallback / Text Mode: Process typed stream-of-thought (e.g. for testing in emulator)
     */
    fun processTypedText(rawText: String, formatStyle: FormatStyle = _selectedFormat.value) {
        if (rawText.isBlank()) return
        _currentScreen.value = AppScreen.PROCESSING
        _processingPhase.value = "Analyzing raw thoughts..."

        viewModelScope.launch {
            try {
                delay(300)
                _processingPhase.value = "Removing filler words & structuring with Gemini..."
                val result = geminiService.reformatText(rawText, formatStyle)

                if (result.isSuccess) {
                    val data = result.getOrThrow()
                    val wordCount = data.polishedText.split("\\s+".toRegex()).count { it.isNotBlank() }
                    val tagsStr = if (data.tags.isNotEmpty()) data.tags.joinToString(", ") else "Thoughts, Notes"

                    val newNote = AudioNote(
                        title = data.title,
                        polishedText = data.polishedText,
                        rawTranscript = rawText,
                        summary = data.summary,
                        keyTakeaways = data.keyTakeaways.joinToString("\n"),
                        formatStyle = formatStyle.id,
                        audioDurationSeconds = (rawText.length / 15).coerceAtLeast(5),
                        audioFilePath = null,
                        createdAt = System.currentTimeMillis(),
                        isFavorite = false,
                        tags = tagsStr,
                        wordCount = wordCount
                    )

                    val noteId = repository.insertNote(newNote)

                    _activeNote.value = ActiveNoteState(
                        id = noteId,
                        title = data.title,
                        polishedText = data.polishedText,
                        rawTranscript = rawText,
                        summary = data.summary,
                        keyTakeaways = data.keyTakeaways,
                        formatStyle = formatStyle,
                        audioDurationSeconds = newNote.audioDurationSeconds,
                        audioFilePath = null,
                        createdAt = System.currentTimeMillis(),
                        isFavorite = false,
                        tags = tagsStr,
                        isSavedToDb = true
                    )

                    _currentScreen.value = AppScreen.RESULT
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Processing failed"
                    _currentScreen.value = AppScreen.RECORD
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.localizedMessage}"
                _currentScreen.value = AppScreen.RECORD
            }
        }
    }

    /**
     * Share formatted text via Android Sharesheet
     */
    fun shareNote(context: Context, note: ActiveNoteState) {
        shareNoteFormatted(context, note, asMarkdown = false)
    }

    fun formatNoteAsMarkdown(note: ActiveNoteState): String {
        return ExportManager.buildMarkdown(
            title = note.title,
            polishedText = note.polishedText,
            rawTranscript = note.rawTranscript,
            summary = note.summary,
            keyTakeaways = note.keyTakeaways,
            checklist = note.actionChecklist,
            category = note.category,
            formatStyle = note.formatStyle.title,
            createdAt = note.createdAt
        )
    }

    fun formatNoteAsPlainText(note: ActiveNoteState): String {
        return ExportManager.buildPlainText(
            title = note.title,
            polishedText = note.polishedText,
            rawTranscript = note.rawTranscript,
            summary = note.summary,
            keyTakeaways = note.keyTakeaways,
            checklist = note.actionChecklist,
            category = note.category,
            createdAt = note.createdAt
        )
    }

    fun shareNoteFormatted(context: Context, note: ActiveNoteState, asMarkdown: Boolean) {
        val text = if (asMarkdown) formatNoteAsMarkdown(note) else formatNoteAsPlainText(note)
        ExportManager.shareText(context, text, note.title)
    }

    /**
     * Export & Share as PDF
     */
    fun exportNoteAsPdf(context: Context, note: ActiveNoteState): File {
        return ExportManager.createPdfFile(
            context = context,
            title = note.title,
            polishedText = note.polishedText,
            summary = note.summary,
            keyTakeaways = note.keyTakeaways,
            checklist = note.actionChecklist,
            category = note.category,
            createdAt = note.createdAt
        )
    }

    fun shareNoteAsPdf(context: Context, note: ActiveNoteState) {
        try {
            val file = exportNoteAsPdf(context, note)
            ExportManager.shareFile(context, file, "application/pdf", note.title)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to export PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Export & Share as Plain Text file (.txt)
     */
    fun exportNoteAsPlainTextFile(context: Context, note: ActiveNoteState): File {
        val content = formatNoteAsPlainText(note)
        return ExportManager.createPlainTextFile(context, note.title, content)
    }

    fun shareNoteAsPlainTextFile(context: Context, note: ActiveNoteState) {
        try {
            val file = exportNoteAsPlainTextFile(context, note)
            ExportManager.shareFile(context, file, "text/plain", note.title)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to export text file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Export & Share as Markdown file (.md)
     */
    fun exportNoteAsMarkdownFile(context: Context, note: ActiveNoteState): File {
        val content = formatNoteAsMarkdown(note)
        return ExportManager.createMarkdownFile(context, note.title, content)
    }

    fun shareNoteAsMarkdownFile(context: Context, note: ActiveNoteState) {
        try {
            val file = exportNoteAsMarkdownFile(context, note)
            ExportManager.shareFile(context, file, "text/markdown", note.title)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to export markdown file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Share AudioNote entity directly via Android Intent
     */
    fun shareAudioNote(context: Context, note: AudioNote) {
        val text = exportNoteAsPlainText(note)
        ExportManager.shareText(context, text, note.title)
    }

    fun shareAudioNoteAsPdf(context: Context, note: AudioNote) {
        try {
            val file = ExportManager.createPdfFile(
                context = context,
                title = note.title,
                polishedText = note.polishedText,
                summary = note.summary,
                keyTakeaways = if (note.keyTakeaways.isNotBlank()) note.keyTakeaways.lines().filter { it.isNotBlank() } else emptyList(),
                checklist = note.getChecklistItems(),
                category = note.category,
                createdAt = note.createdAt
            )
            ExportManager.shareFile(context, file, "application/pdf", note.title)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to export PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Legacy / Direct Export file helper
     */
    fun exportNoteFile(context: Context, note: ActiveNoteState) {
        shareNoteAsMarkdownFile(context, note)
    }

    // ==========================================
    // --- CONVERSATIONAL AI AGENT (VOICE & TEXT) ---
    // ==========================================

    fun toggleVoiceResponse() {
        _isVoiceResponseEnabled.value = !_isVoiceResponseEnabled.value
        if (!_isVoiceResponseEnabled.value) {
            stopSpeaking()
        }
    }

    fun speakChatMessage(message: ChatMessage) {
        if (isSpeaking.value && currentlySpeakingId.value == message.id) {
            stopSpeaking()
        } else {
            ttsManager.speak(message.text, message.id, message.language)
        }
    }

    fun speakText(text: String, utteranceId: String = "tts_utterance") {
        if (isSpeaking.value && currentlySpeakingId.value == utteranceId) {
            stopSpeaking()
        } else {
            ttsManager.speak(text, utteranceId)
        }
    }

    fun stopSpeaking() {
        ttsManager.stop()
    }

    fun clearChatSession() {
        stopSpeaking()
        viewModelScope.launch {
            chatRepository.clearAllMessages()
            val resetMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = MessageSender.AGENT,
                text = "Sessão reiniciada. Qual ideia ou desafio executivo você gostaria de explorar agora?",
                suggestedFollowUps = listOf(
                    "💡 Brainstorming de modelo de negócio",
                    "🎯 Criar proposta de valor irrecusável",
                    "📊 Como apresentar essa ideia para investidores?"
                )
            )
            chatRepository.insertMessage(resetMessage)
        }
    }

    fun deleteChatMessage(id: String) {
        viewModelScope.launch {
            chatRepository.deleteMessageById(id)
        }
    }

    fun toggleGoogleSearch(enabled: Boolean? = null) {
        _isGoogleSearchEnabled.value = enabled ?: !_isGoogleSearchEnabled.value
    }

    fun toggleNotesContext(enabled: Boolean? = null) {
        _isNotesContextEnabled.value = enabled ?: !_isNotesContextEnabled.value
    }

    private fun buildNotesContextString(): String? {
        val notes = allNotes.value
        if (notes.isEmpty()) return null

        val recentNotes = notes.filter { !it.isArchived }.take(8)
        if (recentNotes.isEmpty()) return null

        return buildString {
            append("O usuário possui as seguintes notas recentes registradas no aplicativo:\n")
            recentNotes.forEachIndexed { index, note ->
                append("${index + 1}. Título: \"${note.title}\"\n")
                append("   Estilo: ${note.formatStyle}\n")
                if (note.summary.isNotBlank()) {
                    append("   Resumo: ${note.summary}\n")
                }
                append("   Conteúdo: ${note.polishedText.take(280)}\n\n")
            }
        }
    }

    /**
     * Send a text message to the AI brainstorming agent
     */
    fun sendChatMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || _isAgentThinking.value) return

        val userMessage = ChatMessage(
            sender = MessageSender.USER,
            text = trimmed,
            isAudioInput = false
        )

        val updatedList = _chatMessages.value + userMessage
        _chatMessages.value = updatedList
        _isAgentThinking.value = true

        val notesContext = if (_isNotesContextEnabled.value) buildNotesContextString() else null
        val searchEnabled = _isGoogleSearchEnabled.value

        viewModelScope.launch {
            chatRepository.insertMessage(userMessage)
            try {
                val result = geminiService.chatWithAgent(
                    conversationHistory = updatedList,
                    newUserMessage = trimmed,
                    userAudioFile = null,
                    notesContext = notesContext,
                    enableGoogleSearch = searchEnabled
                )

                if (result.isSuccess) {
                    val reply = result.getOrThrow()
                    val agentMessage = ChatMessage(
                        sender = MessageSender.AGENT,
                        text = reply.replyText,
                        suggestedFollowUps = reply.suggestedFollowUps,
                        language = reply.language,
                        searchQueries = reply.searchQueries,
                        groundingSources = reply.groundingSources
                    )
                    chatRepository.insertMessage(agentMessage)

                    // If auto-voice response is enabled, speak aloud
                    if (_isVoiceResponseEnabled.value) {
                        delay(200)
                        ttsManager.speak(agentMessage.text, agentMessage.id, agentMessage.language)
                    }
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Erro ao se comunicar com o agente"
                    val agentErrorMessage = ChatMessage(
                        sender = MessageSender.AGENT,
                        text = "Não consegui processar no momento: $errorMsg. Por favor, tente novamente."
                    )
                    chatRepository.insertMessage(agentErrorMessage)
                }
            } catch (e: Exception) {
                val agentErrorMessage = ChatMessage(
                    sender = MessageSender.AGENT,
                    text = "Erro inesperado: ${e.localizedMessage}"
                )
                chatRepository.insertMessage(agentErrorMessage)
            } finally {
                _isAgentThinking.value = false
            }
        }
    }

    /**
     * Start recording voice directly inside the chat conversation
     */
    fun startChatVoiceRecording() {
        stopSpeaking()
        clearError()
        val result = recorderManager.startRecording()
        if (result.isSuccess) {
            _isChatRecording.value = true
        } else {
            _errorMessage.value = "Não foi possível acessar o microfone."
        }
    }

    fun cancelChatVoiceRecording() {
        recorderManager.cancelRecording()
        _isChatRecording.value = false
    }

    /**
     * Finish voice recording and send spoken audio to AI Agent
     */
    fun finishChatVoiceRecordingAndSend() {
        val duration = recorderManager.durationSeconds.value
        val audioFile = recorderManager.stopRecording()
        _isChatRecording.value = false

        if (audioFile == null || !audioFile.exists() || audioFile.length() == 0L) {
            _errorMessage.value = "Nenhum áudio detectado."
            return
        }

        val placeholderUserMessage = ChatMessage(
            sender = MessageSender.USER,
            text = "🎤 Mensagem de voz enviada ($duration s)...",
            isAudioInput = true,
            audioDurationSeconds = duration
        )

        val updatedList = _chatMessages.value + placeholderUserMessage
        _chatMessages.value = updatedList
        _isAgentThinking.value = true

        val notesContext = if (_isNotesContextEnabled.value) buildNotesContextString() else null
        val searchEnabled = _isGoogleSearchEnabled.value

        viewModelScope.launch {
            chatRepository.insertMessage(placeholderUserMessage)
            try {
                val result = geminiService.chatWithAgent(
                    conversationHistory = updatedList,
                    newUserMessage = null,
                    userAudioFile = audioFile,
                    notesContext = notesContext,
                    enableGoogleSearch = searchEnabled
                )

                if (result.isSuccess) {
                    val reply = result.getOrThrow()

                    // Replace placeholder with verbatim transcribed user speech if available
                    val finalUserText = if (!reply.userSpokenTranscription.isNullOrBlank()) {
                        "🎤 \"${reply.userSpokenTranscription}\""
                    } else {
                        "🎤 Mensagem de voz ($duration s)"
                    }

                    val updatedUserMsg = placeholderUserMessage.copy(text = finalUserText)
                    chatRepository.insertMessage(updatedUserMsg)

                    val agentMessage = ChatMessage(
                        sender = MessageSender.AGENT,
                        text = reply.replyText,
                        suggestedFollowUps = reply.suggestedFollowUps,
                        language = reply.language,
                        searchQueries = reply.searchQueries,
                        groundingSources = reply.groundingSources
                    )

                    chatRepository.insertMessage(agentMessage)

                    // If auto-voice response is enabled, speak aloud
                    if (_isVoiceResponseEnabled.value) {
                        delay(200)
                        ttsManager.speak(agentMessage.text, agentMessage.id, agentMessage.language)
                    }
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Falha ao processar áudio com o agente"
                    val agentErrorMessage = ChatMessage(
                        sender = MessageSender.AGENT,
                        text = "Não foi possível compreender o áudio: $errorMsg"
                    )
                    chatRepository.insertMessage(agentErrorMessage)
                }
            } catch (e: Exception) {
                val agentErrorMessage = ChatMessage(
                    sender = MessageSender.AGENT,
                    text = "Erro ao enviar áudio: ${e.localizedMessage}"
                )
                chatRepository.insertMessage(agentErrorMessage)
            } finally {
                _isAgentThinking.value = false
            }
        }
    }

    /**
     * Enriches an active or specific note using Google Search Grounding
     */
    fun enrichActiveNoteWithWebSearch() {
        val active = _activeNote.value ?: return
        enrichNoteWithWebSearch(
            noteId = active.id,
            noteTitle = active.title,
            noteContent = active.polishedText,
            tags = ""
        )
    }

    fun enrichNoteWithWebSearch(
        noteId: Long,
        noteTitle: String,
        noteContent: String,
        tags: String = ""
    ) {
        if (_isEnrichingNote.value) return

        _isEnrichingNote.value = true
        _enrichingNoteId.value = noteId

        viewModelScope.launch {
            try {
                val result = geminiService.enrichNoteWithWebSearch(
                    noteId = noteId,
                    noteTitle = noteTitle,
                    noteContent = noteContent,
                    tags = tags
                )

                if (result.isSuccess) {
                    val enrichment = result.getOrThrow()
                    val currentMap = _noteEnrichments.value.toMutableMap()
                    currentMap[noteId] = enrichment
                    _noteEnrichments.value = currentMap
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Erro ao enriquecer nota com pesquisa Google."
                    _errorMessage.value = errorMsg
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erro na pesquisa: ${e.localizedMessage}"
            } finally {
                _isEnrichingNote.value = false
                _enrichingNoteId.value = null
            }
        }
    }

    /**
     * Initiate a brainstorm session based on an existing note with search grounding enabled
     */
    fun startChatWithNoteContext(note: AudioNote) {
        stopSpeaking()
        val starterPrompt = "Olá! Gostaria de explorar e aprofundar minha nota sobre \"${note.title}\":\n\n${note.polishedText}\n\nPoderia me ajudar a encontrar fatos na web, tendências e estruturar os próximos passos?"
        val userStarter = ChatMessage(
            sender = MessageSender.USER,
            text = starterPrompt
        )
        _chatMessages.value = listOf(userStarter)
        _isGoogleSearchEnabled.value = true
        _currentScreen.value = AppScreen.AGENT
        _isAgentThinking.value = true

        viewModelScope.launch {
            chatRepository.insertMessage(userStarter)
            try {
                val result = geminiService.chatWithAgent(
                    conversationHistory = listOf(userStarter),
                    newUserMessage = starterPrompt,
                    enableGoogleSearch = true
                )

                if (result.isSuccess) {
                    val reply = result.getOrThrow()
                    val agentMessage = ChatMessage(
                        sender = MessageSender.AGENT,
                        text = reply.replyText,
                        suggestedFollowUps = reply.suggestedFollowUps,
                        language = reply.language,
                        searchQueries = reply.searchQueries,
                        groundingSources = reply.groundingSources
                    )
                    chatRepository.insertMessage(agentMessage)
                    if (_isVoiceResponseEnabled.value) {
                        delay(200)
                        ttsManager.speak(agentMessage.text, agentMessage.id, agentMessage.language)
                    }
                }
            } catch (_: Exception) {
            } finally {
                _isAgentThinking.value = false
            }
        }
    }

    /**
     * Turn an idea brainstormed in chat directly into an AudioPen Structured Note
     */
    fun createNoteFromAgentIdea(ideaText: String, noteTitle: String? = null) {
        viewModelScope.launch {
            val title = noteTitle ?: "Brainstorm: ${ideaText.take(30)}..."
            val wordCount = ideaText.split("\\s+".toRegex()).count { it.isNotBlank() }

            val newNote = AudioNote(
                title = title,
                polishedText = ideaText,
                rawTranscript = "Criado a partir da conversa com o Agente de IA AudioPen.",
                summary = "Ideia refinada e estruturada em parceria com o Agente de IA.",
                keyTakeaways = "Ideia gerada e pronta para execução.",
                formatStyle = FormatStyle.BRAINSTORM.id,
                audioDurationSeconds = 0,
                audioFilePath = null,
                createdAt = System.currentTimeMillis(),
                isFavorite = false,
                wordCount = wordCount
            )

            val noteId = repository.insertNote(newNote)
            _activeNote.value = ActiveNoteState(
                id = noteId,
                title = title,
                polishedText = ideaText,
                rawTranscript = newNote.rawTranscript,
                summary = newNote.summary,
                keyTakeaways = listOf("Ideia pronta para ação."),
                formatStyle = FormatStyle.BRAINSTORM,
                audioDurationSeconds = 0,
                audioFilePath = null,
                createdAt = System.currentTimeMillis(),
                isFavorite = false,
                isSavedToDb = true
            )
            _currentScreen.value = AppScreen.RESULT
        }
    }

    /**
     * Initiate a brainstorm session based on an existing note
     */
    fun discussNoteWithAgent(note: ActiveNoteState) {
        stopSpeaking()
        val starterPrompt = "Aqui está a minha nota '${note.title}':\n\n${note.polishedText}\n\nO que você acha desta ideia? Como podemos desafiar, aprofundar e transformar isso em um plano de ação concreto?"
        val userStarter = ChatMessage(
            sender = MessageSender.USER,
            text = starterPrompt
        )
        _chatMessages.value = listOf(userStarter)
        _currentScreen.value = AppScreen.AGENT
        _isAgentThinking.value = true

        viewModelScope.launch {
            chatRepository.insertMessage(userStarter)
            try {
                val result = geminiService.chatWithAgent(
                    conversationHistory = listOf(userStarter),
                    newUserMessage = starterPrompt
                )

                if (result.isSuccess) {
                    val reply = result.getOrThrow()
                    val agentMessage = ChatMessage(
                        sender = MessageSender.AGENT,
                        text = reply.replyText,
                        suggestedFollowUps = reply.suggestedFollowUps,
                        language = reply.language
                    )
                    chatRepository.insertMessage(agentMessage)
                    if (_isVoiceResponseEnabled.value) {
                        delay(200)
                        ttsManager.speak(agentMessage.text, agentMessage.id, agentMessage.language)
                    }
                }
            } catch (_: Exception) {
            } finally {
                _isAgentThinking.value = false
            }
        }
    }

    fun playVoiceMemo(filePath: String) {
        stopSpeaking()
        audioPlayerManager.playAudioFile(filePath)
    }

    fun pauseVoiceMemo() {
        audioPlayerManager.pausePlayback()
    }

    fun resumeVoiceMemo() {
        audioPlayerManager.resumePlayback()
    }

    fun toggleVoiceMemo(filePath: String) {
        stopSpeaking()
        audioPlayerManager.togglePlayPause(filePath)
    }

    fun seekVoiceMemo(positionMs: Int) {
        audioPlayerManager.seekTo(positionMs)
    }

    fun setVoiceMemoSpeed(speed: Float) {
        audioPlayerManager.setSpeed(speed)
    }

    fun stopVoiceMemo() {
        audioPlayerManager.stopPlayback()
    }

    override fun onCleared() {
        super.onCleared()
        recorderManager.cancelRecording()
        audioPlayerManager.stopPlayback()
        ttsManager.shutdown()
    }
}
