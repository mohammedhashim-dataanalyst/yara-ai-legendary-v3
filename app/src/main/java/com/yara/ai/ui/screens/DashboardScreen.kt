package com.yara.ai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.yara.ai.data.MemoryStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(nav: NavController, store: MemoryStore) {
    val history = store.history
    val providersUsed = history.groupingBy { it.provider }.eachCount().filterKeys { it.isNotBlank() }
    val emotions = history.groupingBy { it.emotion }.eachCount()

    Scaffold(topBar = {
        TopAppBar(title = { Text("Analytics Dashboard") },
            navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } })
    }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatCard("Messages", history.size)
                StatCard("From Yara", history.count { it.fromYara })
                StatCard("Memories", store.memories.size)
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("Provider usage", fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp))
                    if (providersUsed.isEmpty()) Text("No data yet") else providersUsed.forEach { (k, v) -> Text("$k — $v replies") }
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("Emotion breakdown", fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp))
                    if (emotions.isEmpty()) Text("No data yet") else emotions.forEach { (k, v) -> Text("$k — $v") }
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("API provider health", fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp))
                    store.providers.forEach { Text("${it.name}: ${if (it.apiKey.isNotBlank()) "key set" else "no key"} · ${it.lastLatencyMs}ms · failures ${it.failures}") }
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: Int) {
    Card { Column(Modifier.padding(16.dp)) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("$value", fontSize = 26.sp, fontWeight = FontWeight.Bold)
    }}
}
