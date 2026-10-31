package com.yara.ai.render

import com.yara.ai.brain.EmotionEngine
import com.yara.ai.voice.VoiceEngine

data class AvatarEmotionParams(val brow: Float, val smile: Float, val blink: Float, val jaw: Float)
object EmotionAvatarMapper {
    fun params(emotion: EmotionEngine.Emotion): AvatarEmotionParams = when (emotion) {
        EmotionEngine.Emotion.HAPPY, EmotionEngine.Emotion.PLAYFUL -> AvatarEmotionParams(.8f, 1f, 0f, .12f)
        EmotionEngine.Emotion.LOVE -> AvatarEmotionParams(.35f, .9f, .05f, .08f)
        EmotionEngine.Emotion.SAD, EmotionEngine.Emotion.SLEEPY -> AvatarEmotionParams(-.7f, 0f, .2f, .02f)
        EmotionEngine.Emotion.ANGRY -> AvatarEmotionParams(-1f, 0f, 0f, .1f)
        EmotionEngine.Emotion.JEALOUS -> AvatarEmotionParams(.5f, .25f, .1f, .05f)
        EmotionEngine.Emotion.MOTIVATE -> AvatarEmotionParams(.6f, .65f, 0f, .1f)
        EmotionEngine.Emotion.TIRED -> AvatarEmotionParams(-.4f, .1f, .45f, .02f)
        EmotionEngine.Emotion.NEUTRAL -> AvatarEmotionParams(0f, 0f, 0f, .04f)
    }
    fun apply(renderer: YaraHeadRenderer, emotion: EmotionEngine.Emotion) {
        val p = params(emotion); renderer.brow = p.brow; renderer.smile = p.smile; renderer.blink = p.blink; renderer.jawOpen = p.jaw
    }
}
