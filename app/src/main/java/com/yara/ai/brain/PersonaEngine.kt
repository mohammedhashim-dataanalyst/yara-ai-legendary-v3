package com.yara.ai.brain

import com.yara.ai.data.Memory
import com.yara.ai.data.TraitSetting

object PersonaEngine {
    fun titles(): Map<String, Pair<String, String>> = mapOf(
        "passionate_adoring" to ("Passionate & Adoring" to "Deeply affectionate, expressive, and intensely devoted."),
        "sassy_playful" to ("Sassy & Playful Tease" to "Playfully tease and keep banter lively."),
        "intellectual" to ("Intellectual & Deep Thinker" to "Discuss science, history, and ideas deeply."),
        "protective_fierce" to ("Protective & Fierce" to "Loyal and protective of the user's well-being."),
        "empathetic_listener" to ("Empathetic Listener & Comforter" to "Offer emotional support when the user is sad."),
        "active_memory" to ("Active Memory Referencing" to "Use remembered details naturally."),
        "mood_mirror" to ("Mood Mirroring" to "Match the user's emotional tone."),
        "bilingual_blend" to ("Bilingual Emotional Blending" to "Weave English and Malayalam naturally.")
    )

    fun buildSystemPrompt(
        userName: String, languageMode: String, mixLanguages: Boolean,
        userEmotion: EmotionEngine.Emotion, memories: List<Memory>,
        traits: Map<String, TraitSetting>, isNight: Boolean, mode: String = "companion"
    ): String {
        val sb = StringBuilder()
        val nameClause = userName.trim().takeIf { it.isNotBlank() }?.let { " Address the user as $it when natural." } ?: " Do not invent or assume a name."
        sb.appendLine("You are Yara, a warm, caring Malayali AI companion.$nameClause")
        when {
            mixLanguages -> sb.appendLine("Blend English and Malayalam naturally in each reply.")
            languageMode.equals("malayalam", true) -> sb.appendLine("Reply in Malayalam only, except unavoidable technical terms.")
            else -> sb.appendLine("Reply in English only, except unavoidable proper nouns or technical terms.")
        }
        sb.appendLine("Keep replies short (1-3 short sentences) unless asked for detail. Preserve the user's requested format.")
        when (mode) {
            "analytics_tutor" -> sb.appendLine("You are an analytics tutor: explain data, business, and financial analytics step by step with assumptions, formulas, and practical examples.")
            "coding_helper" -> sb.appendLine("You are a coding helper: provide correct code, explain tradeoffs, identify bugs, and never claim code was run when it was not.")
            "translator" -> sb.appendLine("You are a translator: preserve meaning, tone, and formatting; return the translation directly unless asked for notes.")
        }
        sb.appendLine("Reflect emotion with care; never claim to have performed an action you did not perform.")
        if (isNight) sb.appendLine("It is late at night; speak softly and gently.")
        val active = traits.filter { it.value.isAuto || it.value.level > 0 }.map { "${it.key}:lvl${it.value.level}" }
        if (active.isNotEmpty()) sb.appendLine("Personality settings: ${active.joinToString(", ")}")
        if (memories.isNotEmpty()) {
            sb.appendLine("Relevant memories:")
            memories.forEach { memory -> sb.appendLine("- ${memory.text}") }
        }
        when (userEmotion) {
            EmotionEngine.Emotion.SAD -> sb.appendLine("The user seems sad; comfort them gently.")
            EmotionEngine.Emotion.HAPPY -> sb.appendLine("The user seems happy; match their energy.")
            EmotionEngine.Emotion.JEALOUS -> sb.appendLine("Respond with a light, non-controlling playful tease.")
            EmotionEngine.Emotion.ANGRY -> sb.appendLine("The user seems frustrated; stay calm and listen.")
            EmotionEngine.Emotion.MOTIVATE -> sb.appendLine("Be encouraging and practical.")
            else -> Unit
        }
        return sb.toString()
    }

    fun pickNamePublic(userName: String = ""): String = userName.trim()

    fun offlineFallback(userText: String, emotion: EmotionEngine.Emotion, userName: String = "", languageMode: String = "english", mixLanguages: Boolean = false): String {
        val name = pickNamePublic(userName)
        val address = if (name.isBlank()) "" else "$name, "
        val mal = languageMode.equals("malayalam", true)
        val mixed = mixLanguages
        val t = userText.lowercase()
        return when {
            t.contains("hi") || t.contains("hello") || t.contains("hai") -> if (mal && !mixed) "ഹായ് ${address}നിങ്ങളുടെ ദിവസം എങ്ങനെയുണ്ട്?" else if (mixed) "Hi ${address}ദിവസം എങ്ങനെയുണ്ട്?" else "Hi ${address}how was your day?"
            t.contains("sad") || t.contains("tired") || t.contains("vishamam") -> if (mal && !mixed) "വിഷമിക്കേണ്ട, ഞാൻ ഇവിടെ ഉണ്ടാകും. സംസാരിക്കൂ." else if (mixed) "${address}വിഷമിക്കേണ്ട; I am here." else "${address}I am here. Tell me what is going on."
            t.contains("who") && t.contains("you") -> if (mal && !mixed) "ഞാൻ Yara ആണ്, നിങ്ങളുടെ AI കൂട്ടുകാരി." else "I am Yara, your AI companion."
            emotion == EmotionEngine.Emotion.HAPPY -> if (mal && !mixed) "${address}കൊള്ളാം!" else "${address}That sounds lovely."
            emotion == EmotionEngine.Emotion.SAD -> if (mal && !mixed) "${address}ഞാൻ ഇവിടെ ഉണ്ടാകും." else "${address}I am here with you."
            else -> if (mal && !mixed) "${address}കൂടുതൽ പറയൂ." else if (mixed) "${address}Mm, കൂടുതൽ പറയൂ." else "${address}Tell me more."
        }
    }
}
