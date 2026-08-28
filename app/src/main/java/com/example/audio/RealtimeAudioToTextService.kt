package com.example.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.example.data.model.FormatStyle
import com.example.data.remote.GeminiProcessResult
import com.example.data.remote.GeminiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * Real-time audio-to-text processing service that manages:
 * 1. Real-time live speech recognition while the user speaks.
 * 2. Automatic transcription and structuring triggered after a voice note is recorded.
 */
class RealtimeAudioToTextService(
    private val context: Context,
    private val geminiService: GeminiService
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _isLiveListening = MutableStateFlow(false)
    val isLiveListening: StateFlow<Boolean> = _isLiveListening.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _processingStatus = MutableStateFlow("Ready")
    val processingStatus: StateFlow<String> = _processingStatus.asStateFlow()

    /**
     * Start live speech recognition in real-time while voice recording is ongoing.
     */
    fun startLiveSpeechRecognition(locale: Locale = Locale.getDefault()) {
        mainHandler.post {
            try {
                if (SpeechRecognizer.isRecognitionAvailable(context)) {
                    stopLiveSpeechRecognition()

                    val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.language)
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    }

                    recognizer.setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _isLiveListening.value = true
                        }

                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            _isLiveListening.value = false
                        }

                        override fun onError(error: Int) {
                            Log.w("RealtimeAudioToText", "Live speech recognition error code: $error")
                            _isLiveListening.value = false
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                val text = matches[0]
                                if (text.isNotBlank()) {
                                    val current = _liveTranscript.value
                                    _liveTranscript.value = if (current.isBlank()) text else "$current $text"
                                }
                            }
                            _isLiveListening.value = false
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                val partial = matches[0]
                                if (partial.isNotBlank()) {
                                    _liveTranscript.value = partial
                                }
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })

                    speechRecognizer = recognizer
                    recognizer.startListening(intent)
                }
            } catch (e: Exception) {
                Log.e("RealtimeAudioToText", "Error starting live speech recognition", e)
            }
        }
    }

    /**
     * Stop live speech recognition.
     */
    fun stopLiveSpeechRecognition() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                Log.e("RealtimeAudioToText", "Error destroying speech recognizer", e)
            } finally {
                speechRecognizer = null
                _isLiveListening.value = false
            }
        }
    }

    fun resetLiveTranscript() {
        _liveTranscript.value = ""
    }

    /**
     * Main transcription service triggered automatically after a voice note is recorded.
     * Takes the recorded audio file, executes audio-to-text processing with Gemini,
     * and falls back to live speech recognition transcript or local formatting if needed.
     */
    suspend fun transcribeRecordedAudio(
        audioFile: File,
        formatStyle: FormatStyle = FormatStyle.CLASSIC,
        customInstruction: String? = null
    ): Result<GeminiProcessResult> = withContext(Dispatchers.IO) {
        _isProcessing.value = true
        _processingStatus.value = "Analyzing recorded audio..."

        try {
            _processingStatus.value = "Transcribing speech into note content..."
            val geminiResult = geminiService.processAudio(
                audioFile = audioFile,
                mimeType = "audio/mp4",
                formatStyle = formatStyle,
                customInstruction = customInstruction
            )

            if (geminiResult.isSuccess) {
                val data = geminiResult.getOrThrow()
                _processingStatus.value = "Transcription complete"
                return@withContext Result.success(data)
            }

            // Fallback: If Gemini API had an issue, check if we have a live transcript from local speech recognizer
            val fallbackTranscript = _liveTranscript.value.trim()
            if (fallbackTranscript.isNotBlank()) {
                _processingStatus.value = "Formatting local transcription..."
                val localStructuredResult = generateLocalStructuredNote(fallbackTranscript, formatStyle)
                return@withContext Result.success(localStructuredResult)
            }

            // Return original failure if no fallback transcript
            val error = geminiResult.exceptionOrNull() ?: Exception("Audio transcription failed")
            Result.failure(error)
        } catch (e: Exception) {
            Log.e("RealtimeAudioToText", "Error in audio-to-text transcription service", e)
            val fallbackTranscript = _liveTranscript.value.trim()
            if (fallbackTranscript.isNotBlank()) {
                val localStructuredResult = generateLocalStructuredNote(fallbackTranscript, formatStyle)
                Result.success(localStructuredResult)
            } else {
                Result.failure(e)
            }
        } finally {
            _isProcessing.value = false
        }
    }

    /**
     * Local fallback formatter that generates title, polished text, summary, and takeaways
     * from transcript when offline or without an API key.
     */
    private fun generateLocalStructuredNote(
        rawText: String,
        style: FormatStyle
    ): GeminiProcessResult {
        val sentences = rawText.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }
        val title = if (sentences.isNotEmpty()) {
            sentences.first().take(40).trim().removeSuffix(".")
        } else {
            "Voice Note (${System.currentTimeMillis() % 10000})"
        }

        val polished = when (style) {
            FormatStyle.BULLET_MEMO, FormatStyle.TODO_LIST -> {
                sentences.joinToString("\n") { "• $it" }
            }
            FormatStyle.EMAIL -> {
                "Hi Team,\n\n$rawText\n\nBest regards,"
            }
            else -> rawText
        }

        val summary = if (sentences.size > 1) sentences.take(2).joinToString(" ") else rawText
        val takeaways = sentences.take(3)
        val tags = listOf("VoiceNote", style.name.lowercase().replaceFirstChar { it.uppercase() })

        return GeminiProcessResult(
            title = title,
            polishedText = polished,
            rawTranscript = rawText,
            summary = summary,
            keyTakeaways = takeaways,
            tags = tags,
            language = "Auto"
        )
    }
}
