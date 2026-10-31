package com.yara.ai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.yara.ai.data.ApiManager
import com.yara.ai.data.MemoryStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeysScreen(nav: NavController, store: MemoryStore, api: ApiManager) {
    val scope = rememberCoroutineScope()
    val providers = store.providers
    var primary by remember { mutableStateOf(api.manualPrimary) }
    var adaptive by remember { mutableStateOf(api.autoAdaptive) }
    Scaffold(topBar = { TopAppBar(title = { Text("AI Providers") }, navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } }) }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) {
                    Text("Adaptive provider cascade", fontWeight = FontWeight.Bold)
                    Text("Yara ranks healthy providers by latency, skips 60-second rate limits, then uses local brain and persona fallback.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) { Switch(adaptive, { adaptive = it; api.autoAdaptive = it; store.setFeature("api_autoswitch", it) }); Text("Auto adaptive") }
                    Text("Set as main", fontWeight = FontWeight.Medium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(primary.isBlank(), { primary = ""; api.manualPrimary = "" })
                        Text("Auto")
                        providers.forEach { provider ->
                            RadioButton(primary.equals(provider.name, true), { primary = provider.name; api.manualPrimary = provider.name })
                            Text(provider.name, fontSize = 12.sp)
                        }
                    }
                } }
            }
            items(providers) { provider ->
                var key by remember(provider.name) { mutableStateOf(provider.apiKey) }
                var model by remember(provider.name) { mutableStateOf(provider.model ?: "") }
                var baseUrl by remember(provider.name) { mutableStateOf(provider.baseUrl ?: "") }
                var enabled by remember(provider.name) { mutableStateOf(provider.enabled) }
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(provider.name, fontWeight = FontWeight.Bold); Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.CheckCircle, null, tint = if (key.isNotBlank()) Color(0xFF4CAF50) else Color.Gray, modifier = Modifier.size(12.dp)); Spacer(Modifier.width(4.dp)); Text(if (key.isNotBlank()) "key set · ${provider.lastLatencyMs}ms" else "no key", fontSize = 11.sp) } }
                        Switch(enabled, { enabled = it; provider.enabled = it; store.saveProviders() })
                    }
                    OutlinedTextField(key, { key = it; provider.apiKey = it; store.saveProviders() }, label = { Text("API key") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(model, { model = it; provider.model = it; store.saveProviders() }, label = { Text("Model name (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(baseUrl, { baseUrl = it; provider.baseUrl = it; store.saveProviders() }, label = { Text("Custom base URL (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                } }
            }
        }
    }
}
