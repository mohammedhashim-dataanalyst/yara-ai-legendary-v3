package com.yara.ai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.yara.ai.data.MemoryStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoriesScreen(nav: NavController, store: MemoryStore) {
    var memories by remember { mutableStateOf(store.memories.toList()) }
    var showAdd by remember { mutableStateOf(false) }
    var newText by remember { mutableStateOf("") }
    var importance by remember { mutableStateOf(6f) }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Memories (${memories.size})") },
            navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } },
            actions = { IconButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, null) } })
    }) { pad ->
        Column(Modifier.padding(pad)) {
            if (showAdd) {
                Card(Modifier.fillMaxWidth().padding(12.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Add a memory", fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = newText, onValueChange = { newText = it },
                            modifier = Modifier.fillMaxWidth(), placeholder = { Text("e.g. His birthday is 15 Aug") })
                        Spacer(Modifier.height(8.dp))
                        Text("Importance: ${importance.toInt()}/10", fontSize = 12.sp)
                        Slider(value = importance, onValueChange = { importance = it }, valueRange = 1f..10f, steps = 8)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showAdd = false; newText = "" }) { Text("Cancel") }
                            Button(onClick = {
                                store.addMemory(newText, importance.toInt())
                                memories = store.memories.toList(); newText = ""; showAdd = false
                            }) { Text("Save") }
                        }
                    }
                }
            }
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(memories.sortedByDescending { it.importance }) { m ->
                    Surface(tonalElevation = 2.dp, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(m.text)
                                Text("importance ${m.importance} · ${m.source}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { store.deleteMemory(m.id); memories = store.memories.toList() }) {
                                Icon(Icons.Default.Delete, "delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
