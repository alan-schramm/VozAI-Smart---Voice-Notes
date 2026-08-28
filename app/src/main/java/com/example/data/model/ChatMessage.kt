package com.example.data.model

import java.util.UUID

enum class MessageSender {
    USER,
    AGENT
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isAudioInput: Boolean = false,
    val audioDurationSeconds: Int = 0,
    val suggestedFollowUps: List<String> = emptyList(),
    val language: String = "Auto",
    val searchQueries: List<String> = emptyList(),
    val groundingSources: List<GroundingSource> = emptyList()
)
