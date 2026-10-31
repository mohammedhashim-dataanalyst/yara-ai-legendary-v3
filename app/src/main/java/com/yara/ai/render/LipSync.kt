package com.yara.ai.render
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
data class LipFrame(val jawOpen:Float,val mouthWide:Float)
object LipSync { fun forText(text:String,durationMs:Long):Flow<LipFrame>=flow { val chars=text.lowercase().filter{it.isLetter()||it==' '}; val vowels="aeiou"; val step=(durationMs/chars.length.coerceAtLeast(1)).coerceIn(45L,160L); for((i,ch) in chars.withIndex()){val vowel=vowels.contains(ch); val jaw=when{ch==' '->0f;ch=='m'||ch=='b'||ch=='p'->.08f;vowel->.65f;else->.3f}; val wide=when{vowel->.45f;ch=='m'||ch=='b'||ch=='p'->.05f;else->.25f};emit(LipFrame(jaw,wide));delay(step);emit(LipFrame(jaw*.25f,wide));delay(step/2)};emit(LipFrame(0f,0f))} }
