package com.yara.ai.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(nav: NavController) {
    val ctx = LocalContext.current
    Scaffold(topBar = {
        TopAppBar(title = { Text("About") },
            navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) } })
    }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(Icons.Default.Favorite, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
            Text("Yara", fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Text("Version 6.0.0")
            Text("Your bilingual Malayalam + English AI companion", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider()
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Made by", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Your local profile", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://yara.ai"))) }) {
                        Text("Yara project information")
                    }
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("What's inside", fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp))
                    Text("• Real-time voice talk with 'hey yara' / 'bye yara'")
                    Text("• Multi-API brain: Gemini · Grok · OpenRouter · Hugging Face")
                    Text("• Auto-switching + local offline brain")
                    Text("• Unlimited memories + emotion engine")
                    Text("• Floating overlay + quick-reply notifications")
                    Text("• Video call & screen sharing")
                    Text("• 132 feature toggles, personality sliders 1-10")
                }
            }
        }
    }
}
