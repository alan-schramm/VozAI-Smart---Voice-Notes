package com.example.data.model

enum class FormatStyle(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val systemPromptGuide: String
) {
    CLASSIC(
        id = "classic",
        title = "AudioPen Classic",
        description = "Executive, clear, well-flowing paragraphs",
        iconName = "AutoAwesome",
        systemPromptGuide = """
            Rewrite the spoken thoughts into clean, executive, and natural-sounding prose.
            - Remove all filler words (um, uh, like, you know, sort of), false starts, repetitions, and stuttering.
            - Fix grammar, improve clarity, and organize into logical, coherent paragraphs with smooth transitions.
            - Maintain the author's original voice, intent, and perspective (first person if spoken in first person).
            - Keep the tone confident, articulate, and professional.
        """.trimIndent()
    ),
    EMAIL(
        id = "email",
        title = "Email Draft",
        description = "Ready-to-send professional email with subject",
        iconName = "Mail",
        systemPromptGuide = """
            Transform the spoken thoughts into a crisp, polished email ready to be sent.
            - Provide a compelling, concise Subject line at the start.
            - Structure with an appropriate greeting, a clear context opening, structured body paragraphs or bullet points for key details, a clear call-to-action or next steps, and a professional sign-off.
            - Ensure polite, confident, and executive tone.
        """.trimIndent()
    ),
    BULLET_MEMO(
        id = "bullet_memo",
        title = "Bullet Memo",
        description = "Executive summary with bulleted action items",
        iconName = "FormatListBulleted",
        systemPromptGuide = """
            Structure the thoughts into an executive bullet-point memorandum:
            - Start with a 2-3 sentence Executive Overview.
            - Group key points under clear thematic subheadings with bullet points.
            - Highlight Decisions Made and Action Items with clear ownership/next steps where applicable.
        """.trimIndent()
    ),
    SOCIAL_POST(
        id = "social",
        title = "Social & LinkedIn",
        description = "Engaging post with hook and readability",
        iconName = "Share",
        systemPromptGuide = """
            Turn the spoken ideas into an engaging social media post (ideal for LinkedIn or Twitter/X).
            - Start with a strong, attention-grabbing hook on the first line.
            - Use short, punchy paragraphs with generous line breaks for mobile readability.
            - End with an insightful takeaway and a question to prompt discussion/engagement.
            - Include 2-3 relevant hashtags at the very bottom.
        """.trimIndent()
    ),
    TODO_LIST(
        id = "todo",
        title = "Action To-Dos",
        description = "Categorized actionable tasks with priorities",
        iconName = "CheckCircle",
        systemPromptGuide = """
            Extract and organize all actionable tasks, commitments, and to-do items from the transcript.
            - Group items by category or priority (High Priority / Immediate, Next Steps, Ideas & Follow-ups).
            - Phrase each item starting with an active imperative verb (e.g., "Schedule review with...", "Draft proposal for...").
            - Include any mentioned deadlines or dependencies.
        """.trimIndent()
    ),
    MEETING_MINUTES(
        id = "meeting",
        title = "Meeting Minutes",
        description = "Structured discussion points & next steps",
        iconName = "Groups",
        systemPromptGuide = """
            Organize the spoken content into structured meeting minutes:
            - Main Topic & Objective
            - Key Discussions & Arguments
            - Decisions Agreed Upon
            - Next Steps & Action Owners
        """.trimIndent()
    ),
    BRAINSTORM(
        id = "brainstorm",
        title = "AI Brainstorm",
        description = "Idea exploration, angles & strategic next steps",
        iconName = "Lightbulb",
        systemPromptGuide = """
            Structure the brainstormed ideas into an expansive strategic canvas:
            - Core Concept & Value Proposition
            - Potential Angles & Strategic Opportunities
            - Risks, Blindspots & Assumptions
            - Concrete Milestones & Immediate Next Experiments
        """.trimIndent()
    );

    companion object {
        fun fromId(id: String): FormatStyle {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: CLASSIC
        }
    }
}
