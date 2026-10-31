package com.yara.ai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.yara.ai.data.MemoryStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentityScreen(nav: NavController, store: MemoryStore) {
    var name by remember { mutableStateOf(store.profile.userName) }
    var language by remember { mutableStateOf(store.profile.languageMode) }
    var mix by remember { mutableStateOf(store.profile.mixLanguages) }
    fun save() { store.profile.userName = name.trim(); store.profile.languageMode = language; store.profile.mixLanguages = mix; store.saveProfile() }
    Scaffold(topBar = { TopAppBar(title = { Text("Identity & language") }, navigationIcon = { IconButton(onClick = { save(); nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } }) }) { pad ->
        Column(Modifier.padding(pad).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("What should Yara call you?", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(name, { name = it }, label = { Text("Your name") }, placeholder = { Text("Leave blank if you prefer") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Text("Reply language", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(language == "english", { language = "english" }); Text("English") }
            Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(language == "malayalam", { language = "malayalam" }); Text("Malayalam") }
            Row(verticalAlignment = Alignment.CenterVertically) { Switch(mix, { mix = it }); Text("Mix English & Malayalam") }
            Button(onClick = { save(); nav.popBackStack() }, modifier = Modifier.fillMaxWidth()) { Text("Save identity") }
        }
    }
}

@Composable
fun FirstRunNameDialog(store: MemoryStore, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("What should I call you?") }, text = { OutlinedTextField(name, { name = it }, label = { Text("Your name") }, singleLine = true) }, confirmButton = { TextButton(onClick = { store.profile.userName = name.trim(); store.saveProfile(); onDismiss() }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Later") } })
}
