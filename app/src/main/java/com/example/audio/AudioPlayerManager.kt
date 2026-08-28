package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
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

enum class PlayerState {
    IDLE,
    PLAYING,
    PAUSED,
    COMPLETED,
    ERROR
}

/**
 * MediaPlayer wrapper to play back recorded voice notes with seekbar and playback rate controls
 */
class AudioPlayerManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _playerState = MutableStateFlow(PlayerState.IDLE)
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0)
    val currentPositionMs: StateFlow<Int> = _currentPositionMs.asStateFlow()

    private val _totalDurationMs = MutableStateFlow(0)
    val totalDurationMs: StateFlow<Int> = _totalDurationMs.asStateFlow()

    private val _currentPlayingPath = MutableStateFlow<String?>(null)
    val currentPlayingPath: StateFlow<String?> = _currentPlayingPath.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    fun playAudioFile(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            Log.e("AudioPlayerManager", "Audio file not found at $filePath")
            _playerState.value = PlayerState.ERROR
            return
        }

        // If already playing this file and paused, resume
        if (_currentPlayingPath.value == filePath && mediaPlayer != null) {
            if (_playerState.value == PlayerState.PAUSED) {
                resumePlayback()
                return
            } else if (_playerState.value == PlayerState.PLAYING) {
                pausePlayback()
                return
            }
        }

        stopPlayback()

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, Uri.fromFile(file))
                prepare()
            }

            _totalDurationMs.value = player.duration
            _currentPositionMs.value = 0
            _currentPlayingPath.value = filePath

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    player.playbackParams = player.playbackParams.setSpeed(_playbackSpeed.value)
                } catch (e: Exception) {
                    Log.w("AudioPlayerManager", "Could not set playback speed", e)
                }
            }

            player.setOnCompletionListener {
                _playerState.value = PlayerState.COMPLETED
                _currentPositionMs.value = _totalDurationMs.value
                stopProgressTracking()
            }

            player.setOnErrorListener { _, what, extra ->
                Log.e("AudioPlayerManager", "MediaPlayer error: what=$what, extra=$extra")
                _playerState.value = PlayerState.ERROR
                stopPlayback()
                true
            }

            player.start()
            mediaPlayer = player
            _playerState.value = PlayerState.PLAYING

            startProgressTracking()
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Failed to initialize MediaPlayer", e)
            _playerState.value = PlayerState.ERROR
            cleanup()
        }
    }

    fun pausePlayback() {
        if (_playerState.value == PlayerState.PLAYING) {
            try {
                mediaPlayer?.pause()
                _playerState.value = PlayerState.PAUSED
                stopProgressTracking()
            } catch (e: Exception) {
                Log.e("AudioPlayerManager", "Error pausing playback", e)
            }
        }
    }

    fun resumePlayback() {
        if (_playerState.value == PlayerState.PAUSED || _playerState.value == PlayerState.COMPLETED) {
            try {
                if (_playerState.value == PlayerState.COMPLETED) {
                    mediaPlayer?.seekTo(0)
                    _currentPositionMs.value = 0
                }
                mediaPlayer?.start()
                _playerState.value = PlayerState.PLAYING
                startProgressTracking()
            } catch (e: Exception) {
                Log.e("AudioPlayerManager", "Error resuming playback", e)
            }
        }
    }

    fun togglePlayPause(filePath: String) {
        if (_currentPlayingPath.value == filePath && _playerState.value == PlayerState.PLAYING) {
            pausePlayback()
        } else if (_currentPlayingPath.value == filePath && _playerState.value == PlayerState.PAUSED) {
            resumePlayback()
        } else {
            playAudioFile(filePath)
        }
    }

    fun seekTo(positionMs: Int) {
        try {
            mediaPlayer?.seekTo(positionMs)
            _currentPositionMs.value = positionMs
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error seeking MediaPlayer", e)
        }
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mediaPlayer != null && _playerState.value == PlayerState.PLAYING) {
            try {
                mediaPlayer?.let { player ->
                    player.playbackParams = player.playbackParams.setSpeed(speed)
                }
            } catch (e: Exception) {
                Log.w("AudioPlayerManager", "Error setting playback speed", e)
            }
        }
    }

    fun stopPlayback() {
        stopProgressTracking()
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error stopping player", e)
        } finally {
            mediaPlayer = null
            _playerState.value = PlayerState.IDLE
            _currentPositionMs.value = 0
            _totalDurationMs.value = 0
            _currentPlayingPath.value = null
        }
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _playerState.value == PlayerState.PLAYING) {
                mediaPlayer?.let { player ->
                    try {
                        if (player.isPlaying) {
                            _currentPositionMs.value = player.currentPosition
                        }
                    } catch (_: Exception) {}
                }
                delay(100)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun cleanup() {
        stopPlayback()
    }
}
