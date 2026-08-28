package com.example.data.remote

import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.FormatStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.TimeUnit

import com.example.data.model.ChatMessage
import com.example.data.model.GroundingMetadata
import com.example.data.model.GroundingSource
import com.example.data.model.MessageSender
import com.example.data.model.NoteEnrichmentResult

data class GeminiProcessResult(
    val title: String,
    val polishedText: String,
    val rawTranscript: String,
    val summary: String,
    val keyTakeaways: List<String>,
    val tags: List<String> = emptyList(),
    val language: String = "Auto"
)

data class GeminiSummaryResult(
    val summary: String,
    val keyTakeaways: List<String> = emptyList(),
    val tags: List<String> = emptyList()
)

data class AgentChatResponse(
    val userSpokenTranscription: String? = null,
    val replyText: String,
    val suggestedFollowUps: List<String> = emptyList(),
    val language: String = "Auto",
    val searchQueries: List<String> = emptyList(),
    val groundingSources: List<GroundingSource> = emptyList()
)

class GeminiService(private val customApiKeyProvider: () -> String? = { null }) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val modelName = "gemini-3.5-flash"
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"

    private fun getApiKey(): String {
        val customKey = customApiKeyProvider()
        if (!customKey.isNullOrBlank()) {
            return customKey.trim()
        }
        val buildKey = BuildConfig.GEMINI_API_KEY
        if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") {
            return buildKey.trim()
        }
        return ""
    }

    /**
     * Transcribes audio and creates polished, structured text in the selected format.
     */
    suspend fun processAudio(
        audioFile: File,
        mimeType: String = "audio/mp4",
        formatStyle: FormatStyle = FormatStyle.CLASSIC,
        customInstruction: String? = null
    ): Result<GeminiProcessResult> {
        val audioBytes = try {
            audioFile.readBytes()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return processAudioBytes(audioBytes, mimeType, formatStyle, customInstruction)
    }

    /**
     * Transcribes audio from raw byte array and creates polished, structured text.
     */
    suspend fun processAudioBytes(
        audioBytes: ByteArray,
        mimeType: String = "audio/mp4",
        formatStyle: FormatStyle = FormatStyle.CLASSIC,
        customInstruction: String? = null
    ): Result<GeminiProcessResult> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            if (apiKey.isBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("Gemini API Key is missing. Please configure it in Settings or via Secrets.")
                )
            }

            val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)

            val prompt = buildAudioProcessingPrompt(formatStyle, customInstruction)

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray()
                val contentObj = JSONObject()
                val partsArray = JSONArray()

                // Text instruction part
                partsArray.put(JSONObject().apply {
                    put("text", prompt)
                })

                // Audio inlineData part
                partsArray.put(JSONObject().apply {
                    val inlineData = JSONObject().apply {
                        put("mimeType", mimeType)
                        put("data", base64Audio)
                    }
                    put("inlineData", inlineData)
                })

                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
                put("contents", contentsArray)

                // Generation config with JSON response format
                val generationConfig = JSONObject().apply {
                    put("temperature", 0.3)
                    val responseFormat = JSONObject().apply {
                        put("mimeType", "application/json")
                    }
                    put("responseFormat", responseFormat)
                }
                put("generationConfig", generationConfig)
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$baseUrl?key=$apiKey")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = extractErrorMessage(responseBody) ?: "HTTP ${response.code}: ${response.message}"
                return@withContext Result.failure(Exception("Gemini API Error: $errorMsg"))
            }

            val parsedResult = parseGeminiJsonResponse(responseBody)
            Result.success(parsedResult)
        } catch (e: Exception) {
            Log.e("GeminiService", "Error processing audio with Gemini", e)
            Result.failure(e)
        }
    }

    /**
     * Re-structures and refines raw text into a new format style or with custom guidance.
     */
    suspend fun reformatText(
        rawOrPolishedText: String,
        targetFormatStyle: FormatStyle,
        customInstruction: String? = null
    ): Result<GeminiProcessResult> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            if (apiKey.isBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("Gemini API Key is missing. Please configure it in Settings or via Secrets.")
                )
            }

            val prompt = """
                You are AudioPen AI, an expert executive editor and ghostwriter.
                Your task is to take the provided text and transform it into the requested format.
                
                TARGET FORMAT: ${targetFormatStyle.title}
                FORMAT GUIDELINES:
                ${targetFormatStyle.systemPromptGuide}
                ${if (!customInstruction.isNullOrBlank()) "ADDITIONAL USER INSTRUCTION: $customInstruction" else ""}
                
                ORIGINAL TEXT:
                $rawOrPolishedText
                
                Respond ONLY with a valid JSON object strictly matching this schema:
                {
                  "title": "A short, punchy 3-6 word title capturing the core essence",
                  "polishedText": "The rewritten, crystal-clear, structured text adhering strictly to the TARGET FORMAT guidelines",
                  "summary": "A 2-sentence executive summary of the main idea",
                  "keyTakeaways": ["Key bullet 1", "Key bullet 2", "Key bullet 3"],
                  "tags": ["Tag1", "Tag2"],
                  "rawTranscript": "Preserve the original input text provided",
                  "language": "Detected language (e.g. English, Portuguese, Spanish, etc.)"
                }
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray()
                val contentObj = JSONObject()
                val partsArray = JSONArray()

                partsArray.put(JSONObject().apply {
                    put("text", prompt)
                })

                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
                put("contents", contentsArray)

                val generationConfig = JSONObject().apply {
                    put("temperature", 0.3)
                    val responseFormat = JSONObject().apply {
                        put("mimeType", "application/json")
                    }
                    put("responseFormat", responseFormat)
                }
                put("generationConfig", generationConfig)
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$baseUrl?key=$apiKey")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = extractErrorMessage(responseBody) ?: "HTTP ${response.code}: ${response.message}"
                return@withContext Result.failure(Exception("Gemini API Error: $errorMsg"))
            }

            val parsedResult = parseGeminiJsonResponse(responseBody)
            Result.success(parsedResult)
        } catch (e: Exception) {
            Log.e("GeminiService", "Error reformatting text with Gemini", e)
            Result.failure(e)
        }
    }

    /**
     * Sends the selected note content to the Gemini API and generates a concise, high-impact summary
     * with key takeaways and relevant tags.
     */
    suspend fun generateConciseSummary(
        noteTitle: String,
        noteContent: String
    ): Result<GeminiSummaryResult> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            if (apiKey.isBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("Gemini API Key is missing. Please configure it in Settings or via Secrets.")
                )
            }

            val prompt = """
                You are AudioPen AI, an expert executive editor, strategic summarizer, and analyst.
                Your task is to analyze the provided note content and produce:
                1. A concise, crystal-clear executive summary (2-3 sentences maximum) capturing the core essence, decisions, and crucial takeaways.
                2. 2-4 actionable key takeaways or bullet points.
                3. 2-4 concise topic tags (without # symbol).

                NOTE TITLE: $noteTitle
                NOTE CONTENT:
                $noteContent

                Respond naturally in the same language as the note content (e.g., English, Portuguese, Spanish).
                Respond ONLY in valid JSON matching this schema:
                {
                  "summary": "A concise, crystal-clear executive summary (2-3 sentences max).",
                  "keyTakeaways": [
                    "Key point or action item 1",
                    "Key point or action item 2"
                  ],
                  "tags": [
                    "tag1",
                    "tag2"
                  ]
                }
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray()
                val contentObj = JSONObject()
                val partsArray = JSONArray()

                partsArray.put(JSONObject().apply {
                    put("text", prompt)
                })

                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
                put("contents", contentsArray)

                val generationConfig = JSONObject().apply {
                    put("temperature", 0.2)
                    val responseFormat = JSONObject().apply {
                        put("mimeType", "application/json")
                    }
                    put("responseFormat", responseFormat)
                }
                put("generationConfig", generationConfig)
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$baseUrl?key=$apiKey")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = extractErrorMessage(responseBody) ?: "HTTP ${response.code}: ${response.message}"
                return@withContext Result.failure(Exception("Gemini API Error: $errorMsg"))
            }

            val root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                throw IllegalStateException("No summary generated by Gemini")
            }

            val content = candidates.getJSONObject(0).optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            if (parts == null || parts.length() == 0) {
                throw IllegalStateException("Empty summary response parts from Gemini")
            }

            var textContent = parts.getJSONObject(0).optString("text", "").trim()
            if (textContent.startsWith("```json")) {
                textContent = textContent.removePrefix("```json").removeSuffix("```").trim()
            } else if (textContent.startsWith("```")) {
                textContent = textContent.removePrefix("```").removeSuffix("```").trim()
            }

            val parsed = JSONObject(textContent)
            val summary = parsed.optString("summary", "").trim()
            val takeawaysList = mutableListOf<String>()
            val takeawaysJsonArray = parsed.optJSONArray("keyTakeaways")
            if (takeawaysJsonArray != null) {
                for (i in 0 until takeawaysJsonArray.length()) {
                    val item = takeawaysJsonArray.optString(i)
                    if (item.isNotBlank()) {
                        takeawaysList.add(item.trim())
                    }
                }
            }

            val tagsList = mutableListOf<String>()
            val tagsJsonArray = parsed.optJSONArray("tags")
            if (tagsJsonArray != null) {
                for (i in 0 until tagsJsonArray.length()) {
                    val item = tagsJsonArray.optString(i)
                    if (item.isNotBlank()) {
                        val cleanTag = item.trim().removePrefix("#").trim()
                        if (cleanTag.isNotBlank() && !tagsList.contains(cleanTag)) {
                            tagsList.add(cleanTag)
                        }
                    }
                }
            }

            Result.success(GeminiSummaryResult(summary, takeawaysList, tagsList))
        } catch (e: Exception) {
            Log.e("GeminiService", "Error generating concise summary with Gemini", e)
            Result.failure(e)
        }
    }

    private fun buildAudioProcessingPrompt(formatStyle: FormatStyle, customInstruction: String?): String {
        return """
            You are AudioPen AI, a world-class audio transcriber, executive speech cleaner, and editorial ghostwriter.
            You will listen to the attached voice recording and perform two essential jobs:
            1. Transcribe EXACTLY what the user said (verbatim raw transcript, including spoken words).
            2. Transform their messy, spontaneous, unorganized speech into a clear, compelling, and professional written piece in the requested style.
            
            STYLE: ${formatStyle.title}
            STYLE INSTRUCTIONS:
            ${formatStyle.systemPromptGuide}
            ${if (!customInstruction.isNullOrBlank()) "SPECIAL INSTRUCTION: $customInstruction" else ""}
            
            CORE RULES FOR POLISHING:
            - Eradicate verbal tics, filler words ('um', 'uh', 'like', 'tipo', 'né', 'you know', 'sort of', 'aham'), hesitations, repeated phrases, and false starts.
            - Keep the author's authentic perspective and voice, but elevate the prose to be lucid, persuasive, and beautifully written.
            - Ensure paragraphs have clean logical transitions and generous readability.
            - Transcribe in whatever language the user spoke (Portuguese, English, Spanish, etc.), and craft the polished output in that SAME language naturally.
            
            Respond ONLY with a valid JSON object adhering strictly to this JSON format:
            {
              "title": "A short, elegant 3-6 word title for this note",
              "polishedText": "The final beautifully written, structured text with clean paragraph breaks",
              "summary": "A 2-sentence executive summary of the note",
              "keyTakeaways": [
                "Key takeaway or action point 1",
                "Key takeaway or action point 2",
                "Key takeaway or action point 3"
              ],
              "tags": [
                "CategoryOrTopic1",
                "CategoryOrTopic2"
              ],
              "rawTranscript": "The verbatim, word-for-word transcript of the audio",
              "language": "Detected language (e.g. Portuguese, English, Spanish)"
            }
        """.trimIndent()
    }

    private fun parseGeminiJsonResponse(jsonString: String): GeminiProcessResult {
        val root = JSONObject(jsonString)
        val candidates = root.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) {
            throw IllegalStateException("No content generated by Gemini")
        }

        val content = candidates.getJSONObject(0).optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        if (parts == null || parts.length() == 0) {
            throw IllegalStateException("Empty response parts from Gemini")
        }

        var textContent = parts.getJSONObject(0).optString("text", "").trim()
        if (textContent.startsWith("```json")) {
            textContent = textContent.removePrefix("```json").removeSuffix("```").trim()
        } else if (textContent.startsWith("```")) {
            textContent = textContent.removePrefix("```").removeSuffix("```").trim()
        }

        val parsed = JSONObject(textContent)
        val title = parsed.optString("title", "Voice Note").trim()
        val polishedText = parsed.optString("polishedText", "").trim()
        val rawTranscript = parsed.optString("rawTranscript", "").trim()
        val summary = parsed.optString("summary", "").trim()
        val language = parsed.optString("language", "Auto").trim()

        val takeawaysList = mutableListOf<String>()
        val takeawaysJsonArray = parsed.optJSONArray("keyTakeaways")
        if (takeawaysJsonArray != null) {
            for (i in 0 until takeawaysJsonArray.length()) {
                val item = takeawaysJsonArray.optString(i)
                if (item.isNotBlank()) {
                    takeawaysList.add(item.trim())
                }
            }
        }

        val tagsList = mutableListOf<String>()
        val tagsJsonArray = parsed.optJSONArray("tags")
        if (tagsJsonArray != null) {
            for (i in 0 until tagsJsonArray.length()) {
                val item = tagsJsonArray.optString(i)
                if (item.isNotBlank()) {
                    val cleanTag = item.trim().removePrefix("#").trim()
                    if (cleanTag.isNotBlank() && !tagsList.contains(cleanTag)) {
                        tagsList.add(cleanTag)
                    }
                }
            }
        }

        return GeminiProcessResult(
            title = title.ifBlank { "Spoken Memo" },
            polishedText = polishedText.ifBlank { rawTranscript },
            rawTranscript = rawTranscript.ifBlank { polishedText },
            summary = summary,
            keyTakeaways = takeawaysList,
            tags = tagsList,
            language = language
        )
    }

    /**
     * Interactive conversational AI brainstorming & research agent.
     * Supports text, voice input, multi-turn history, Notes Knowledge Context, and Google Search Grounding.
     */
    suspend fun chatWithAgent(
        conversationHistory: List<ChatMessage>,
        newUserMessage: String?,
        userAudioFile: File? = null,
        notesContext: String? = null,
        enableGoogleSearch: Boolean = true
    ): Result<AgentChatResponse> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            if (apiKey.isBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("Gemini API Key is missing. Please configure it in Settings or via Secrets.")
                )
            }

            val requestJson = JSONObject().apply {
                // System instruction
                val systemInstruction = JSONObject().apply {
                    val parts = JSONArray().apply {
                        put(JSONObject().apply {
                            val notesContextPrompt = if (!notesContext.isNullOrBlank()) {
                                """
                                
                                USER'S LOCAL NOTES CONTEXT:
                                The user has provided context from their stored voice notes:
                                $notesContext
                                You can cross-reference, summarize, find connections, or answer questions directly referencing these notes when helpful.
                                """
                            } else ""

                            val searchGuidance = if (enableGoogleSearch) {
                                """
                                - GOOGLE SEARCH GROUNDING IS ACTIVE. You have access to real-time Google Search data. For current events, recent tech/news, market stats, fact-checking, prices, and references, use Google Search grounding to provide verified, up-to-date facts with real-world sources and links.
                                """
                            } else ""

                            put(
                                "text",
                                """
                                You are AudioPen AI Partner, an elite executive brainstorming partner, venture strategist, knowledge synthesizer, and researcher.
                                You help the user explore, expand, critique, fact-check, and structure their thoughts and raw ideas into actionable, high-impact plans.
                                $notesContextPrompt
                                
                                CORE RESPONSIBILITIES:
                                1. If user provided a voice recording, first transcribe their spoken words verbatim in 'userTranscript'.
                                2. Provide a sharp, inspiring, structured reply in 'replyText'. Use clean formatting (bullet points, bold highlights, strategic advice, or actionable next steps).
                                $searchGuidance
                                3. Formulate 2-3 short, compelling suggested follow-up prompts in 'suggestedFollowUps' to help the user dive deeper.
                                4. Match the user's language naturally (Portuguese, English, Spanish, etc.).
                                
                                Respond ONLY in valid JSON matching this schema:
                                {
                                  "userTranscript": "Verbatim transcript of user audio if audio was provided, or empty string",
                                  "replyText": "Your executive response with bullet points and bold key terms",
                                  "suggestedFollowUps": ["Suggested next question 1", "Suggested next question 2", "Suggested next question 3"],
                                  "language": "Detected language (e.g. Portuguese, English)"
                                }
                                """.trimIndent()
                            )
                        })
                    }
                    put("parts", parts)
                }
                put("systemInstruction", systemInstruction)

                // Google Search Grounding Tool
                if (enableGoogleSearch) {
                    val toolsArray = JSONArray().apply {
                        val searchTool = JSONObject().apply {
                            put("googleSearch", JSONObject())
                        }
                        put(searchTool)
                    }
                    put("tools", toolsArray)
                }

                val contentsArray = JSONArray()

                // Append past conversation history (up to last 10 turns)
                val recentHistory = conversationHistory.takeLast(10)
                for (msg in recentHistory) {
                    val turnObj = JSONObject().apply {
                        put("role", if (msg.sender == MessageSender.USER) "user" else "model")
                        val parts = JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", msg.text)
                            })
                        }
                        put("parts", parts)
                    }
                    contentsArray.put(turnObj)
                }

                // New turn
                val currentTurnObj = JSONObject().apply {
                    put("role", "user")
                    val parts = JSONArray()

                    if (userAudioFile != null && userAudioFile.exists() && userAudioFile.length() > 0) {
                        val audioBytes = userAudioFile.readBytes()
                        val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
                        parts.put(JSONObject().apply {
                            put("text", "I'm speaking to you by voice about my idea. Please listen, transcribe what I said, and brainstorm with me.")
                        })
                        parts.put(JSONObject().apply {
                            val inlineData = JSONObject().apply {
                                put("mimeType", "audio/mp4")
                                put("data", base64Audio)
                            }
                            put("inlineData", inlineData)
                        })
                    } else {
                        parts.put(JSONObject().apply {
                            put("text", newUserMessage ?: "Olá! Vamos conversar sobre uma ideia.")
                        })
                    }
                    put("parts", parts)
                }
                contentsArray.put(currentTurnObj)
                put("contents", contentsArray)

                val generationConfig = JSONObject().apply {
                    put("temperature", 0.7)
                }
                put("generationConfig", generationConfig)
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$baseUrl?key=$apiKey")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = extractErrorMessage(responseBody) ?: "HTTP ${response.code}: ${response.message}"
                return@withContext Result.failure(Exception("Gemini Agent Error: $errorMsg"))
            }

            val parsedResult = parseAgentJsonResponse(responseBody)
            Result.success(parsedResult)
        } catch (e: Exception) {
            Log.e("GeminiService", "Error in Gemini Agent chat", e)
            Result.failure(e)
        }
    }

    /**
     * Enriches an existing audio note with real-time Google Search web grounding.
     * Discovers current facts, relevant market context, related news, and verified citations.
     */
    suspend fun enrichNoteWithWebSearch(
        noteId: Long,
        noteTitle: String,
        noteContent: String,
        tags: String = ""
    ): Result<NoteEnrichmentResult> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            if (apiKey.isBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("Gemini API Key is missing. Please configure it in Settings or via Secrets.")
                )
            }

            val prompt = """
                You are AudioPen AI Research Assistant. Your mission is to enrich and fact-check the following user note using real-time Google Search data.
                
                NOTE TITLE: $noteTitle
                NOTE CONTENT:
                $noteContent
                ${if (tags.isNotBlank()) "TAGS: $tags" else ""}
                
                TASK:
                1. Perform web searches to find the latest updates, real-world context, statistics, authoritative references, or market insights directly relevant to what the user discussed.
                2. Provide a structured research analysis in 'enrichedAnalysis' in the SAME language as the note. Include:
                   - 📊 Contexto Atual & Fatos Relevantes (Key factual updates from the web)
                   - 💡 Oportunidades & Análise Crítica (Strategic expansion of the idea)
                   - 🔗 Pontos de Aprofundamento (Key directions to explore)
                3. Provide 2-4 concise bullet points in 'verifiedFacts' summarizing key findings.
                
                Respond ONLY in valid JSON matching this schema:
                {
                  "enrichedAnalysis": "Your structured analysis with markdown headings and bullet points",
                  "verifiedFacts": [
                    "Fact/insight 1 found on the web",
                    "Fact/insight 2 found on the web"
                  ]
                }
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray()
                val contentObj = JSONObject()
                val partsArray = JSONArray()

                partsArray.put(JSONObject().apply {
                    put("text", prompt)
                })

                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
                put("contents", contentsArray)

                // Google Search Grounding Tool
                val toolsArray = JSONArray().apply {
                    val searchTool = JSONObject().apply {
                        put("googleSearch", JSONObject())
                    }
                    put(searchTool)
                }
                put("tools", toolsArray)

                val generationConfig = JSONObject().apply {
                    put("temperature", 0.4)
                }
                put("generationConfig", generationConfig)
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$baseUrl?key=$apiKey")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = extractErrorMessage(responseBody) ?: "HTTP ${response.code}: ${response.message}"
                return@withContext Result.failure(Exception("Gemini Search Enrichment Error: $errorMsg"))
            }

            val parsedResult = parseEnrichmentResponse(noteId, responseBody)
            Result.success(parsedResult)
        } catch (e: Exception) {
            Log.e("GeminiService", "Error enriching note with web search", e)
            Result.failure(e)
        }
    }

    private fun parseAgentJsonResponse(jsonString: String): AgentChatResponse {
        val root = JSONObject(jsonString)
        val candidates = root.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) {
            throw IllegalStateException("No agent response received from Gemini")
        }

        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        if (parts == null || parts.length() == 0) {
            throw IllegalStateException("Empty response parts from Gemini Agent")
        }

        var textContent = parts.getJSONObject(0).optString("text", "").trim()
        val groundingData = extractGroundingMetadata(firstCandidate)

        var userTranscript: String? = null
        var replyText = textContent
        var language = "Auto"
        val followUpsList = mutableListOf<String>()

        try {
            var jsonCleaned = textContent
            if (jsonCleaned.startsWith("```json")) {
                jsonCleaned = jsonCleaned.removePrefix("```json").removeSuffix("```").trim()
            } else if (jsonCleaned.startsWith("```")) {
                jsonCleaned = jsonCleaned.removePrefix("```").removeSuffix("```").trim()
            }

            val parsed = JSONObject(jsonCleaned)
            userTranscript = parsed.optString("userTranscript", "").trim().ifBlank { null }
            val parsedReply = parsed.optString("replyText", "").trim()
            if (parsedReply.isNotBlank()) {
                replyText = parsedReply
            }
            language = parsed.optString("language", "Auto").trim()

            val followUpsArray = parsed.optJSONArray("suggestedFollowUps")
            if (followUpsArray != null) {
                for (i in 0 until followUpsArray.length()) {
                    val item = followUpsArray.optString(i)
                    if (item.isNotBlank()) {
                        followUpsList.add(item.trim())
                    }
                }
            }
        } catch (e: Exception) {
            // If plain text was returned instead of JSON, keep textContent as replyText
            replyText = textContent
        }

        return AgentChatResponse(
            userSpokenTranscription = userTranscript,
            replyText = replyText.ifBlank { "Ideia muito interessante! Como você gostaria de aprofundar esse plano?" },
            suggestedFollowUps = followUpsList,
            language = language,
            searchQueries = groundingData.searchQueries,
            groundingSources = groundingData.sources
        )
    }

    private fun parseEnrichmentResponse(noteId: Long, jsonString: String): NoteEnrichmentResult {
        val root = JSONObject(jsonString)
        val candidates = root.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) {
            throw IllegalStateException("No enrichment response received from Gemini")
        }

        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        if (parts == null || parts.length() == 0) {
            throw IllegalStateException("Empty enrichment response parts from Gemini")
        }

        var textContent = parts.getJSONObject(0).optString("text", "").trim()
        val groundingData = extractGroundingMetadata(firstCandidate)

        var analysis = textContent
        val factsList = mutableListOf<String>()

        try {
            var jsonCleaned = textContent
            if (jsonCleaned.startsWith("```json")) {
                jsonCleaned = jsonCleaned.removePrefix("```json").removeSuffix("```").trim()
            } else if (jsonCleaned.startsWith("```")) {
                jsonCleaned = jsonCleaned.removePrefix("```").removeSuffix("```").trim()
            }

            val parsed = JSONObject(jsonCleaned)
            val parsedAnalysis = parsed.optString("enrichedAnalysis", "").trim()
            if (parsedAnalysis.isNotBlank()) {
                analysis = parsedAnalysis
            }

            val factsArray = parsed.optJSONArray("verifiedFacts")
            if (factsArray != null) {
                for (i in 0 until factsArray.length()) {
                    val item = factsArray.optString(i)
                    if (item.isNotBlank()) {
                        factsList.add(item.trim())
                    }
                }
            }
        } catch (e: Exception) {
            analysis = textContent
        }

        return NoteEnrichmentResult(
            noteId = noteId,
            enrichedAnalysis = analysis,
            verifiedFacts = factsList,
            searchQueries = groundingData.searchQueries,
            sources = groundingData.sources
        )
    }

    private fun extractGroundingMetadata(candidateObj: JSONObject): GroundingMetadata {
        val queriesList = mutableListOf<String>()
        val sourcesList = mutableListOf<GroundingSource>()

        val groundingMetadata = candidateObj.optJSONObject("groundingMetadata") ?: return GroundingMetadata()

        // Extract webSearchQueries
        val webQueries = groundingMetadata.optJSONArray("webSearchQueries")
        if (webQueries != null) {
            for (i in 0 until webQueries.length()) {
                val q = webQueries.optString(i).trim()
                if (q.isNotBlank() && !queriesList.contains(q)) {
                    queriesList.add(q)
                }
            }
        }

        // Extract groundingChunks -> web
        val groundingChunks = groundingMetadata.optJSONArray("groundingChunks")
        if (groundingChunks != null) {
            for (i in 0 until groundingChunks.length()) {
                val chunk = groundingChunks.optJSONObject(i) ?: continue
                val webObj = chunk.optJSONObject("web") ?: continue
                val uri = webObj.optString("uri", "").trim()
                val title = webObj.optString("title", "").trim()
                if (uri.isNotBlank() && sourcesList.none { it.uri == uri }) {
                    sourcesList.add(
                        GroundingSource(
                            title = title.ifBlank { GroundingSource.extractDomain(uri) },
                            uri = uri,
                            domain = GroundingSource.extractDomain(uri)
                        )
                    )
                }
            }
        }

        return GroundingMetadata(
            searchQueries = queriesList,
            sources = sourcesList
        )
    }

    /**
     * Translates polished text into another language maintaining tone, structure and formatting.
     */
    suspend fun translateText(
        text: String,
        targetLanguage: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            if (apiKey.isBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("Gemini API Key is missing.")
                )
            }

            val prompt = """
                Translate the following text into $targetLanguage.
                Maintain the exact same structure, paragraph breaks, bullet points, and professional tone.
                Do NOT add any conversational preamble or explanations. Return ONLY the translated text.

                TEXT TO TRANSLATE:
                $text
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray()
                val contentObj = JSONObject()
                val partsArray = JSONArray()

                partsArray.put(JSONObject().apply {
                    put("text", prompt)
                })

                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
                put("contents", contentsArray)

                val generationConfig = JSONObject().apply {
                    put("temperature", 0.2)
                }
                put("generationConfig", generationConfig)
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$baseUrl?key=$apiKey")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = extractErrorMessage(responseBody) ?: "HTTP ${response.code}: ${response.message}"
                return@withContext Result.failure(Exception("Gemini API Error: $errorMsg"))
            }

            val root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val translated = parts?.optJSONObject(0)?.optString("text", "")?.trim() ?: text
            Result.success(translated)
        } catch (e: Exception) {
            Log.e("GeminiService", "Error translating text", e)
            Result.failure(e)
        }
    }

    /**
     * Extracts actionable tasks / checklist items from note content.
     */
    suspend fun generateActionChecklist(
        noteTitle: String,
        noteContent: String
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            if (apiKey.isBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("Gemini API Key is missing.")
                )
            }

            val prompt = """
                Analyze the following note content and extract a list of concrete, actionable tasks / checklist items.
                Each item should be a clear, concise action starting with an imperative verb.
                Generate between 3 and 7 practical tasks based on the note.

                NOTE TITLE: $noteTitle
                NOTE CONTENT:
                $noteContent

                Respond ONLY with a valid JSON array of strings in the same language as the note, e.g.:
                ["Create presentation slides", "Schedule follow-up meeting with team", "Send updated budget proposal"]
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray()
                val contentObj = JSONObject()
                val partsArray = JSONArray()

                partsArray.put(JSONObject().apply {
                    put("text", prompt)
                })

                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
                put("contents", contentsArray)

                val generationConfig = JSONObject().apply {
                    put("temperature", 0.3)
                    val responseFormat = JSONObject().apply {
                        put("mimeType", "application/json")
                    }
                    put("responseFormat", responseFormat)
                }
                put("generationConfig", generationConfig)
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$baseUrl?key=$apiKey")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = extractErrorMessage(responseBody) ?: "HTTP ${response.code}: ${response.message}"
                return@withContext Result.failure(Exception("Gemini API Error: $errorMsg"))
            }

            val root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            var text = parts?.optJSONObject(0)?.optString("text", "")?.trim() ?: "[]"
            if (text.startsWith("```json")) {
                text = text.removePrefix("```json").removeSuffix("```").trim()
            } else if (text.startsWith("```")) {
                text = text.removePrefix("```").removeSuffix("```").trim()
            }

            val jsonArray = JSONArray(text)
            val checklist = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.optString(i).trim()
                if (item.isNotBlank()) {
                    checklist.add(item)
                }
            }
            Result.success(checklist)
        } catch (e: Exception) {
            Log.e("GeminiService", "Error generating action checklist", e)
            Result.failure(e)
        }
    }

    /**
     * Repurposes note content into specialized formats (LinkedIn Post, Twitter Thread, Exec Memo, Newsletter).
     */
    suspend fun repurposeContent(
        text: String,
        targetPurpose: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            if (apiKey.isBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("Gemini API Key is missing.")
                )
            }

            val prompt = """
                You are AudioPen AI, a world-class content repurposing strategist.
                Take the provided note content and transform it into a high-impact $targetPurpose.
                
                FORMAT SPECIFICATIONS:
                - If "LinkedIn Post": Use hook sentence, short scannable paragraphs, bullet points, engaging insights, and 3-5 relevant hashtags at the bottom.
                - If "Twitter/X Thread": Format as a numbered thread (1/, 2/, 3/, ...) with punchy tweets under 280 chars each, starting with an irresistible hook.
                - If "Executive Memo": Structured with Header (To, Date, Subject), Executive Summary, Key Decisions/Findings, and Next Steps.
                - If "Email Newsletter": Engaging subject line suggestions, personal intro, curated body insights, and a clear call-to-action (CTA).
                - If "Action Plan": Objective, Milestone breakdown with phases, Resource requirements, and Success metrics.
                
                Respond in the SAME language as the original text.
                Return ONLY the repurposed content without any surrounding conversational wrapper.

                ORIGINAL CONTENT:
                $text
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray()
                val contentObj = JSONObject()
                val partsArray = JSONArray()

                partsArray.put(JSONObject().apply {
                    put("text", prompt)
                })

                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
                put("contents", contentsArray)

                val generationConfig = JSONObject().apply {
                    put("temperature", 0.4)
                }
                put("generationConfig", generationConfig)
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$baseUrl?key=$apiKey")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = extractErrorMessage(responseBody) ?: "HTTP ${response.code}: ${response.message}"
                return@withContext Result.failure(Exception("Gemini API Error: $errorMsg"))
            }

            val root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val repurposed = parts?.optJSONObject(0)?.optString("text", "")?.trim() ?: text
            Result.success(repurposed)
        } catch (e: Exception) {
            Log.e("GeminiService", "Error repurposing content", e)
            Result.failure(e)
        }
    }

    private fun extractErrorMessage(responseBody: String): String? {
        return try {
            val json = JSONObject(responseBody)
            val error = json.optJSONObject("error")
            error?.optString("message")
        } catch (e: Exception) {
            null
        }
    }
}
