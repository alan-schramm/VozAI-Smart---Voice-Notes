package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioPlayerManager
import com.example.audio.PlayerState
import com.example.ui.theme.CharcoalBackground
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalCard
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CharcoalSurfaceVariant
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueDark
import com.example.ui.theme.ElectricBlueGlow
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import java.io.File
import java.util.Locale

@Composable
fun VoiceMemoPlayerCard(
    audioFilePath: String?,
    audioDurationSeconds: Int,
    audioPlayerManager: AudioPlayerManager,
    modifier: Modifier = Modifier
) {
    if (audioFilePath.isNullOrBlank()) return
    val file = File(audioFilePath)
    if (!file.exists() || file.length() == 0L) return

    val playerState by audioPlayerManager.playerState.collectAsState()
    val currentPath by audioPlayerManager.currentPlayingPath.collectAsState()
    val currentPosMs by audioPlayerManager.currentPositionMs.collectAsState()
    val totalDurMs by audioPlayerManager.totalDurationMs.collectAsState()
    val playbackSpeed by audioPlayerManager.playbackSpeed.collectAsState()

    val isThisPlaying = currentPath == audioFilePath && playerState == PlayerState.PLAYING
    val isThisActive = currentPath == audioFilePath

    val durationMs = if (isThisActive && totalDurMs > 0) totalDurMs else (audioDurationSeconds * 1000).coerceAtLeast(1000)
    val displayPosMs = if (isThisActive) currentPosMs else 0

    var isUserDraggingSlider by remember { mutableStateOf(false) }
    var userSliderPos by remember { mutableStateOf(0f) }

    val progress = if (isUserDraggingSlider) {
        userSliderPos
    } else {
        (displayPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp))
            .testTag("voice_memo_player_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Badge & Recording details
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ElectricBlueDark.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Original voice recording",
                            tint = ElectricBlueGlow,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Original Voice Note",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Recorded directly via MediaRecorder (AAC)",
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Speed Selector Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = CharcoalSurface,
                    modifier = Modifier
                        .clickable {
                            val nextSpeed = when (playbackSpeed) {
                                1.0f -> 1.25f
                                1.25f -> 1.5f
                                1.5f -> 2.0f
                                else -> 1.0f
                            }
                            audioPlayerManager.setSpeed(nextSpeed)
                        }
                        .border(1.dp, CharcoalBorder, RoundedCornerShape(20.dp))
                        .testTag("playback_speed_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = ElectricBlueGlow,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${playbackSpeed}x",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Seekbar
            Slider(
                value = progress,
                onValueChange = { newValue ->
                    isUserDraggingSlider = true
                    userSliderPos = newValue
                },
                onValueChangeFinished = {
                    isUserDraggingSlider = false
                    val targetMs = (userSliderPos * durationMs).toInt()
                    if (isThisActive) {
                        audioPlayerManager.seekTo(targetMs)
                    } else {
                        audioPlayerManager.playAudioFile(audioFilePath)
                        audioPlayerManager.seekTo(targetMs)
                    }
                },
                colors = SliderDefaults.colors(
                    thumbColor = ElectricBlueGlow,
                    activeTrackColor = ElectricBlue,
                    inactiveTrackColor = CharcoalSurfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .testTag("voice_player_slider")
            )

            // Timers & Play Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val currentSeconds = (displayPosMs / 1000)
                val totalSeconds = (durationMs / 1000)
                Text(
                    text = String.format(Locale.US, "%d:%02d / %d:%02d", currentSeconds / 60, currentSeconds % 60, totalSeconds / 60, totalSeconds % 60),
                    color = TextTertiary,
                    fontSize = 11.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Rewind 5s
                    IconButton(
                        onClick = {
                            val newPos = (displayPosMs - 5000).coerceAtLeast(0)
                            if (isThisActive) {
                                audioPlayerManager.seekTo(newPos)
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("rewind_5s_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rewind",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Play/Pause Hero Button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ElectricBlue)
                            .clickable {
                                audioPlayerManager.togglePlayPause(audioFilePath)
                            }
                            .testTag("voice_player_play_pause_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isThisPlaying) "Pause voice note" else "Play voice note",
                            tint = CharcoalBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
