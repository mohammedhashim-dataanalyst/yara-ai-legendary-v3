package com.yara.ai.proactive

import android.content.Context
import android.speech.tts.TextToSpeech
import com.yara.ai.notify.Notifier
import java.util.Locale

object NotifierPoster {
    fun post(context: Context, text: String, speak: Boolean) {
        Notifier.notifyYara(context, text)
        if (speak) {
            var engine: TextToSpeech? = null
            engine = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    engine?.language = Locale("en", "IN")
                    engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "proactive_${System.currentTimeMillis()}")
                }
            }
        }
    }
}
