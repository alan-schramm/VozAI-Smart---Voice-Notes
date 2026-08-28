package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.audio.RecorderState
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueDark
import com.example.ui.theme.ElectricBlueGlow
import com.example.ui.theme.TextPrimary

@Composable
fun HeroRecordButton(
    recorderState: RecorderState,
    amplitude: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRecording = recorderState == RecorderState.RECORDING
    val isPaused = recorderState == RecorderState.PAUSED

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_rings")

    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse1"
    )

    val pulseAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha1"
    )

    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, delayMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse2"
    )

    val pulseAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, delayMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha2"
    )

    val buttonColor by animateColorAsState(
        targetValue = when {
            isRecording -> CrimsonAccent
            isPaused -> ElectricBlueDark
            else -> ElectricBlue
        },
        label = "btnColor"
    )

    Box(
        modifier = modifier
            .size(160.dp)
            .semantics {
                contentDescription = if (isRecording) "Stop voice recording" else "Start voice recording"
            },
        contentAlignment = Alignment.Center
    ) {
        // Pulsing Rings (Active when recording or breathing)
        if (isRecording) {
            Canvas(modifier = Modifier.size(150.dp)) {
                // Ring 2
                drawCircle(
                    color = ElectricBlueGlow.copy(alpha = pulseAlpha2),
                    radius = (size.minDimension / 2f) * (pulseScale2 * (0.8f + amplitude * 0.2f))
                )
                // Ring 1
                drawCircle(
                    color = ElectricBlue.copy(alpha = pulseAlpha1),
                    radius = (size.minDimension / 2f) * (pulseScale1 * (0.85f + amplitude * 0.15f))
                )
            }
        }

        // Ambient glow around main button
        Box(
            modifier = Modifier
                .size(96.dp)
                .scale(if (isRecording) 1f + (amplitude * 0.15f) else 1f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            buttonColor.copy(alpha = 0.45f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Main Tap Target Button
        Box(
            modifier = Modifier
                .size(84.dp)
                .shadow(
                    elevation = if (isRecording) 16.dp else 8.dp,
                    shape = CircleShape,
                    spotColor = buttonColor
                )
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            buttonColor,
                            buttonColor.copy(alpha = 0.85f)
                        )
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = TextPrimary),
                    onClick = onClick
                )
                .testTag(if (isRecording) "stop_record_button" else "start_record_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isRecording) "Stop Recording" else "Record Audio",
                tint = TextPrimary,
                modifier = Modifier.size(38.dp)
            )
        }
    }
}
