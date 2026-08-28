package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.CustomPromptDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.screens.AgentScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ProcessingScreen
import com.example.ui.screens.RecordScreen
import com.example.ui.screens.ResultScreen
import com.example.ui.theme.AudioPenTheme
import com.example.ui.theme.CharcoalBackground
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.AudioPenViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AudioPenTheme {
                val viewModel: AudioPenViewModel = viewModel()
                AudioPenApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun AudioPenApp(viewModel: AudioPenViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val showSettingsDialog by viewModel.showSettingsDialog.collectAsState()
    val showCustomPromptDialog by viewModel.showCustomPromptDialog.collectAsState()
    val customApiKey by viewModel.customApiKey.collectAsState()
    val selectedFormat by viewModel.selectedFormat.collectAsState()

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(CharcoalBackground),
        color = CharcoalBackground
    ) {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                fadeIn(animationSpec = tween(280)) togetherWith fadeOut(animationSpec = tween(280))
            },
            label = "screen_transition"
        ) { screen ->
            when (screen) {
                AppScreen.RECORD -> {
                    RecordScreen(viewModel = viewModel)
                }
                AppScreen.PROCESSING -> {
                    ProcessingScreen(viewModel = viewModel)
                }
                AppScreen.RESULT -> {
                    ResultScreen(viewModel = viewModel)
                }
                AppScreen.HISTORY -> {
                    HistoryScreen(viewModel = viewModel)
                }
                AppScreen.AGENT -> {
                    AgentScreen(viewModel = viewModel)
                }
            }
        }

        // Settings Dialog
        if (showSettingsDialog) {
            SettingsDialog(
                currentApiKey = customApiKey,
                onSaveApiKey = { viewModel.saveCustomApiKey(it) },
                onDismiss = { viewModel.setSettingsDialogVisible(false) }
            )
        }

        // Custom Prompt Dialog
        if (showCustomPromptDialog) {
            CustomPromptDialog(
                onApplyCustomPrompt = { prompt ->
                    viewModel.reformatActiveNote(
                        targetStyle = selectedFormat,
                        customPrompt = prompt
                    )
                },
                onDismiss = { viewModel.setCustomPromptDialogVisible(false) }
            )
        }
    }
}
