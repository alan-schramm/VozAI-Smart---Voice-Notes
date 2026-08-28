package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueGlow
import com.example.ui.theme.ElectricBlueLight
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldGlow
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Visual waveform animation built with Jetpack Compose Canvas API.
 * Dynamically reacts to real-time microphone audio intensity with:
 * - Fluid multi-bar frequency equalizer with rounded bars and vertical color gradients.
 * - Glowing bezier fluid wave ribbon pulsating with instantaneous voice amplitude.
 * - Reactive central energy glow halo expanding and contracting based on volume.
 * - Peak intensity indicator particles bouncing above high-volume audio crests.
 * - Smooth state transitions between Idle, Recording, and Paused modes.
 */
@Composable
fun AudioWaveVisualizer(
    isRecording: Boolean,
    amplitude: Float,
    amplitudeHistory: List<Float>,
    modifier: Modifier = Modifier,
    isPaused: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "audio_canvas_anim")

    // Phase animation for continuous fluid sinusoidal wave motion
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    // Secondary harmonic phase for multi-layered wave physics
    val harmonicPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -(2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "harmonic_phase"
    )

    // Gentle breathing scale for idle resting state
    val idleBreathing by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_breathing"
    )

    // Smooth instantaneous amplitude to prevent jarring jumps while speaking
    val smoothedAmplitude by animateFloatAsState(
        targetValue = if (isRecording) amplitude.coerceIn(0.04f, 1.0f) else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "smoothed_amplitude"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val centerY = canvasHeight / 2f
        val centerX = canvasWidth / 2f

        // 1. Central Ambient Audio Glow Halo (Reacts to Real-Time Voice Intensity)
        if (isRecording && smoothedAmplitude > 0.12f) {
            val glowRadius = (canvasWidth * 0.35f * smoothedAmplitude).coerceIn(40f, canvasWidth * 0.45f)
            val glowAlpha = (smoothedAmplitude * 0.35f).coerceIn(0.05f, 0.4f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ElectricBlueGlow.copy(alpha = glowAlpha),
                        EmeraldGlow.copy(alpha = glowAlpha * 0.4f),
                        Color.Transparent
                    ),
                    center = Offset(centerX, centerY),
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = Offset(centerX, centerY)
            )
        }

        // 2. Background Continuous Fluid Wave Ribbon (Bezier path with Canvas API)
        drawFluidWaveRibbon(
            isRecording = isRecording,
            isPaused = isPaused,
            amplitude = smoothedAmplitude,
            phase = phase,
            harmonicPhase = harmonicPhase,
            idleBreathing = idleBreathing,
            centerY = centerY,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight
        )

        // 3. Central Baseline Axis Line
        val baselineAlpha = if (isRecording) 0.35f else 0.15f
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    (if (isPaused) AmberAccent else ElectricBlue).copy(alpha = baselineAlpha),
                    (if (isPaused) AmberAccent else EmeraldAccent).copy(alpha = baselineAlpha),
                    Color.Transparent
                )
            ),
            start = Offset(0f, centerY),
            end = Offset(canvasWidth, centerY),
            strokeWidth = 1.dp.toPx()
        )

        // 4. Foreground Multi-Bar Equalizer Waveform
        val barCount = 32
        val barSpacing = 4.dp.toPx()
        val totalSpacing = barSpacing * (barCount - 1)
        val barWidth = ((canvasWidth - totalSpacing) / barCount).coerceAtLeast(3.5.dp.toPx())

        val recordingGradient = Brush.verticalGradient(
            colors = listOf(
                ElectricBlueGlow,
                ElectricBlueLight,
                ElectricBlue,
                EmeraldAccent
            ),
            startY = 0f,
            endY = canvasHeight
        )

        val pausedGradient = Brush.verticalGradient(
            colors = listOf(
                AmberAccent.copy(alpha = 0.9f),
                AmberAccent.copy(alpha = 0.6f),
                Color(0xFF8D6E63)
            ),
            startY = 0f,
            endY = canvasHeight
        )

        val idleGradient = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF334155),
                Color(0xFF1E293B)
            ),
            startY = 0f,
            endY = canvasHeight
        )

        for (i in 0 until barCount) {
            val x = i * (barWidth + barSpacing)

            // Normalized distance from center (0 at center, 1 at edges) for acoustic bell curve
            val distanceFromCenter = abs(i - (barCount - 1) / 2f) / ((barCount - 1) / 2f)
            val bellWeight = (1.0f - distanceFromCenter * 0.45f).coerceIn(0.4f, 1.0f)

            val barHeight = when {
                isRecording -> {
                    val historyIdx = amplitudeHistory.size - barCount + i
                    val historicalAmp = if (historyIdx in amplitudeHistory.indices) {
                        amplitudeHistory[historyIdx]
                    } else {
                        smoothedAmplitude
                    }

                    // Harmonic sine wave perturbation for natural audio fluidity
                    val sineModulation = sin(phase + i * 0.38f) * 0.22f + cos(harmonicPhase + i * 0.25f) * 0.15f
                    val combinedIntensity = (historicalAmp * 0.75f + smoothedAmplitude * 0.25f + sineModulation)
                        .coerceIn(0.08f, 1.0f)

                    (canvasHeight * 0.88f * combinedIntensity * bellWeight).coerceAtLeast(6.dp.toPx())
                }
                isPaused -> {
                    // Frozen resting pulse
                    val pauseWave = (sin(i * 0.4f) * 0.2f + 0.35f)
                    (canvasHeight * 0.35f * pauseWave * bellWeight).coerceAtLeast(5.dp.toPx())
                }
                else -> {
                    // Idle breathing wave
                    val idleWave = (sin(phase + i * 0.28f) * 0.25f + 0.35f) * idleBreathing
                    (canvasHeight * 0.38f * idleWave * bellWeight).coerceAtLeast(4.dp.toPx())
                }
            }

            val top = centerY - barHeight / 2f
            val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)

            val barBrush = when {
                isRecording -> recordingGradient
                isPaused -> pausedGradient
                else -> idleGradient
            }

            // Draw rounded frequency equalizer bar
            drawRoundRect(
                brush = barBrush,
                topLeft = Offset(x, top),
                size = Size(barWidth, barHeight),
                cornerRadius = cornerRadius
            )

            // 5. Peak Floating Indicator Droplet for high-energy spoken words
            if (isRecording && smoothedAmplitude > 0.35f && barHeight > canvasHeight * 0.4f) {
                val peakGap = (4.dp.toPx() + smoothedAmplitude * 6.dp.toPx())
                val peakY = top - peakGap
                if (peakY > 2.dp.toPx()) {
                    val peakRadius = (barWidth * 0.45f).coerceIn(1.5f, 3.5.dp.toPx())
                    drawCircle(
                        color = ElectricBlueGlow.copy(alpha = (smoothedAmplitude * 0.9f).coerceIn(0.4f, 1.0f)),
                        radius = peakRadius,
                        center = Offset(x + barWidth / 2f, peakY)
                    )
                }
            }
        }
    }
}

/**
 * Draws a flowing sinusoidal energy ribbon across the canvas background.
 */
private fun DrawScope.drawFluidWaveRibbon(
    isRecording: Boolean,
    isPaused: Boolean,
    amplitude: Float,
    phase: Float,
    harmonicPhase: Float,
    idleBreathing: Float,
    centerY: Float,
    canvasWidth: Float,
    canvasHeight: Float
) {
    val path = Path()
    val steps = 30
    val stepWidth = canvasWidth / steps

    val waveAmp = when {
        isRecording -> (canvasHeight * 0.28f * (amplitude * 0.8f + 0.2f)).coerceIn(4f, canvasHeight * 0.4f)
        isPaused -> canvasHeight * 0.08f
        else -> canvasHeight * 0.12f * idleBreathing
    }

    path.moveTo(0f, centerY + sin(phase) * waveAmp)

    for (step in 1..steps) {
        val prevX = (step - 1) * stepWidth
        val currentX = step * stepWidth
        val prevY = centerY + sin(phase + (step - 1) * 0.4f) * waveAmp + cos(harmonicPhase + (step - 1) * 0.2f) * (waveAmp * 0.4f)
        val currentY = centerY + sin(phase + step * 0.4f) * waveAmp + cos(harmonicPhase + step * 0.2f) * (waveAmp * 0.4f)

        val controlX = (prevX + currentX) / 2f
        val controlY = (prevY + currentY) / 2f

        path.quadraticTo(prevX, prevY, controlX, controlY)
    }

    val waveColor = when {
        isRecording -> ElectricBlue.copy(alpha = 0.35f + amplitude * 0.35f)
        isPaused -> AmberAccent.copy(alpha = 0.25f)
        else -> CharcoalBorder.copy(alpha = 0.4f)
    }

    drawPath(
        path = path,
        color = waveColor,
        style = Stroke(
            width = if (isRecording) 2.2.dp.toPx() else 1.5.dp.toPx(),
            cap = StrokeCap.Round
        )
    )
}
