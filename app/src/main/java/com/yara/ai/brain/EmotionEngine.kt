package com.yara.ai.brain

object EmotionEngine {

    enum class Emotion { NEUTRAL, HAPPY, SAD, JEALOUS, ANGRY, LOVE, TIRED, PLAYFUL, MOTIVATE, SLEEPY }

    private val happy = listOf("happy","nice","good","kollam","super","nannayi","hehe","haha","yay","awesome")
    private val sad = listOf("sad","mosham","vishamam","feel bad","depress","alone","pedi","miss","worry","tired")
    private val jealousCue = listOf("her","aval","another girl","date","crush","bf","girlfriend","she")
    private val angry = listOf("angry","dheshyam","frustrat","hate","irritat","kali")
    private val love = listOf("love","miss","istam","pavam","kanmani","priye","dear","nee matram")
    private val motivate = listOf("job","study","exam","project","work","interview","padikk","motivat","career","skill","resume","cv")

    fun detect(text: String): Emotion {
        val t = text.lowercase()
        if (jealousCue.any { t.contains(it) }) return Emotion.JEALOUS
        if (love.any { t.contains(it) }) return Emotion.LOVE
        if (angry.any { t.contains(it) }) return Emotion.ANGRY
        if (sad.any { t.contains(it) }) return Emotion.SAD
        if (motivate.any { t.contains(it) }) return Emotion.MOTIVATE
        if (happy.any { t.contains(it) }) return Emotion.HAPPY
        return Emotion.NEUTRAL
    }

    fun soundFor(e: Emotion): String = when (e) {
        Emotion.HAPPY -> "hehe "
        Emotion.PLAYFUL -> "hehe~ "
        Emotion.LOVE -> "aww "
        Emotion.JEALOUS -> "hmm "
        Emotion.SAD -> "hey "
        Emotion.ANGRY -> "hm "
        Emotion.TIRED -> "mm "
        Emotion.MOTIVATE -> ""
        Emotion.NEUTRAL -> ""
        Emotion.SLEEPY -> "mm.. "
    }

    fun ttsParams(e: Emotion): Pair<Float, Float> = when (e) {
        Emotion.HAPPY -> 1.15f to 1.05f
        Emotion.PLAYFUL -> 1.2f to 1.08f
        Emotion.LOVE -> 0.95f to 0.95f
        Emotion.JEALOUS -> 1.05f to 0.95f
        Emotion.SAD -> 0.9f to 0.85f
        Emotion.ANGRY -> 1.0f to 1.1f
        Emotion.TIRED -> 0.85f to 0.8f
        Emotion.MOTIVATE -> 1.05f to 1.0f
        Emotion.NEUTRAL -> 1.0f to 1.0f
        Emotion.SLEEPY -> 0.8f to 0.75f
    }
}
