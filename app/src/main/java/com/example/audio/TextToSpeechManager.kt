package com.example.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TextToSpeechManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _speechRate = MutableStateFlow(1.0f)
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _currentlySpeakingId = MutableStateFlow<String?>(null)
    val currentlySpeakingId: StateFlow<String?> = _currentlySpeakingId.asStateFlow()

    init {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
                _currentlySpeakingId.value = utteranceId
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
                _currentlySpeakingId.value = null
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
                _currentlySpeakingId.value = null
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _isSpeaking.value = false
                _currentlySpeakingId.value = null
                Log.e("TextToSpeechManager", "TTS error on $utteranceId code: $errorCode")
            }
        })
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            // Try setting Portuguese first, fallback to default/English
            val result = tts?.setLanguage(Locale("pt", "BR"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.getDefault())
            }
            tts?.setPitch(1.0f)
            tts?.setSpeechRate(1.05f)
        } else {
            Log.e("TextToSpeechManager", "TTS initialization failed: $status")
            isInitialized = false
        }
    }

    /**
     * Speaks the provided text aloud. Cleans markdown asterisks and hashtags for clean speech.
     */
    fun speak(text: String, utteranceId: String, preferredLanguage: String? = null) {
        if (!isInitialized || tts == null) {
            return
        }

        // Auto-adapt language if detected
        if (!preferredLanguage.isNullOrBlank()) {
            when {
                preferredLanguage.contains("pt", ignoreCase = true) || preferredLanguage.contains("portug", ignoreCase = true) -> {
                    tts?.setLanguage(Locale("pt", "BR"))
                }
                preferredLanguage.contains("en", ignoreCase = true) || preferredLanguage.contains("ingl", ignoreCase = true) -> {
                    tts?.setLanguage(Locale.US)
                }
                preferredLanguage.contains("es", ignoreCase = true) || preferredLanguage.contains("span", ignoreCase = true) -> {
                    tts?.setLanguage(Locale("es", "ES"))
                }
                else -> {
                    tts?.setLanguage(Locale.getDefault())
                }
            }
        }

        val cleanedText = cleanTextForSpeech(text)
        if (cleanedText.isBlank()) return

        stop()

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        tts?.setSpeechRate(_speechRate.value)
        tts?.speak(cleanedText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun setSpeechRate(rate: Float) {
        val clamped = rate.coerceIn(0.5f, 2.5f)
        _speechRate.value = clamped
        tts?.setSpeechRate(clamped)
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
        _isSpeaking.value = false
        _currentlySpeakingId.value = null
    }

    fun shutdown() {
        stop()
        try {
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
        isInitialized = false
    }

    private fun cleanTextForSpeech(text: String): String {
        return text
            .replace(Regex("[*#_`~>]"), "") // Strip markdown symbols
            .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1") // Replace links with link text
            .replace(Regex("(?m)^[-•*]\\s+"), "") // Remove bullet points markers
            .replace(Regex("\\n+"), ". ") // Replace newlines with sentence pauses
            .trim()
    }
}
