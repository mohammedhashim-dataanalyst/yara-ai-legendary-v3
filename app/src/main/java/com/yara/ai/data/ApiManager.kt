package com.yara.ai.data

import android.util.Log
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/** Provider cascade: latency-ranked healthy APIs, local learned replies, then persona. */
class ApiManager(private val store: MemoryStore) {
    private val client = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS).build()
    private val json = "application/json; charset=utf-8".toMediaType()
    @Volatile var manualPrimary: String = ""
    @Volatile var autoAdaptive: Boolean = true

    suspend fun ask(systemPrompt: String, userText: String): Pair<String, String> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val usable = store.providers.filter { it.enabled && it.apiKey.isNotBlank() && now >= it.rateLimitedUntil }
        val manual = usable.firstOrNull { it.name.equals(manualPrimary, true) }
        val ranked = if (autoAdaptive) usable.sortedBy { it.lastLatencyMs } else usable
        val order = listOfNotNull(manual) + ranked.filter { it != manual }
        for (provider in order) {
            try {
                val started = System.currentTimeMillis()
                val reply = callProvider(provider, systemPrompt, userText)
                val latency = System.currentTimeMillis() - started
                if (!reply.isNullOrBlank()) {
                    provider.lastLatencyMs = if (provider.lastLatencyMs == Long.MAX_VALUE) latency else (provider.lastLatencyMs + latency) / 2
                    provider.failures = 0
                    store.saveProviders()
                    return@withContext reply.trim() to provider.name
                }
            } catch (rate: RateLimit) {
                provider.rateLimitedUntil = now + RATE_LIMIT_COOLDOWN_MS
                provider.failures++
                store.saveProviders()
            } catch (error: Exception) {
                Log.w("ApiManager", "${provider.name} failed: ${error.message}")
                provider.failures++
                store.saveProviders()
            }
        }
        store.offlineReply(userText)?.let { return@withContext it to "local_brain" }
        val emotion = com.yara.ai.brain.EmotionEngine.detect(userText)
        return@withContext com.yara.ai.brain.PersonaEngine.offlineFallback(userText, emotion, store.profile.userName, store.profile.languageMode, store.profile.mixLanguages) to "local_persona"
    }

    private fun callProvider(provider: ProviderState, system: String, user: String): String? = when (provider.name.lowercase()) {
        "gemini" -> callGemini(provider, system, user)
        "grok" -> callOpenAiStyle(provider, provider.baseUrl?.ifBlank { "https://api.x.ai/v1/chat/completions" } ?: "https://api.x.ai/v1/chat/completions", provider.model?.ifBlank { "grok-2-latest" } ?: "grok-2-latest", system, user)
        "openrouter" -> callOpenAiStyle(provider, provider.baseUrl?.ifBlank { "https://openrouter.ai/api/v1/chat/completions" } ?: "https://openrouter.ai/api/v1/chat/completions", provider.model?.ifBlank { "openai/gpt-4o-mini" } ?: "openai/gpt-4o-mini", system, user)
        "huggingface" -> callHuggingFace(provider, system, user)
        else -> null
    }

    private fun callGemini(p: ProviderState, system: String, user: String): String? {
        val model = p.model?.ifBlank { "gemini-1.5-flash" } ?: "gemini-1.5-flash"
        val base = p.baseUrl?.ifBlank { "https://generativelanguage.googleapis.com/v1beta" } ?: "https://generativelanguage.googleapis.com/v1beta"
        val url = when {
            base.contains("{model}") -> base.replace("{model}", model) + if (base.contains("?") ) "&key=${p.apiKey}" else "?key=${p.apiKey}"
            base.contains(":generateContent") -> base + if (base.contains("?") ) "&key=${p.apiKey}" else "?key=${p.apiKey}"
            else -> "$base/models/$model:generateContent?key=${p.apiKey}"
        }
        val body = JsonObject().apply {
            add("system_instruction", JsonObject().apply { add("parts", com.google.gson.JsonArray().apply { add(JsonObject().apply { addProperty("text", system) }) }) })
            add("contents", com.google.gson.JsonArray().apply { add(JsonObject().apply { add("parts", com.google.gson.JsonArray().apply { add(JsonObject().apply { addProperty("text", user) }) }) }) })
            add("generationConfig", JsonObject().apply { addProperty("temperature", 0.9); addProperty("maxOutputTokens", 300) })
        }.toString().toRequestBody(json)
        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).execute().use { response ->
            if (response.code == 429) throw RateLimit()
            if (!response.isSuccessful) throw Exception("gemini ${response.code}")
            val root = JsonParser.parseString(response.body?.string() ?: return null).asJsonObject
            return root["candidates"]?.asJsonArray?.firstOrNull()?.asJsonObject?.get("content")?.asJsonObject?.get("parts")?.asJsonArray?.firstOrNull()?.asJsonObject?.get("text")?.asString
        }
    }

    private fun callOpenAiStyle(p: ProviderState, url: String, model: String, system: String, user: String): String? {
        val body = JsonObject().apply {
            addProperty("model", model); addProperty("max_tokens", 300); addProperty("temperature", 0.9)
            add("messages", com.google.gson.JsonArray().apply {
                add(JsonObject().apply { addProperty("role", "system"); addProperty("content", system) })
                add(JsonObject().apply { addProperty("role", "user"); addProperty("content", user) })
            })
        }.toString().toRequestBody(json)
        val requestBuilder = Request.Builder().url(url).addHeader("Authorization", "Bearer ${p.apiKey}")
        if (p.name.equals("OpenRouter", true)) requestBuilder.addHeader("HTTP-Referer", "https://yara.ai")
        client.newCall(requestBuilder.post(body).build()).execute().use { response ->
            if (response.code == 429) throw RateLimit()
            if (!response.isSuccessful) throw Exception("${p.name} ${response.code}")
            return JsonParser.parseString(response.body?.string() ?: return null).asJsonObject["choices"]?.asJsonArray?.firstOrNull()?.asJsonObject?.get("message")?.asJsonObject?.get("content")?.asString
        }
    }

    private fun callHuggingFace(p: ProviderState, system: String, user: String): String? {
        val url = p.baseUrl?.ifBlank { "https://api-inference.huggingface.co/models/" + (p.model?.ifBlank { "mistralai/Mistral-7B-Instruct-v0.2" } ?: "mistralai/Mistral-7B-Instruct-v0.2") } ?: "https://api-inference.huggingface.co/models/" + (p.model?.ifBlank { "mistralai/Mistral-7B-Instruct-v0.2" } ?: "mistralai/Mistral-7B-Instruct-v0.2")
        val prompt = "<s>[INST] $system\n\n$user [/INST]"
        val body = JsonObject().apply { addProperty("inputs", prompt); add("parameters", JsonObject().apply { addProperty("max_new_tokens", 200); addProperty("temperature", 0.9) }) }.toString().toRequestBody(json)
        client.newCall(Request.Builder().url(url).addHeader("Authorization", "Bearer ${p.apiKey}").post(body).build()).execute().use { response ->
            if (response.code == 429) throw RateLimit()
            if (!response.isSuccessful) throw Exception("huggingface ${response.code}")
            val array = JsonParser.parseString(response.body?.string() ?: return null).asJsonArray
            return array.firstOrNull()?.asJsonObject?.get("generated_text")?.asString?.substringAfter("[/INST]")?.trim()
        }
    }
    class RateLimit : Exception("rate limited")
    companion object { private const val RATE_LIMIT_COOLDOWN_MS = 60_000L }
}
