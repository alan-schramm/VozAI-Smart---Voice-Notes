package com.example.data.model

import java.net.URI

data class GroundingSource(
    val title: String,
    val uri: String,
    val domain: String = extractDomain(uri)
) {
    companion object {
        fun extractDomain(url: String): String {
            return try {
                val uriObj = URI(url)
                val host = uriObj.host ?: url
                host.removePrefix("www.")
            } catch (e: Exception) {
                if (url.length > 28) url.take(28) + "..." else url
            }
        }
    }
}

data class GroundingMetadata(
    val searchQueries: List<String> = emptyList(),
    val sources: List<GroundingSource> = emptyList()
)

data class NoteEnrichmentResult(
    val noteId: Long,
    val enrichedAnalysis: String,
    val verifiedFacts: List<String> = emptyList(),
    val searchQueries: List<String> = emptyList(),
    val sources: List<GroundingSource> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)
