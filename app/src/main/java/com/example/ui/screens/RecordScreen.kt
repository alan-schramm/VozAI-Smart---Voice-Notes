package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.audio.RecorderState
import com.example.ui.components.AudioWaveVisualizer
import com.example.ui.components.FormatStyleSelector
import com.example.ui.components.HeroRecordButton
import com.example.ui.theme.CharcoalBackground
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalCard
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CharcoalSurfaceVariant
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueDark
import com.example.ui.theme.ElectricBlueGlow
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.AudioPenViewModel

@Composable
fun RecordScreen(
    viewModel: AudioPenViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recorderState by viewModel.recorderManager.recorderState.collectAsState()
    val amplitude by viewModel.recorderManager.currentAmplitude.collectAsState()
    val amplitudeHistory by viewModel.recorderManager.amplitudeHistory.collectAsState()
    val durationSeconds by viewModel.recorderManager.durationSeconds.collectAsState()
    val selectedFormat by viewModel.selectedFormat.collectAsState()
    val allNotes by viewModel.allNotes.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val liveTranscript by viewModel.liveTranscript.collectAsState()
    val isLiveListening by viewModel.isLiveListening.collectAsState()

    var showTextFallback by remember { mutableStateOf(false) }
    var fallbackText by remember { mutableStateOf("") }

    val isRecording = recorderState == RecorderState.RECORDING
    val isPaused = recorderState == RecorderState.PAUSED
    val isIdle = recorderState == RecorderState.IDLE

    // Audio Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startRecording()
        } else {
            Toast.makeText(context, "Microphone permission is needed to record voice notes", Toast.LENGTH_SHORT).show()
        }
    }

    // Audio File Import Launcher (mp3, m4a, wav, aac, etc.)
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importAudioFromUri(uri, context)
        }
    }

    val handleRecordButtonClick = {
        if (isIdle) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                viewModel.startRecording()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        } else {
            // Finish recording and process
            viewModel.finishRecordingAndProcess()
        }
    }

    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    val formattedDuration = String.format("%02d:%02d", minutes, seconds)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBackground)
            .statusBarsPadding()
    ) {
        // --- TOP APP BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand Logo & Title
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(ElectricBlue, ElectricBlueDark)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "AudioPen",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    )
                    Text(
                        text = "Executive AI Speech to Text",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            // Top Action Buttons (AI Agent + History + Settings)
            Row(verticalAlignment = Alignment.CenterVertically) {
                // AI Agent / Idea Partner Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(ElectricBlue.copy(alpha = 0.2f), CharcoalCard)
                            )
                        )
                        .border(1.dp, ElectricBlue.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .clickable { viewModel.navigateTo(AppScreen.AGENT) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("open_agent_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Forum,
                            contentDescription = "Agente de Ideias",
                            tint = ElectricBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI Agent",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = { viewModel.navigateTo(AppScreen.HISTORY) },
                    modifier = Modifier.testTag("history_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (allNotes.isNotEmpty()) {
                                Badge(
                                    containerColor = ElectricBlue,
                                    contentColor = TextPrimary
                                ) {
                                    Text(
                                        text = allNotes.size.toString(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Saved Notes Library",
                            tint = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.setSettingsDialogVisible(true) },
                    modifier = Modifier.testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Preferences",
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // --- ERROR BANNER (if any) ---
        errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = CrimsonAccent.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = error,
                        color = CrimsonAccent,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.clearError() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss error",
                            tint = CrimsonAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // --- FORMAT STYLE SELECTOR ---
        Column(modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)) {
            Text(
                text = "FORMAT STYLE",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
            FormatStyleSelector(
                selectedStyle = selectedFormat,
                onSelectStyle = { viewModel.selectFormatStyle(it) }
            )
        }

        // --- CENTER HERO RECORDING AREA ---
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Live Timer & Status
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    text = if (isIdle) "00:00" else formattedDuration,
                    color = if (isRecording) ElectricBlueGlow else TextPrimary,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = when {
                        isRecording -> "Listening to your thoughts..."
                        isPaused -> "Recording paused"
                        else -> "Tap the microphone and start speaking"
                    },
                    color = when {
                        isRecording -> EmeraldAccent
                        isPaused -> TextSecondary
                        else -> TextSecondary
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            // Live Waveform Visualizer with real-time Canvas animation
            AudioWaveVisualizer(
                isRecording = isRecording,
                isPaused = isPaused,
                amplitude = amplitude,
                amplitudeHistory = amplitudeHistory,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Real-Time Speech-to-Text Live Preview (While Recording)
            if (!isIdle && liveTranscript.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("live_speech_transcript_preview"),
                    colors = CardDefaults.cardColors(containerColor = CharcoalCard),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(ElectricBlue.copy(alpha = 0.6f), EmeraldAccent.copy(alpha = 0.4f)))
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(EmeraldAccent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = liveTranscript,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            maxLines = 2,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Giant Hero Record Button
            HeroRecordButton(
                recorderState = recorderState,
                amplitude = amplitude,
                onClick = handleRecordButtonClick
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action Controls when Active (Cancel, Pause, Done)
            AnimatedVisibility(
                visible = !isIdle,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Discard / Trash Button
                    IconButton(
                        onClick = { viewModel.cancelRecording() },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(CharcoalCard)
                            .border(1.dp, CharcoalBorder, CircleShape)
                            .testTag("discard_recording_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Discard recording",
                            tint = CrimsonAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Pause / Resume Button
                    IconButton(
                        onClick = {
                            if (isRecording) viewModel.pauseRecording() else viewModel.resumeRecording()
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(CharcoalCard)
                            .border(1.dp, CharcoalBorder, CircleShape)
                            .testTag("pause_resume_recording_button")
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isRecording) "Pause recording" else "Resume recording",
                            tint = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Finish & Polish Button
                    IconButton(
                        onClick = { viewModel.finishRecordingAndProcess() },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(ElectricBlue)
                            .testTag("finish_recording_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Finish and Polish",
                            tint = TextPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Hint / Tip Card in Idle State
            if (isIdle) {
                // Interactive AI Agent Banner
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .clickable { viewModel.navigateTo(AppScreen.AGENT) },
                    colors = CardDefaults.cardColors(containerColor = CharcoalCard),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(
                            listOf(ElectricBlue.copy(alpha = 0.5f), CharcoalBorder)
                        )
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(ElectricBlue.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = ElectricBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Conversar com o Agente de IA",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(ElectricBlue)
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "VOZ & TEXTO",
                                        color = TextPrimary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Debata suas ideias em uma conversa por áudio ou texto e ouça a IA responder por voz.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = CharcoalCard),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(CharcoalBorder, CharcoalBackground)))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Fale espontaneamente sem se preocupar com pausas ou vícios de linguagem. O Gemini estrutura tudo com perfeição executiva.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }

        // --- BOTTOM ACTION BAR (FILE IMPORT & TEXT INPUT) ---
        Surface(
            color = CharcoalSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                if (!showTextFallback) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Import Audio File Button
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CharcoalCard)
                                .border(1.dp, CharcoalBorder, RoundedCornerShape(12.dp))
                                .clickable { audioPickerLauncher.launch("audio/*") }
                                .padding(vertical = 10.dp, horizontal = 12.dp)
                                .testTag("import_audio_button"),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = "Import Audio",
                                tint = ElectricBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Import Audio",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Type Raw Thoughts Button
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CharcoalCard)
                                .border(1.dp, CharcoalBorder, RoundedCornerShape(12.dp))
                                .clickable { showTextFallback = true }
                                .padding(vertical = 10.dp, horizontal = 12.dp)
                                .testTag("type_thoughts_button"),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Keyboard,
                                contentDescription = "Type text",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Type Thoughts",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = fallbackText,
                            onValueChange = { fallbackText = it },
                            placeholder = { Text("Type any rough thoughts, notes, ideas...", color = TextMuted, fontSize = 13.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricBlue,
                                unfocusedBorderColor = CharcoalBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = CharcoalBackground,
                                unfocusedContainerColor = CharcoalBackground
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("fallback_text_input")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (fallbackText.isNotBlank()) {
                                    viewModel.processTypedText(fallbackText)
                                    fallbackText = ""
                                    showTextFallback = false
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (fallbackText.isNotBlank()) ElectricBlue else CharcoalSurfaceVariant)
                                .testTag("submit_text_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Process Text",
                                tint = if (fallbackText.isNotBlank()) TextPrimary else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { showTextFallback = false },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close keyboard input",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
