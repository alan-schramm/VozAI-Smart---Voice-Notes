package com.example.data.repository

import com.example.data.local.ChatDao
import com.example.data.local.ChatMessageEntity
import com.example.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatRepository(private val chatDao: ChatDao) {

    val allMessages: Flow<List<ChatMessage>> = chatDao.getAllMessages()
        .map { entities -> entities.map { it.toDomainModel() } }

    suspend fun insertMessage(message: ChatMessage) {
        chatDao.insertMessage(ChatMessageEntity.fromDomainModel(message))
    }

    suspend fun insertMessages(messages: List<ChatMessage>) {
        chatDao.insertMessages(messages.map { ChatMessageEntity.fromDomainModel(it) })
    }

    suspend fun deleteMessageById(id: String) {
        chatDao.deleteMessageById(id)
    }

    suspend fun clearAllMessages() {
        chatDao.clearAllMessages()
    }
}
