package com.yara.ai.data

data class UserProfile(
    var userName: String = "",
    var languageMode: String = "english",
    var mixLanguages: Boolean = false,
    var personaMode: String = "companion",
    var voicePitch: Float = 1.0f,
    var voiceRate: Float = 1.0f,
    var voiceLanguage: String = "en-IN",
    var vipSenders: MutableList<String> = mutableListOf(),
    var lastSentAt: Long = 0L,
    var lastLearnedAt: Long = 0L,
    var homeAssistantUrl: String = "",
    var homeAssistantToken: String = "",
    var pcGatewayUrl: String = ""
)
