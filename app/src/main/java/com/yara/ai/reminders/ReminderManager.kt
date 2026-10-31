package com.yara.ai.reminders

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.work.*
import com.yara.ai.notify.Notifier
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

data class LocalReminder(val text: String, val at: Long)

object ReminderManager {
    private const val PREFIX = "yara_reminder_"

    fun parseAndSchedule(ctx: Context, input: String): Boolean {
        val m = Regex("(?i)remind me to (.+) at (\\d{1,2}):(\\d{2})").find(input) ?: return false
        val h = m.groupValues[2].toIntOrNull() ?: return false
        val min = m.groupValues[3].toIntOrNull() ?: return false
        if (h !in 0..23 || min !in 0..59) return false
        val now = Calendar.getInstance()
        val at = now.clone() as Calendar
        at.set(Calendar.HOUR_OF_DAY, h); at.set(Calendar.MINUTE, min); at.set(Calendar.SECOND, 0)
        if (at.timeInMillis <= now.timeInMillis) at.add(Calendar.DAY_OF_YEAR, 1)
        val text = m.groupValues[1].trim()
        val data = workDataOf("text" to text, "at" to at.timeInMillis)
        val req = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(at.timeInMillis - now.timeInMillis, TimeUnit.MILLISECONDS)
            .setInputData(data).build()
        WorkManager.getInstance(ctx).enqueueUniqueWork(PREFIX + at.timeInMillis, ExistingWorkPolicy.REPLACE, req)
        return true
    }
}

class ReminderWorker(ctx: Context, p: WorkerParameters) : CoroutineWorker(ctx, p) {
    override suspend fun doWork(): Result {
        val text = inputData.getString("text") ?: return Result.failure()
        Notifier.notifyYara(applicationContext, "Reminder: $text")
        // Speak the reminder; wait briefly for TTS init before the worker exits.
        val latch = CountDownLatch(1)
        var tts: TextToSpeech? = null
        try {
            tts = TextToSpeech(applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) latch.countDown()
            }
            latch.await(1500, TimeUnit.MILLISECONDS)
            tts?.speak("Reminder: $text", TextToSpeech.QUEUE_FLUSH, null, "reminder_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            // TTS unavailable — notification already shown
        } finally {
            try { tts?.shutdown() } catch (_: Exception) {}
        }
        return Result.success()
    }
}
