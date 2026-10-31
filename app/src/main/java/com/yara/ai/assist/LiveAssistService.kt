package com.yara.ai.assist

import android.app.*
import android.content.*
import android.os.*
import androidx.core.app.NotificationCompat
import com.yara.ai.YaraApp
import com.yara.ai.data.*
import com.yara.ai.device.YaraAccessibilityService
import com.yara.ai.brain.PersonaEngine
import com.yara.ai.voice.VoiceEngine
import kotlinx.coroutines.*

class LiveAssistService:Service(){
 private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main);private lateinit var voice:VoiceEngine;private lateinit var store:MemoryStore;private lateinit var api:ApiManager;private var active=false
 override fun onCreate(){super.onCreate();store=MemoryStore.get(this);store.load();api=ApiManager(store);voice=VoiceEngine(this,onSpeechResult={if(active)answer(it)});startForeground(77,NotificationCompat.Builder(this,YaraApp.CHANNEL_SERVICE).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Yara live assist").setContentText("Ask Yara about the screen").build());active=true;voice.startListening();scope.launch{while(isActive){delay(30000); if(active) YaraAccessibilityService.instance?.readScreen()?.take(2000)}}}
 private fun answer(q:String){voice.stopListening();scope.launch{val screen=YaraAccessibilityService.instance?.readScreen().orEmpty();val prompt=PersonaEngine.buildSystemPrompt(store.profile.userName,store.profile.languageMode,store.profile.mixLanguages,com.yara.ai.brain.EmotionEngine.Emotion.NEUTRAL,emptyList(),store.traits,false)+"\nScreen text context:\n"+screen;val(reply,_)=api.ask(prompt,q);voice.speak(reply){if(active)voice.startListening()}}}
 override fun onDestroy(){active=false;voice.release();scope.cancel();super.onDestroy()};override fun onBind(i:Intent?):IBinder?=null
}
