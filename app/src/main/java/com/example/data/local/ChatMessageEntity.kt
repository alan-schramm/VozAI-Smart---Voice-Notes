package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.ChatMessage
import com.example.data.model.GroundingSource
import com.example.data.model.MessageSender
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    val sender: String, // "USER" or "AGENT"
    val text: String,
    val timestamp: Long,
    val isAudioInput: Boolean = false,
    val audioDurationSeconds: Int = 0,
    val suggestedFollowUps: String = "", // Delimited with newline
    val language: String = "Auto",
    val searchQueriesJson: String = "",
    val groundingSourcesJson: String = ""
) {
    fun toDomainModel(): ChatMessage {
        val followUps = if (suggestedFollowUps.isBlank()) {
            emptyList()
        } else {
            suggestedFollowUps.split("\n").filter { it.isNotBlank() }
        }

        val queries = parseQueriesJson(searchQueriesJson)
        val sources = parseSourcesJson(groundingSourcesJson)

        return ChatMessage(
            id = id,
            sender = if (sender.equals("AGENT", ignoreCase = true)) MessageSender.AGENT else MessageSender.USER,
            text = text,
            timestamp = timestamp,
            isAudioInput = isAudioInput,
            audioDurationSeconds = audioDurationSeconds,
            suggestedFollowUps = followUps,
            language = language,
            searchQueries = queries,
            groundingSources = sources
        )
    }

    companion object {
        fun fromDomainModel(message: ChatMessage): ChatMessageEntity {
            return ChatMessageEntity(
                id = message.id,
                sender = message.sender.name,
                text = message.text,
                timestamp = message.timestamp,
                isAudioInput = message.isAudioInput,
                audioDurationSeconds = message.audioDurationSeconds,
                suggestedFollowUps = message.suggestedFollowUps.joinToString("\n"),
                language = message.language,
                searchQueriesJson = serializeQueriesJson(message.searchQueries),
                groundingSourcesJson = serializeSourcesJson(message.groundingSources)
            )
        }

        private fun parseQueriesJson(jsonStr: String): List<String> {
            if (jsonStr.isBlank()) return emptyList()
            return try {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    val q = array.optString(i)
                    if (q.isNotBlank()) list.add(q)
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }

        private fun serializeQueriesJson(queries: List<String>): String {
            if (queries.isEmpty()) return ""
            val array = JSONArray()
            queries.forEach { array.put(it) }
            return array.toString()
        }

        private fun parseSourcesJson(jsonStr: String): List<GroundingSource> {
            if (jsonStr.isBlank()) return emptyList()
            return try {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<GroundingSource>()
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val title = obj.optString("title", "")
                    val uri = obj.optString("uri", "")
                    val domain = obj.optString("domain", "")
                    if (uri.isNotBlank()) {
                        list.add(GroundingSource(title = title.ifBlank { domain.ifBlank { uri } }, uri = uri, domain = domain.ifBlank { GroundingSource.extractDomain(uri) }))
                    }
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }

        private fun serializeSourcesJson(sources: List<GroundingSource>): String {
            if (sources.isEmpty()) return ""
            val array = JSONArray()
            sources.forEach { s ->
                val obj = JSONObject().apply {
                    put("title", s.title)
                    put("uri", s.uri)
                    put("domain", s.domain)
                }
                array.put(obj)
            }
            return array.toString()
        }
    }
}
