package com.yara.ai.proactive

import android.content.Context
import androidx.work.*
import com.yara.ai.data.MemoryStore
import com.yara.ai.features.FeatureRuntime
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ProactiveEngine {
    private const val PERIODIC = "yara_proactive_periodic"
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<ProactiveWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build()).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
        scheduleWindow(context, "yara_proactive_next_morning", nextWindowDelay(8))
        scheduleWindow(context, "yara_proactive_next_night", nextWindowDelay(22))
    }
    private fun scheduleWindow(context: Context, name: String, delay: Long) {
        val request = OneTimeWorkRequestBuilder<ProactiveWorker>().setInitialDelay(delay, TimeUnit.MILLISECONDS).build()
        WorkManager.getInstance(context).enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, request)
    }
    private fun nextWindowDelay(targetHour: Int): Long {
        val now = Calendar.getInstance(); val next = now.clone() as Calendar
        next.set(Calendar.HOUR_OF_DAY, targetHour); next.set(Calendar.MINUTE, 0); next.set(Calendar.SECOND, 0); next.set(Calendar.MILLISECOND, 0)
        if (next.timeInMillis <= now.timeInMillis) next.add(Calendar.DAY_OF_YEAR, 1)
        return (next.timeInMillis - now.timeInMillis).coerceAtLeast(60_000L)
    }
}

class ProactiveWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val store = MemoryStore.get(applicationContext); store.load()
        if (!FeatureRuntime.on(store, "proactive_checkins")) return Result.success()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val morning = hour in 5..11
        val night = hour >= 20 || hour < 5
        val allowed = (morning && FeatureRuntime.on(store, "morning_greet")) || (night && FeatureRuntime.on(store, "night_greet"))
        if (!allowed || System.currentTimeMillis() - store.profile.lastSentAt < 12 * 60 * 60 * 1000L) return Result.success()
        val name = store.profile.userName.trim().ifBlank { "there" }
        val line = if (morning) "Good morning, $name. ${if (FeatureRuntime.on(store, "ask_about_day")) "How is your day starting?" else "I am here if you need me."}" else "Good night, $name. Rest well."
        NotifierPoster.post(applicationContext, line, FeatureRuntime.on(store, "background_talk"))
        store.profile.lastSentAt = System.currentTimeMillis(); store.saveProfile()
        return Result.success()
    }
}
