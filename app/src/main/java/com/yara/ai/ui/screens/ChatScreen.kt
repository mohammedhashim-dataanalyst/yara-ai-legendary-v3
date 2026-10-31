package com.yara.ai.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.yara.ai.brain.EmotionEngine
import com.yara.ai.brain.PersonaEngine
import com.yara.ai.data.ApiManager
import com.yara.ai.data.ChatMessage
import com.yara.ai.data.MemoryStore
import com.yara.ai.device.CommandRouter
import com.yara.ai.ui.ScreenShareActivity
import com.yara.ai.ui.VideoCallActivity
import com.yara.ai.voice.VoiceEngine
import com.yara.ai.voice.VoiceNoteRecorder
import com.yara.ai.reminders.ReminderManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(nav: NavController, store: MemoryStore, api: ApiManager) {
    val ctx = LocalContext.current
    var input by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(store.history.toList()) }
    var attachOpen by remember { mutableStateOf(false) }
    var listening by remember { mutableStateOf(false) }
    var yaraTyping by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(store.profile.userName.isBlank()) }
    val noteRecorder = remember { VoiceNoteRecorder(ctx) }
    var recording by remember { mutableStateOf(false) }

    val voice = remember {
        VoiceEngine(ctx,
            onSpeechResult = { text ->
                listening = false; input = ""
                send(ctx, text, store, api) { messages = store.history.toList(); yaraTyping = it }
            },
            onListeningState = { listening = it })
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { send(ctx, "📷 [photo]", store, api) { messages = store.history.toList(); yaraTyping = it } }
    }
    val pickDoc = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { send(ctx, "📄 [document]", store, api) { messages = store.history.toList(); yaraTyping = it } }
    }

    LaunchedEffect(Unit) {
        com.yara.ai.ui.MainActivity.pendingQuick?.let {
            send(ctx, it, store, api) { m -> messages = store.history.toList(); yaraTyping = m }
            com.yara.ai.ui.MainActivity.pendingQuick = null
        }
    }

    if (showNameDialog) FirstRunNameDialog(store) { showNameDialog = false }

    Scaffold(topBar = {
        TopAppBar(
            title = { Column {
                Text("Yara 💛", fontWeight = FontWeight.Bold)
                Text(if (yaraTyping) "typing…" else "online", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }},
            navigationIcon = { Icon(Icons.Default.Favorite, null, tint = Color(0xFFFF4D8D)) },
            actions = {
                IconButton(onClick = { nav.navigate("avatar") }) { Icon(Icons.Default.Face, "avatar") }
                IconButton(onClick = { nav.navigate("voice") }) { Icon(Icons.Default.RecordVoiceOver, "voice") }
                IconButton(onClick = { nav.navigate("memories") }) { Icon(Icons.Default.Psychology, "memories") }
                IconButton(onClick = { nav.navigate("games") }) { Icon(Icons.Default.SportsEsports, "games") }
                IconButton(onClick = { nav.navigate("settings") }) { Icon(Icons.Default.Settings, "settings") }
            }
        )
    }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(messages) { m -> MessageBubble(m) }
                if (yaraTyping) item { TypingBubble() }
            }
            if (attachOpen) {
                AttachmentSheet(
                    onDismiss = { attachOpen = false },
                    onImage = { pickImage.launch("image/*") },
                    onDoc = { pickDoc.launch(arrayOf("*/*")) },
                    onCamera = { ctx.startActivity(Intent(ctx, VideoCallActivity::class.java)) },
                    onScreen = { ctx.startActivity(Intent(ctx, ScreenShareActivity::class.java)) },
                    onVideo = { ctx.startActivity(Intent(ctx, VideoCallActivity::class.java)) },
                    onPoll = { input = "[poll] " },
                    onLocation = { input = "[location] " },
                    onSticker = { input = "[sticker] 😘" }
                )
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("companion" to "Companion", "analytics_tutor" to "Analytics tutor", "coding_helper" to "Coding helper", "translator" to "Translator").forEach { pair ->
                    FilterChip(selected = store.profile.personaMode == pair.first, onClick = { store.profile.personaMode = pair.first; store.saveProfile() }, label = { Text(pair.second) })
                }
            }
            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { attachOpen = true }) { Icon(Icons.Default.Add, "attach") }
                IconButton(onClick = { if (recording) { noteRecorder.stop(); recording = false } else { recording = noteRecorder.start() != null } }) { Icon(if (recording) Icons.Default.Stop else Icons.Default.Mic, "voice note") }
                IconButton(onClick = { ctx.startActivity(Intent(ctx, ScreenShareActivity::class.java)) }) { Icon(Icons.Default.ScreenShare, "screen") }
                IconButton(onClick = { ctx.startActivity(Intent(ctx, VideoCallActivity::class.java)) }) { Icon(Icons.Default.Videocam, "video") }
                OutlinedTextField(value = input, onValueChange = { input = it },
                    modifier = Modifier.weight(1f), placeholder = { Text("Message Yara…") },
                    shape = RoundedCornerShape(24.dp), singleLine = false)
                Spacer(Modifier.width(6.dp))
                FilledIconButton(onClick = {
                    if (input.isBlank()) { if (listening) voice.stopListening() else voice.startListening() }
                    else { val t = input; input = ""; send(ctx, t, store, api) { messages = store.history.toList(); yaraTyping = it } }
                }, modifier = Modifier.clip(CircleShape)) {
                    Icon(if (input.isBlank()) (if (listening) Icons.Default.MicOff else Icons.Default.Mic) else Icons.Default.Send, null)
                }
            }
        }
    }
}

private fun send(ctx: android.content.Context, text: String, store: MemoryStore, api: ApiManager, onUpdate: (Boolean) -> Unit) {
    CoroutineScope(Dispatchers.Main).launch {
        onUpdate(true)
        if (ReminderManager.parseAndSchedule(ctx, text)) {
            store.addMessage(ChatMessage(fromYara = false, text = text))
            store.addMessage(ChatMessage(fromYara = true, text = "Reminder scheduled.", provider = "local"))
            onUpdate(false); return@launch
        }
        val commandReply = CommandRouter(ctx).route(text)
        if (commandReply != null) {
            store.addMessage(ChatMessage(fromYara = false, text = text))
            store.addMessage(ChatMessage(fromYara = true, text = commandReply, provider = "device"))
            onUpdate(false)
            return@launch
        }
        val emo = EmotionEngine.detect(text)
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isNight = hour >= 22 || hour < 5
        val memories = store.recallMemories(text)
        val sys = PersonaEngine.buildSystemPrompt(store.profile.userName, store.profile.languageMode, store.profile.mixLanguages, emo, memories, store.traits, isNight, store.profile.personaMode)
        val (reply, provider) = api.ask(sys, text)
        store.addMessage(ChatMessage(fromYara = false, text = text))
        store.addMessage(ChatMessage(fromYara = true, text = reply, emotion = emo.name.lowercase(), provider = provider))
        store.teach(text, reply)
        onUpdate(false)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(m: ChatMessage) {
    val ctx = LocalContext.current
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.fromYara) Arrangement.Start else Arrangement.End) {
        Surface(modifier = Modifier.combinedClickable(onClick = {}, onLongClick = { clipboard.setText(androidx.compose.ui.text.AnnotatedString(m.text)) }), color = if (m.fromYara) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(18.dp, if (m.fromYara) 4.dp else 18.dp, 18.dp, if (m.fromYara) 18.dp else 4.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text(m.text, color = Color.White)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (m.emotion.isNotBlank() && m.emotion != "neutral") Text(m.emotion, fontSize = 10.sp, color = Color(0xAAFFFFFF))
                    if (m.edited) Text(" edited", fontSize = 10.sp, color = Color(0x88FFFFFF))
                }
            }
        }
    }
}

@Composable
fun TypingBubble() {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(18.dp)) {
        Text("Yara is typing…", modifier = Modifier.padding(12.dp), fontSize = 13.sp)
    }
}

@Composable
fun AttachmentSheet(onDismiss: () -> Unit, onImage: () -> Unit, onDoc: () -> Unit, onCamera: () -> Unit,
    onScreen: () -> Unit, onVideo: () -> Unit, onPoll: () -> Unit, onLocation: () -> Unit, onSticker: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), tonalElevation = 6.dp, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Share with Yara", fontWeight = FontWeight.Bold)
                TextButton(onClick = onDismiss) { Text("Close") }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SheetItem(Icons.Default.Image, "Gallery", onImage)
                SheetItem(Icons.Default.PhotoCamera, "Camera", onCamera)
                SheetItem(Icons.Default.Description, "Docs", onDoc)
                SheetItem(Icons.Default.Poll, "Poll", onPoll)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SheetItem(Icons.Default.ScreenShare, "Screen", onScreen)
                SheetItem(Icons.Default.VideoCall, "Video", onVideo)
                SheetItem(Icons.Default.LocationOn, "Location", onLocation)
                SheetItem(Icons.Default.EmojiEmotions, "Sticker", onSticker)
            }
        }
    }
}

@Composable
fun SheetItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(onClick = onClick, modifier = Modifier.size(56.dp)) { Icon(icon, label) }
        Text(label, fontSize = 11.sp)
    }
}
