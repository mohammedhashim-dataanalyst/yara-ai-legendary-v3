package com.yara.ai.data

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val fromYara: Boolean,
    val kind: String = "TEXT",
    val text: String = "",
    val emotion: String = "neutral",
    val provider: String = "local",
    val edited: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class Memory(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val tags: List<String> = emptyList(),
    val importance: Int = 5,
    val source: String = "chat",
    val createdAt: Long = System.currentTimeMillis()
)

data class TraitSetting(
    val key: String,
    val title: String,
    val description: String,
    val isAuto: Boolean = true,
    val level: Int = 6
)

data class ProviderState(
    val name: String,
    var apiKey: String = "",
    var enabled: Boolean = false,
    var lastLatencyMs: Long = Long.MAX_VALUE,
    var failures: Int = 0,
    var rateLimitedUntil: Long = 0L,
    var baseUrl: String? = null,
    var model: String? = null
)
