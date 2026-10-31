package com.yara.ai.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.yara.ai.data.BackupManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.yara.ai.data.MemoryStore
import com.yara.ai.device.DeviceController
import com.yara.ai.device.CommandRouter
import com.yara.ai.ambient.AmbientOverlayService
import com.yara.ai.overlay.OverlayService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(nav: NavController, store: MemoryStore) {
    val context = LocalContext.current
    val device = remember { DeviceController(context) }
    val router = remember { CommandRouter(context, device) }
    var status by remember { mutableStateOf("Ready") }
    var haUrl by remember { mutableStateOf(store.profile.homeAssistantUrl) }
    var haToken by remember { mutableStateOf(store.profile.homeAssistantToken) }
    var gateway by remember { mutableStateOf(store.profile.pcGatewayUrl) }
    var orbOn by remember { mutableStateOf(store.isFeatureOn("floating_orb")) }
    var vipOn by remember { mutableStateOf(store.isFeatureOn("vip_alerts")) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let { context.contentResolver.openOutputStream(it)?.use { out -> BackupManager.write(context, out, store); status = "Backup exported." } } }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { context.contentResolver.openInputStream(it)?.use { input -> BackupManager.restore(context, input, store); status = "Backup restored." } } }
    fun saveIntegration() { store.profile.homeAssistantUrl = haUrl; store.profile.homeAssistantToken = haToken; store.profile.pcGatewayUrl = gateway; store.saveProfile() }
    Scaffold(topBar = { TopAppBar(title = { Text("Assistant & Device") }, navigationIcon = { IconButton(onClick = { saveIntegration(); nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } }) }) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("System integration", fontWeight = FontWeight.Bold)
            Button(onClick = { context.startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)) }, modifier = Modifier.fillMaxWidth()) { Text("Set default assistant") }
            OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }, modifier = Modifier.fillMaxWidth()) { Text("Notification sentinel access") }
            OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }, modifier = Modifier.fillMaxWidth()) { Text("Accessibility service access") }
            Row(verticalAlignment = Alignment.CenterVertically) { Switch(orbOn, { orbOn = it; store.setFeature("floating_orb", it); if (it) AmbientOverlayService.start(context) else AmbientOverlayService.stop(context) }); Text("Floating holographic orb") }
            Row(verticalAlignment = Alignment.CenterVertically) { Switch(vipOn, { vipOn = it; store.setFeature("vip_alerts", it); com.yara.ai.device.NotificationSentinelService.vipEnabled = it }); Text("VIP notification spoken alerts") }
            HorizontalDivider()
            Text("Device controls", fontWeight = FontWeight.Bold)
            Text("Supported commands: ${router.supportedCommands().sorted().joinToString(", ")}", fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = { status = device.flashlight(true) }) { Text("Torch on") }; OutlinedButton(onClick = { status = device.flashlight(false) }) { Text("Torch off") } }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = { status = device.setVolume(50) }) { Text("Volume 50%") }; OutlinedButton(onClick = { status = device.battery() }) { Text("Battery") } }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = { status = device.setBrightness(60) }) { Text("Brightness 60%") }; OutlinedButton(onClick = { status = device.setRinger(true) }) { Text("Silent") } }
            OutlinedButton(onClick = { status = router.route("battery") ?: "Unavailable" }) { Text("Run battery command") }
            Text(status, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider()
            Text("Smart home (saved only)", fontWeight = FontWeight.Bold)
            Text("Home Assistant connection is stored locally; no network call is made yet.", fontSize = 12.sp)
            OutlinedTextField(haUrl, { haUrl = it }, label = { Text("Home Assistant URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(haToken, { haToken = it }, label = { Text("Long-lived access token") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Text("PC gateway (saved only)", fontWeight = FontWeight.Bold)
            OutlinedTextField(gateway, { gateway = it }, label = { Text("WebSocket URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Button(onClick = { saveIntegration(); status = "Integration settings saved locally." }, modifier = Modifier.fillMaxWidth()) { Text("Save integration settings") }
            HorizontalDivider()
            Text("Backup and restore", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = { export.launch("yara-backup.json") }) { Text("Export JSON") }; OutlinedButton(onClick = { import.launch(arrayOf("application/json", "text/*")) }) { Text("Import JSON") } }
            Text("Model engine", fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = { nav.navigate("api") }, modifier = Modifier.fillMaxWidth()) { Text("Choose provider, model and base URL") }
        }
    }
}
