package com.yara.ai.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.yara.ai.brain.EmotionEngine
import java.util.Locale

class VoiceEngine(
    ctx: Context,
    private val onSpeechResult: (String) -> Unit,
    private val onListeningState: (Boolean) -> Unit = {}
) {
    private val app = ctx.applicationContext
    private var tts: TextToSpeech? = null
    private var recognizer: SpeechRecognizer? = null
    private var ttsReady = false

    var language: Locale = Locale("en", "IN")
        set(v) { field = v; tts?.setLanguage(v) }

    init {
        tts = TextToSpeech(app) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) tts?.setLanguage(language)
        }
    }

    fun speak(text: String, emotion: EmotionEngine.Emotion = EmotionEngine.Emotion.NEUTRAL, onDone: () -> Unit = {}) {
        if (!ttsReady) { onDone(); return }
        val (pitch, rate) = EmotionEngine.ttsParams(emotion)
        tts?.setPitch(pitch); tts?.setSpeechRate(rate)
        val id = "yara_${System.currentTimeMillis()}"
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) { onDone() }
            @Deprecated("deprecated") override fun onError(id: String?) { onDone() }
        })
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
    }

    fun stopSpeaking() { tts?.stop() }

    fun startListening() {
        try {
            if (!SpeechRecognizer.isRecognitionAvailable(app)) return
            recognizer?.destroy()
            recognizer = SpeechRecognizer.createSpeechRecognizer(app).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(p: Bundle?) { onListeningState(true) }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(v: Float) {}
                    override fun onBufferReceived(b: ByteArray?) {}
                    override fun onEndOfSpeech() { onListeningState(false) }
                    override fun onError(e: Int) {
                        onListeningState(false)
                        if (e == SpeechRecognizer.ERROR_NO_MATCH || e == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) startListening()
                    }
                    override fun onResults(results: Bundle?) {
                        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                        if (!text.isNullOrBlank()) onSpeechResult(text)
                    }
                    override fun onPartialResults(p: Bundle?) {}
                    override fun onEvent(t: Int, p: Bundle?) {}
                })
            }
            val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            recognizer?.startListening(i)
        } catch (e: Exception) { Log.w("VoiceEngine", "listen error ${e.message}") }
    }

    fun stopListening() { try { recognizer?.stopListening() } catch (_: Exception) {} }
    fun release() {
        try { recognizer?.destroy() } catch (_: Exception) {}
        try { tts?.shutdown() } catch (_: Exception) {}
    }
}
