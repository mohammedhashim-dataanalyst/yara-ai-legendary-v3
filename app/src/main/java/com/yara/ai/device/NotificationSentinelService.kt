package com.yara.ai.device

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import java.util.Locale

data class RecentNotification(val app: String, val title: String, val text: String, val timestamp: Long = System.currentTimeMillis())

class NotificationSentinelService : NotificationListenerService() {
    private var tts: TextToSpeech? = null
    override fun onCreate() { super.onCreate(); instance = this; tts = TextToSpeech(this) { status -> if (status == TextToSpeech.SUCCESS) tts?.setLanguage(Locale.getDefault()) } }
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val item = RecentNotification(sbn.packageName, extras.getString("android.title", ""), extras.getCharSequence("android.text")?.toString() ?: "")
        synchronized(recent) { recent.add(0, item); while (recent.size > 100) recent.removeLast() }
        if (vipEnabled && (item.title + item.text).contains(Regex("vip|urgent|important|critical", RegexOption.IGNORE_CASE))) tts?.speak("VIP alert from ${item.app}: ${item.title}", TextToSpeech.QUEUE_ADD, null, "vip")
    }
    override fun onNotificationRemoved(sbn: StatusBarNotification) {}
    override fun onDestroy() { tts?.shutdown(); instance = null; super.onDestroy() }
    companion object { val recent: MutableList<RecentNotification> = mutableListOf(); @Volatile var vipEnabled = false; @Volatile var instance: NotificationSentinelService? = null }
}
