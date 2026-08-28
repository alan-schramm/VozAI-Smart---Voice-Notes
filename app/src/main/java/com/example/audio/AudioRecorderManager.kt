package com.example.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

enum class RecorderState {
    IDLE,
    RECORDING,
    PAUSED
}

/**
 * Robust MediaRecorder wrapper for recording voice notes directly on Android.
 * Captures high-quality AAC/M4A audio in app-persistent storage with real-time waveform tracking.
 */
class AudioRecorderManager(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var amplitudeJob: Job? = null
    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _recorderState = MutableStateFlow(RecorderState.IDLE)
    val recorderState: StateFlow<RecorderState> = _recorderState.asStateFlow()

    private val _currentAmplitude = MutableStateFlow(0f)
    val currentAmplitude: StateFlow<Float> = _currentAmplitude.asStateFlow()

    private val _amplitudeHistory = MutableStateFlow<List<Float>>(emptyList())
    val amplitudeHistory: StateFlow<List<Float>> = _amplitudeHistory.asStateFlow()

    private val _durationSeconds = MutableStateFlow(0)
    val durationSeconds: StateFlow<Int> = _durationSeconds.asStateFlow()

    private val _recordingError = MutableStateFlow<String?>(null)
    val recordingError: StateFlow<String?> = _recordingError.asStateFlow()

    /**
     * Directory dedicated to voice recordings
     */
    private val recordingsDir: File
        get() {
            val dir = File(context.filesDir, "voice_notes")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    /**
     * Start recording voice audio via Android MediaRecorder API
     */
    fun startRecording(): Result<File> {
        if (_recorderState.value != RecorderState.IDLE) {
            stopRecording()
        }

        _recordingError.value = null

        val outputFile = File(
            recordingsDir,
            "audiopen_${System.currentTimeMillis()}.m4a"
        )
        currentOutputFile = outputFile

        return try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setAudioChannels(1)
                setOutputFile(outputFile.absolutePath)

                setOnErrorListener { _, what, extra ->
                    Log.e("AudioRecorderManager", "MediaRecorder error: what=$what, extra=$extra")
                    _recordingError.value = "Recording error occurred ($what)"
                }

                setOnInfoListener { _, what, extra ->
                    Log.i("AudioRecorderManager", "MediaRecorder info: what=$what, extra=$extra")
                }

                prepare()
                start()
            }

            mediaRecorder = recorder
            _recorderState.value = RecorderState.RECORDING
            _durationSeconds.value = 0
            _amplitudeHistory.value = emptyList()

            startAmplitudePolling()
            startTimer()

            Result.success(outputFile)
        } catch (e: Exception) {
            Log.e("AudioRecorderManager", "Failed to start MediaRecorder", e)
            _recordingError.value = e.localizedMessage ?: "Failed to start MediaRecorder"
            cleanup()
            Result.failure(e)
        }
    }

    /**
     * Pause active recording (API 24+)
     */
    fun pauseRecording() {
        if (_recorderState.value == RecorderState.RECORDING && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.pause()
                _recorderState.value = RecorderState.PAUSED
            } catch (e: Exception) {
                Log.e("AudioRecorderManager", "Failed to pause MediaRecorder", e)
            }
        }
    }

    /**
     * Resume active recording (API 24+)
     */
    fun resumeRecording() {
        if (_recorderState.value == RecorderState.PAUSED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                mediaRecorder?.resume()
                _recorderState.value = RecorderState.RECORDING
            } catch (e: Exception) {
                Log.e("AudioRecorderManager", "Failed to resume MediaRecorder", e)
            }
        }
    }

    /**
     * Stop recording and return the saved audio file
     */
    fun stopRecording(): File? {
        stopJobs()
        val file = currentOutputFile
        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: RuntimeException) {
                    // Happens if stop is called immediately after start without valid audio frames
                    Log.w("AudioRecorderManager", "MediaRecorder stop failed immediately", e)
                }
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioRecorderManager", "Error stopping MediaRecorder", e)
        } finally {
            mediaRecorder = null
            _recorderState.value = RecorderState.IDLE
            _currentAmplitude.value = 0f
        }
        return file
    }

    /**
     * Cancel recording and purge temporary audio file
     */
    fun cancelRecording() {
        stopJobs()
        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (_: Exception) {}
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioRecorderManager", "Error cancelling MediaRecorder", e)
        } finally {
            mediaRecorder = null
            _recorderState.value = RecorderState.IDLE
            _currentAmplitude.value = 0f
            currentOutputFile?.delete()
            currentOutputFile = null
            _durationSeconds.value = 0
            _amplitudeHistory.value = emptyList()
        }
    }

    /**
     * Poll audio amplitude from MediaRecorder for fluid real-time waveform visualization
     */
    private fun startAmplitudePolling() {
        amplitudeJob?.cancel()
        amplitudeJob = scope.launch(Dispatchers.Default) {
            val maxHistory = 40
            while (isActive && _recorderState.value != RecorderState.IDLE) {
                if (_recorderState.value == RecorderState.RECORDING) {
                    val maxAmp = try {
                        mediaRecorder?.maxAmplitude ?: 0
                    } catch (e: Exception) {
                        0
                    }
                    // Normalize 16-bit PCM amplitude (0..32767) into fluid 0.05f..1.0f curve
                    val rawNormalized = (maxAmp / 24000f).coerceIn(0f, 1f)
                    // Logarithmic/power curve for natural human auditory perception
                    val perceptualAmp = (Math.pow(rawNormalized.toDouble(), 0.65).toFloat()).coerceIn(0.04f, 1f)
                    _currentAmplitude.value = perceptualAmp

                    val updatedHistory = (_amplitudeHistory.value + perceptualAmp).takeLast(maxHistory)
                    _amplitudeHistory.value = updatedHistory
                }
                delay(45)
            }
        }
    }

    /**
     * Track elapsed recording time in seconds
     */
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch(Dispatchers.Default) {
            while (isActive && _recorderState.value != RecorderState.IDLE) {
                if (_recorderState.value == RecorderState.RECORDING) {
                    _durationSeconds.value += 1
                }
                delay(1000)
            }
        }
    }

    private fun stopJobs() {
        amplitudeJob?.cancel()
        timerJob?.cancel()
    }

    private fun cleanup() {
        stopJobs()
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null
        _recorderState.value = RecorderState.IDLE
    }
}
