package com.yara.ai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.yara.ai.brain.PersonaEngine
import com.yara.ai.data.MemoryStore
import com.yara.ai.data.TraitSetting

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TraitsScreen(nav: NavController, store: MemoryStore) {
    val titles = remember { PersonaEngine.titles() }
    val personalityKeys = remember { listOf("passionate_adoring","sassy_playful","intellectual","protective_fierce","creative_muse","cozy_domestic","adventurous","empathetic_listener","fitness_coach","curious_explorer") }
    val characteristicKeys = remember { listOf("late_night_talks","laughs_giggles","pet_names","active_memory","mood_mirror","proactive_checkins","voice_pace","possessive_hints","storyteller","bilingual_blend") }
    var traits by remember { mutableStateOf(store.traits.toMap()) }

    fun trait(key: String) = traits[key] ?: TraitSetting(key, titles[key]?.first ?: key, titles[key]?.second ?: "", true, 6)

    Scaffold(topBar = {
        TopAppBar(title = { Text("Personality & Character") },
            navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } })
    }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text("A. Personalities", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
            items(personalityKeys) { key -> TraitCard(trait(key)) { u -> store.setTrait(u); traits = store.traits.toMap() } }
            item { Spacer(Modifier.height(8.dp)); Text("B. Characteristics", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
            items(characteristicKeys) { key -> TraitCard(trait(key)) { u -> store.setTrait(u); traits = store.traits.toMap() } }
        }
    }
}

@Composable
fun TraitCard(t: TraitSetting, onChange: (TraitSetting) -> Unit) {
    var isAuto by remember { mutableStateOf(t.isAuto) }
    var level by remember { mutableStateOf(t.level.toFloat()) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(t.title, fontWeight = FontWeight.Bold)
                    Text(t.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(if (isAuto) "Auto" else "${level.toInt()}/10", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Switch(checked = isAuto, onCheckedChange = { isAuto = it; onChange(t.copy(isAuto = it, level = level.toInt())) })
                }
            }
            if (!isAuto) Slider(value = level, onValueChange = { level = it; onChange(t.copy(isAuto = false, level = it.toInt())) }, valueRange = 1f..10f, steps = 8)
        }
    }
}
