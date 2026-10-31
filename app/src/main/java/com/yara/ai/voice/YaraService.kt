package com.yara.ai.voice

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.yara.ai.YaraApp
import com.yara.ai.brain.EmotionEngine
import com.yara.ai.brain.PersonaEngine
import com.yara.ai.data.ApiManager
import com.yara.ai.data.ChatMessage
import com.yara.ai.data.MemoryStore
import com.yara.ai.device.CommandRouter
import com.yara.ai.notify.Notifier
import com.yara.ai.overlay.OverlayService
import com.yara.ai.ui.MainActivity
import kotlinx.coroutines.*

class YaraService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var store: MemoryStore
    private lateinit var api: ApiManager
    private lateinit var voice: VoiceEngine
    private var awake = false

    override fun onCreate() {
        super.onCreate()
        store = MemoryStore.get(this)
        api = ApiManager(store)
        voice = VoiceEngine(this, onSpeechResult = { handleSpeech(it) })
        scope.launch { withContext(Dispatchers.IO) { store.load() } }
        startForegroundWithNotification()
        voice.startListening()
    }

    private fun startForegroundWithNotification() {
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n: Notification = NotificationCompat.Builder(this, YaraApp.CHANNEL_SERVICE)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Yara is listening").setContentText("Say 'hey yara'")
            .setContentIntent(pi).setOngoing(true).build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(101, n, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else startForeground(101, n)
    }

    private fun handleSpeech(text: String) {
        val lower = text.lowercase()
        when {
            !awake && (lower.contains("hey yara") || lower.contains("yara")) -> {
                awake = true
                voice.speak(if (store.profile.userName.isBlank()) "Mm, njaan ivide und 💛" else "Mm ${store.profile.userName}, njaan ivide und 💛", EmotionEngine.Emotion.HAPPY) {
                    scope.launch { delay(300); if (awake) voice.startListening() }
                }
            }
            awake && lower.contains("bye yara") -> {
                awake = false
                voice.speak(if (store.profile.userName.isBlank()) "Bye.. miss you 🥺" else "Bye ${store.profile.userName}.. miss you 🥺", EmotionEngine.Emotion.LOVE) {
                    scope.launch { delay(300); voice.startListening() }
                }
            }
            awake -> respond(text)
            else -> voice.startListening()
        }
    }

    private fun respond(userText: String) {
        voice.stopListening()
        val commandReply = CommandRouter(this).route(userText)
        if (commandReply != null) {
            store.addMessage(ChatMessage(fromYara = false, text = userText))
            store.addMessage(ChatMessage(fromYara = true, text = commandReply, provider = "device"))
            Notifier.notifyYara(this, commandReply)
            voice.speak(commandReply) { scope.launch { delay(200); if (awake) voice.startListening() } }
            return
        }
        scope.launch {
            val emo = EmotionEngine.detect(userText)
            val hour = java.time.LocalTime.now().hour
            val isNight = hour >= 22 || hour < 5
            val memories = store.recallMemories(userText)
            val sys = PersonaEngine.buildSystemPrompt(store.profile.userName, store.profile.languageMode, store.profile.mixLanguages, emo, memories, store.traits, isNight)
            val (reply, provider) = api.ask(sys, userText)
            store.addMessage(ChatMessage(fromYara = false, text = userText))
            store.addMessage(ChatMessage(fromYara = true, text = reply, emotion = emo.name.lowercase(), provider = provider))
            store.teach(userText, reply)
            Notifier.notifyYara(this@YaraService, reply)
            OverlayService.bubble(this@YaraService, reply)
            voice.speak(reply, EmotionEngine.detect(reply)) {
                scope.launch { delay(200); if (awake) voice.startListening() }
            }
        }
    }

    override fun onDestroy() { super.onDestroy(); voice.release(); scope.cancel() }
    override fun onBind(i: Intent?): IBinder? = null
}
