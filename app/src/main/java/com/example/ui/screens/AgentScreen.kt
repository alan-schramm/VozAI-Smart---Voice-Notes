package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.ChatMessage
import com.example.data.model.MessageSender
import com.example.ui.components.AudioWaveVisualizer
import com.example.ui.components.GroundingSourcesView
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
import com.example.ui.theme.GraphiteElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.AudioPenViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AgentScreen(
    viewModel: AudioPenViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isAgentThinking by viewModel.isAgentThinking.collectAsState()
    val isVoiceEnabled by viewModel.isVoiceResponseEnabled.collectAsState()
    val isChatRecording by viewModel.isChatRecording.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val currentlySpeakingId by viewModel.currentlySpeakingId.collectAsState()

    val isGoogleSearchEnabled by viewModel.isGoogleSearchEnabled.collectAsState()
    val isNotesContextEnabled by viewModel.isNotesContextEnabled.collectAsState()

    val recordingAmplitude by viewModel.recorderManager.currentAmplitude.collectAsState()
    val amplitudeHistory by viewModel.recorderManager.amplitudeHistory.collectAsState()
    val durationSeconds by viewModel.recorderManager.durationSeconds.collectAsState()

    var inputMessageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Permission launcher for voice recording
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startChatVoiceRecording()
        } else {
            Toast.makeText(context, "Permissão de microfone necessária para falar com o agente", Toast.LENGTH_SHORT).show()
        }
    }

    val handleVoiceRecordToggle = {
        if (!isChatRecording) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                viewModel.startChatVoiceRecording()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        } else {
            viewModel.finishChatVoiceRecordingAndSend()
        }
    }

    // Auto-scroll when new messages arrive or agent starts thinking
    LaunchedEffect(chatMessages.size, isAgentThinking) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CharcoalBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // --- HEADER BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        viewModel.stopSpeaking()
                        viewModel.navigateTo(AppScreen.RECORD)
                    },
                    modifier = Modifier.testTag("agent_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
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
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Idea Partner AI",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isSpeaking) ElectricBlueGlow else EmeraldAccent)
                        )
                    }
                    Text(
                        text = if (isSpeaking) "Falando em voz alta..." else if (isVoiceEnabled) "Voz ativa • Conversação bilateral" else "Voz desativada (Modo Texto)",
                        color = if (isSpeaking) ElectricBlue else TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            // Top action buttons (Voice toggle & Reset session)
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Voice Response toggle
                IconButton(
                    onClick = { viewModel.toggleVoiceResponse() },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isVoiceEnabled) CharcoalCard else CharcoalSurfaceVariant)
                        .border(1.dp, if (isVoiceEnabled) ElectricBlue.copy(alpha = 0.5f) else CharcoalBorder, CircleShape)
                        .testTag("toggle_voice_button")
                ) {
                    Icon(
                        imageVector = if (isVoiceEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                        contentDescription = if (isVoiceEnabled) "Desativar áudio" else "Ativar resposta por voz",
                        tint = if (isVoiceEnabled) ElectricBlue else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Reset session
                IconButton(
                    onClick = { viewModel.clearChatSession() },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CharcoalCard)
                        .border(1.dp, CharcoalBorder, CircleShape)
                        .testTag("reset_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reiniciar conversa",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // --- CONVERSATION FEED ---
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
            }

            items(chatMessages, key = { it.id }) { message ->
                ChatMessageItem(
                    message = message,
                    isSpeakingThis = isSpeaking && currentlySpeakingId == message.id,
                    onSpeakClick = { viewModel.speakChatMessage(message) },
                    onFollowUpClick = { followUpText ->
                        viewModel.sendChatMessage(followUpText)
                    },
                    onSaveAsNote = { ideaText ->
                        viewModel.createNoteFromAgentIdea(ideaText)
                    },
                    onCopyText = { textToCopy ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Idea Partner", textToCopy)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copiado para a área de transferência!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Agent Thinking Indicator
            if (isAgentThinking) {
                item {
                    AgentThinkingBubble()
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // --- QUICK STARTER PROMPTS (if chat is short) ---
        if (chatMessages.size <= 2 && !isChatRecording && !isAgentThinking) {
            val starterPrompts = listOf(
                "💡 Validar modelo de negócio",
                "🎯 Estruturar proposta de valor",
                "⚡ Encontrar pontos cegos no meu plano",
                "📊 Criar roteiro de pitch de 1 min"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    starterPrompts.forEach { prompt ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(CharcoalCard)
                                .border(1.dp, CharcoalBorder, RoundedCornerShape(20.dp))
                                .clickable { viewModel.sendChatMessage(prompt) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = prompt,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // --- BOTTOM INPUT AREA (VOICE & TEXT) ---
        Surface(
            color = CharcoalSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                if (isChatRecording) {
                    // LIVE VOICE RECORDING ACTIVE BAR
                    val minutes = durationSeconds / 60
                    val seconds = durationSeconds % 60
                    val timerStr = String.format("%02d:%02d", minutes, seconds)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CharcoalCard)
                            .border(1.dp, ElectricBlue.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(CrimsonAccent)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Ouvindo sua ideia...",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = timerStr,
                                color = ElectricBlueGlow,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Waveform Visualizer
                        AudioWaveVisualizer(
                            isRecording = true,
                            amplitude = recordingAmplitude,
                            amplitudeHistory = amplitudeHistory,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Cancel recording
                            IconButton(
                                onClick = { viewModel.cancelChatVoiceRecording() },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(CharcoalSurfaceVariant)
                                    .testTag("cancel_voice_chat_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Cancelar áudio",
                                    tint = CrimsonAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Finish and send voice
                            IconButton(
                                onClick = { viewModel.finishChatVoiceRecordingAndSend() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(ElectricBlue)
                                    .testTag("send_voice_chat_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Enviar áudio ao agente",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                } else {
                    // AI CAPABILITY SWITCHES: Google Search Grounding & Notes Context
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Google Search Grounding Toggle Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isGoogleSearchEnabled) EmeraldAccent.copy(alpha = 0.18f)
                                    else CharcoalSurfaceVariant.copy(alpha = 0.6f)
                                )
                                .border(
                                    1.dp,
                                    if (isGoogleSearchEnabled) EmeraldAccent.copy(alpha = 0.6f)
                                    else CharcoalBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.toggleGoogleSearch() }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .testTag("toggle_grounding_search_pill")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = if (isGoogleSearchEnabled) EmeraldAccent else TextMuted,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Google Search",
                                    color = if (isGoogleSearchEnabled) EmeraldAccent else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isGoogleSearchEnabled) FontWeight.Bold else FontWeight.Medium
                                )
                                if (isGoogleSearchEnabled) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldAccent)
                                    )
                                }
                            }
                        }

                        // Notes Context Toggle Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isNotesContextEnabled) ElectricBlue.copy(alpha = 0.18f)
                                    else CharcoalSurfaceVariant.copy(alpha = 0.6f)
                                )
                                .border(
                                    1.dp,
                                    if (isNotesContextEnabled) ElectricBlue.copy(alpha = 0.6f)
                                    else CharcoalBorder,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.toggleNotesContext() }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .testTag("toggle_notes_context_pill")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = if (isNotesContextEnabled) ElectricBlue else TextMuted,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Usar Notas",
                                    color = if (isNotesContextEnabled) ElectricBlue else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isNotesContextEnabled) FontWeight.Bold else FontWeight.Medium
                                )
                                if (isNotesContextEnabled) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(ElectricBlue)
                                    )
                                }
                            }
                        }
                    }

                    // NORMAL TEXT & VOICE INPUT BAR
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputMessageText,
                            onValueChange = { inputMessageText = it },
                            placeholder = {
                                Text(
                                    text = "Fale ou digite sua ideia...",
                                    color = TextMuted,
                                    fontSize = 14.sp
                                )
                            },
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricBlue,
                                unfocusedBorderColor = CharcoalBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = CharcoalBackground,
                                unfocusedContainerColor = CharcoalBackground
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_text_input")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Mic Button (Tap to record voice message)
                        IconButton(
                            onClick = handleVoiceRecordToggle,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(if (inputMessageText.isBlank()) ElectricBlue else CharcoalSurfaceVariant)
                                .testTag("chat_voice_mic_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Falar por voz",
                                tint = if (inputMessageText.isBlank()) TextPrimary else TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Send Button (if text is present)
                        AnimatedVisibility(visible = inputMessageText.isNotBlank()) {
                            Row {
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = {
                                        if (inputMessageText.isNotBlank()) {
                                            viewModel.sendChatMessage(inputMessageText)
                                            inputMessageText = ""
                                        }
                                    },
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(ElectricBlue)
                                        .testTag("chat_send_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Enviar mensagem",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatMessageItem(
    message: ChatMessage,
    isSpeakingThis: Boolean,
    onSpeakClick: () -> Unit,
    onFollowUpClick: (String) -> Unit,
    onSaveAsNote: (String) -> Unit,
    onCopyText: (String) -> Unit
) {
    val isUser = message.sender == MessageSender.USER

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (isUser) {
            // USER MESSAGE BUBBLE
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 4.dp,
                            bottomStart = 18.dp,
                            bottomEnd = 18.dp
                        )
                    )
                    .background(GraphiteElevated)
                    .border(
                        1.dp,
                        if (message.isAudioInput) ElectricBlue.copy(alpha = 0.5f) else CharcoalBorder,
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 4.dp,
                            bottomStart = 18.dp,
                            bottomEnd = 18.dp
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column {
                    if (message.isAudioInput) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = ElectricBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Entrada de Voz",
                                color = ElectricBlue,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = message.text,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        } else {
            // AGENT RESPONSE BUBBLE
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CharcoalCard),
                shape = RoundedCornerShape(
                    topStart = 4.dp,
                    topEnd = 18.dp,
                    bottomStart = 18.dp,
                    bottomEnd = 18.dp
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(
                        colors = listOf(ElectricBlue.copy(alpha = 0.3f), CharcoalBorder)
                    )
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Agent Header & Action Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(ElectricBlue.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Idea Partner AI",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Speech & Action Buttons
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Speak / Stop Voice button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSpeakingThis) ElectricBlue.copy(alpha = 0.25f) else CharcoalSurfaceVariant)
                                    .clickable { onSpeakClick() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isSpeakingThis) {
                                        SpeakingBarsVisualizer()
                                        Spacer(modifier = Modifier.width(4.dp))
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Ouvir",
                                            tint = ElectricBlue,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = if (isSpeakingThis) "Pausar" else "Ouvir",
                                        color = if (isSpeakingThis) ElectricBlueGlow else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Copy button
                            IconButton(
                                onClick = { onCopyText(message.text) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copiar resposta",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Message text
                    Text(
                        text = message.text,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )

                    // Grounding Sources & Search Queries View
                    if (message.groundingSources.isNotEmpty() || message.searchQueries.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        GroundingSourcesView(
                            searchQueries = message.searchQueries,
                            sources = message.groundingSources,
                            isInitiallyExpanded = false
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // "Save Idea as AudioPen Note" button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CharcoalSurface)
                            .border(1.dp, CharcoalBorder, RoundedCornerShape(10.dp))
                            .clickable { onSaveAsNote(message.text) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NoteAdd,
                                contentDescription = null,
                                tint = ElectricBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Salvar esta Ideia como Nota AudioPen",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Suggested Follow-up chips
                    if (message.suggestedFollowUps.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Aprofundar ideia:",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            message.suggestedFollowUps.forEach { followUp ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(CharcoalSurfaceVariant)
                                        .border(1.dp, CharcoalBorder, RoundedCornerShape(16.dp))
                                        .clickable { onFollowUpClick(followUp) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = followUp,
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AgentThinkingBubble() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha_pulse"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CharcoalCard),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                colors = listOf(ElectricBlue.copy(alpha = alpha), CharcoalBorder)
            )
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = ElectricBlue,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "AudioPen AI pensando & estruturando raciocínio...",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun SpeakingBarsVisualizer() {
    val infiniteTransition = rememberInfiniteTransition(label = "bars")
    val height1 by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h1"
    )
    val height2 by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h2"
    )
    val height3 by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h3"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.height(16.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(height1.dp)
                .clip(CircleShape)
                .background(ElectricBlue)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(height2.dp)
                .clip(CircleShape)
                .background(ElectricBlueGlow)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(height3.dp)
                .clip(CircleShape)
                .background(ElectricBlue)
        )
    }
}
