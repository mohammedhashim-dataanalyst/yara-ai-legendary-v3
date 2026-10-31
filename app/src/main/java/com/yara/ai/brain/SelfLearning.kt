package com.yara.ai.brain

import com.yara.ai.data.MemoryStore

object SelfLearning {
    private const val DAY = 86_400_000L
    fun scan(store: MemoryStore) {
        if (!store.isFeatureOn("auto_learn", true) || System.currentTimeMillis() - store.profile.lastLearnedAt < DAY) return
        val recent = store.history.takeLast(80).filter { !it.fromYara }.map { it.text }
        val patterns = listOf(
            Regex("(?i)my\\s+([\\w ]{1,40})\\s+is\\s+([^.!?]{1,100})"),
            Regex("(?i)i\\s+(?:like|love)\\s+([^.!?]{1,100})"),
            Regex("(?i)i\\s+work\\s+(?:at|as|in)\\s+([^.!?]{1,100})"),
            Regex("(?i)എന്റെ\\s+([^.!?]{1,40})\\s+(?:ആണ്|ആകുന്നു)\\s+([^.!?]{1,100})"),
            Regex("(?i)എനിക്ക്\\s+([^.!?]{1,100})\\s+ഇഷ്ടമാണ്")
        )
        val existing = store.memories.map { it.text.trim().lowercase() }.toSet()
        for (line in recent) for (pattern in patterns) pattern.findAll(line).forEach { match ->
            val fact = match.value.trim().replace(Regex("\\s+"), " ")
            if (fact.length >= 5 && fact.lowercase() !in existing) { store.addMemory(fact, 7) }
        }
        store.profile.lastLearnedAt = System.currentTimeMillis(); store.saveProfile()
    }
}
