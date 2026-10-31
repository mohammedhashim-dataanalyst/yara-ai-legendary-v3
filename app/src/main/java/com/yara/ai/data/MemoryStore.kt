package com.yara.ai.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * Local brain — chat history, memories, learned replies, features, traits,
 * providers. Fully offline. Each file deserializes with an explicit TypeToken
 * so generic types are never erased (a generic <T> TypeToken corrupts data).
 */
class MemoryStore private constructor(ctx: Context) {

    private val gson = Gson()
    private val root = File(ctx.filesDir, "yara_brain").apply { mkdirs() }

    private val historyFile = File(root, "chat_history.json")
    private val memoriesFile = File(root, "memories.json")
    private val learnedFile = File(root, "learned.json")
    private val featuresFile = File(root, "features.json")
    private val providersFile = File(root, "providers.json")
    private val traitsFile = File(root, "traits.json")
    private val profileFile = File(root, "profile.json")

    var history: MutableList<ChatMessage> = mutableListOf()
    var memories: MutableList<Memory> = mutableListOf()
    var learned: MutableMap<String, String> = mutableMapOf()
    var features: MutableMap<String, Boolean> = mutableMapOf()
    var providers: MutableList<ProviderState> = mutableListOf()
    var traits: MutableMap<String, TraitSetting> = mutableMapOf()
    var profile: UserProfile = UserProfile()

    @Synchronized
    fun load() {
        history = readMessages(historyFile)
        memories = readMemories(memoriesFile)
        learned = readStringMap(learnedFile)
        features = readBoolMap(featuresFile)
        providers = readProviders(providersFile)
        traits = readTraitMap(traitsFile)
        profile = readProfile(profileFile)
        if (providers.isEmpty()) {
            providers = mutableListOf(
                ProviderState("Gemini"),
                ProviderState("Grok"),
                ProviderState("OpenRouter"),
                ProviderState("HuggingFace")
            )
            writeJson(providersFile, providers)
        }
    }

    @Synchronized
    fun addMessage(m: ChatMessage) {
        history.add(m)
        if (history.size > 2000) history = history.takeLast(2000).toMutableList()
        writeJson(historyFile, history)
    }

    @Synchronized
    fun addMemory(text: String, importance: Int = 5) {
        memories.add(Memory(text = text, importance = importance))
        writeJson(memoriesFile, memories)
    }

    @Synchronized
    fun deleteMemory(id: String) {
        memories.removeAll { it.id == id }
        writeJson(memoriesFile, memories)
    }

    fun recallMemories(query: String, limit: Int = 8): List<Memory> {
        val q = query.lowercase()
        val now = System.currentTimeMillis()
        return memories.map { m ->
            var score = m.importance.toDouble()
            if (q.isNotBlank() && q.split(" ").any { m.text.lowercase().contains(it) }) score += 4.0
            score += (1.0 - ((now - m.createdAt).coerceAtLeast(0) / 86_400_000.0 / 30)).coerceIn(0.0, 1.0)
            m to score
        }.sortedByDescending { it.second }.take(limit).map { it.first }
    }

    @Synchronized
    fun teach(prompt: String, reply: String) {
        learned[prompt.lowercase().take(160)] = reply
        if (learned.size > 5000) {
            val keep = learned.entries.toList().takeLast(4000)
            val trimmed = mutableMapOf<String, String>()
            for (e in keep) trimmed[e.key] = e.value
            learned = trimmed
        }
        writeJson(learnedFile, learned)
    }

    fun offlineReply(prompt: String): String? {
        val p = prompt.lowercase()
        learned[p]?.let { return it }
        val words = p.split(Regex("\\s+")).filter { it.length > 2 }.toSet()
        var best: String? = null
        var bestScore = 0.0
        for ((k, v) in learned) {
            val kw = k.split(Regex("\\s+")).filter { it.length > 2 }.toSet()
            if (kw.isEmpty()) continue
            val inter = words.intersect(kw).size.toDouble()
            val score = inter / (words.size + kw.size - inter)
            if (score > bestScore && score > 0.6) { bestScore = score; best = v }
        }
        return best
    }

    @Synchronized
    fun saveHistory() { writeJson(historyFile, history) }

    @Synchronized
    fun saveMemories() { writeJson(memoriesFile, memories) }

    @Synchronized
    fun setFeature(key: String, enabled: Boolean) {
        features[key] = enabled
        writeJson(featuresFile, features)
    }
    fun isFeatureOn(key: String, def: Boolean = false) = features[key] ?: def

    @Synchronized
    fun setTrait(t: TraitSetting) {
        traits[t.key] = t
        writeJson(traitsFile, traits)
    }

    @Synchronized
    fun saveProviders() { writeJson(providersFile, providers) }

    @Synchronized
    fun saveProfile(value: UserProfile = profile) { profile = value; writeJson(profileFile, profile) }

    // ---- typed readers (no erasure) ----
    private fun readMessages(f: File): MutableList<ChatMessage> = try {
        if (!f.exists()) mutableListOf()
        else gson.fromJson<MutableList<ChatMessage>>(f.readText(), object : TypeToken<MutableList<ChatMessage>>() {}.type) ?: mutableListOf()
    } catch (e: Exception) { mutableListOf() }

    private fun readMemories(f: File): MutableList<Memory> = try {
        if (!f.exists()) mutableListOf()
        else gson.fromJson<MutableList<Memory>>(f.readText(), object : TypeToken<MutableList<Memory>>() {}.type) ?: mutableListOf()
    } catch (e: Exception) { mutableListOf() }

    private fun readProviders(f: File): MutableList<ProviderState> = try {
        if (!f.exists()) mutableListOf()
        else (gson.fromJson<MutableList<ProviderState>>(f.readText(), object : TypeToken<MutableList<ProviderState>>() {}.type) ?: mutableListOf()).also { list ->
            list.forEach { provider ->
                provider.baseUrl = provider.baseUrl ?: ""
                provider.model = provider.model ?: ""
            }
        }
    } catch (e: Exception) { mutableListOf() }

    private fun readStringMap(f: File): MutableMap<String, String> = try {
        if (!f.exists()) mutableMapOf()
        else gson.fromJson<MutableMap<String, String>>(f.readText(), object : TypeToken<MutableMap<String, String>>() {}.type) ?: mutableMapOf()
    } catch (e: Exception) { mutableMapOf() }

    private fun readBoolMap(f: File): MutableMap<String, Boolean> = try {
        if (!f.exists()) mutableMapOf()
        else gson.fromJson<MutableMap<String, Boolean>>(f.readText(), object : TypeToken<MutableMap<String, Boolean>>() {}.type) ?: mutableMapOf()
    } catch (e: Exception) { mutableMapOf() }

    private fun readProfile(f: File): UserProfile = try {
        if (!f.exists()) UserProfile()
        else (gson.fromJson(f.readText(), UserProfile::class.java) ?: UserProfile()).also { value ->
            value.userName = value.userName ?: ""
            value.languageMode = value.languageMode ?: "english"
            value.personaMode = value.personaMode ?: "companion"
            value.homeAssistantUrl = value.homeAssistantUrl ?: ""
            value.homeAssistantToken = value.homeAssistantToken ?: ""
            value.pcGatewayUrl = value.pcGatewayUrl ?: ""
        }
    } catch (e: Exception) { UserProfile() }

    private fun readTraitMap(f: File): MutableMap<String, TraitSetting> = try {
        if (!f.exists()) mutableMapOf()
        else gson.fromJson<MutableMap<String, TraitSetting>>(f.readText(), object : TypeToken<MutableMap<String, TraitSetting>>() {}.type) ?: mutableMapOf()
    } catch (e: Exception) { mutableMapOf() }

    private fun writeJson(f: File, v: Any) { try { f.writeText(gson.toJson(v)) } catch (e: Exception) {} }

    companion object {
        @Volatile private var I: MemoryStore? = null
        fun get(ctx: Context): MemoryStore =
            I ?: synchronized(this) { I ?: MemoryStore(ctx.applicationContext).also { I = it } }
    }
}
