package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalCard
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CharcoalSurfaceVariant
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueDark
import com.example.ui.theme.ElectricBlueGlow
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun TtsPlayerBar(
    isSpeaking: Boolean,
    isThisNoteSpeaking: Boolean,
    currentSpeed: Float,
    onTogglePlay: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val speedOptions = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tts_player_bar_card")
            .border(
                1.dp,
                if (isThisNoteSpeaking) {
                    Brush.horizontalGradient(listOf(ElectricBlueGlow, EmeraldAccent))
                } else {
                    Brush.horizontalGradient(listOf(CharcoalBorder, CharcoalBorder))
                },
                RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalCard)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Player Top Row: Voice Icon, Title & Play/Stop Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (isThisNoteSpeaking) ElectricBlueGlow.copy(alpha = 0.2f)
                                else CharcoalSurfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isThisNoteSpeaking) Icons.Default.VolumeUp else Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = if (isThisNoteSpeaking) ElectricBlueGlow else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isThisNoteSpeaking) "READING ALOUD (TTS)" else "LISTEN TO NOTE",
                                color = if (isThisNoteSpeaking) ElectricBlueGlow else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }
                        Text(
                            text = if (isThisNoteSpeaking) "High clarity neural narration" else "Text-to-speech audio reader",
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Play / Stop Master Button
                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isThisNoteSpeaking) EmeraldAccent else ElectricBlue)
                        .testTag("tts_play_stop_button")
                ) {
                    Icon(
                        imageVector = if (isThisNoteSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isThisNoteSpeaking) "Stop Speaking" else "Play Note Audio",
                        tint = CharcoalSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Speed Control Chips Row
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Speed:",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    speedOptions.forEach { speed ->
                        val isSelected = kotlin.math.abs(currentSpeed - speed) < 0.05f
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) ElectricBlueDark.copy(alpha = 0.8f)
                                    else CharcoalSurface
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) ElectricBlue else CharcoalBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onSetSpeed(speed) }
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                                .testTag("tts_speed_${speed}x")
                        ) {
                            Text(
                                text = "${speed}x",
                                color = if (isSelected) ElectricBlueGlow else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}
