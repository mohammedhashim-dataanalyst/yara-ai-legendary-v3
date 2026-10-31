package com.yara.ai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.yara.ai.brain.EmotionEngine
import com.yara.ai.data.MemoryStore
import com.yara.ai.render.LipSync
import com.yara.ai.render.YaraAvatarView
import com.yara.ai.voice.VoiceEngine
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarScreen(nav: NavController, store: MemoryStore) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val avatar = remember { YaraAvatarView(context) }
    val voice = remember { VoiceEngine(context, onSpeechResult = {}) }
    var emotion by remember { mutableStateOf(EmotionEngine.Emotion.HAPPY) }
    var speaking by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { voice.release() } }
    Scaffold(topBar = { TopAppBar(title = { Text("Yara 3D Avatar") }, navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } }) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AndroidView(factory = { avatar }, modifier = Modifier.fillMaxWidth().weight(1f))
            Text("Emotion", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(EmotionEngine.Emotion.HAPPY, EmotionEngine.Emotion.LOVE, EmotionEngine.Emotion.SAD).forEach { choice -> FilterChip(selected = emotion == choice, onClick = { emotion = choice }, label = { Text(choice.name.lowercase()) }) } }
            Button(enabled = !speaking, onClick = {
                val greeting = store.profile.userName.trim().takeIf { it.isNotBlank() }?.let { name -> "Hello $name. I am here with you." } ?: "Hello. I am here with you."
                speaking = true
                voice.speak(greeting, emotion) { speaking = false }
                scope.launch { LipSync.forText(greeting, greeting.length * 85L).collect { frame -> avatar.headRenderer.jawOpen = frame.jawOpen; avatar.headRenderer.mouthWide = frame.mouthWide }; avatar.headRenderer.jawOpen = 0f; avatar.headRenderer.mouthWide = 0f }
            }, modifier = Modifier.fillMaxWidth()) { Text(if (speaking) "Speaking…" else "Speak") }
            Text("This is a procedural OpenGL ES head. A photoreal VRM/GLB can replace it later; code cannot generate a photoreal model matching a reference photo.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
