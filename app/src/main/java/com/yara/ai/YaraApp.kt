package com.yara.ai

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.yara.ai.proactive.ProactiveEngine
import com.yara.ai.data.MemoryStore
import com.yara.ai.brain.SelfLearning

class YaraApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val store = MemoryStore.get(this)
        store.load()
        ProactiveEngine.schedule(this)
        SelfLearning.scan(store)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val chat = NotificationChannel(CHANNEL_CHAT, getString(R.string.channel_name), NotificationManager.IMPORTANCE_HIGH)
                .apply { description = getString(R.string.channel_desc) }
            val service = NotificationChannel(CHANNEL_SERVICE, "Yara background listener", NotificationManager.IMPORTANCE_MIN)
            nm.createNotificationChannels(listOf(chat, service))
        }
    }
    companion object {
        const val CHANNEL_CHAT = "yara_chat"
        const val CHANNEL_SERVICE = "yara_service"
    }
}
