package com.yara.ai.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yara.ai.data.ApiManager
import com.yara.ai.data.MemoryStore
import com.yara.ai.overlay.OverlayService
import com.yara.ai.ambient.AmbientOverlayService
import com.yara.ai.ambient.OverlayState
import com.yara.ai.ui.screens.*
import com.yara.ai.ui.theme.YaraTheme
import com.yara.ai.voice.YaraService
import com.yara.ai.games.GameCenterScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var store: MemoryStore
    private lateinit var api: ApiManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = MemoryStore.get(this)
        api = ApiManager(store)
        setContent {
            YaraTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    YaraNav(store, api)
                }
            }
        }
        requestRuntimePermissions()
        startListener()
    }

    private fun startListener() {
        try {
            val i = Intent(this, YaraService::class.java)
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
        } catch (e: Exception) { }
    }

    private fun requestRuntimePermissions() {
        val needed = mutableListOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= 33) {
            needed += Manifest.permission.READ_MEDIA_IMAGES
            needed += Manifest.permission.READ_MEDIA_VIDEO
            needed += Manifest.permission.POST_NOTIFICATIONS
        } else needed += Manifest.permission.READ_EXTERNAL_STORAGE
        val missing = needed.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) requestPermissions(missing.toTypedArray(), 42)
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this))
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
    }

    override fun onResume() {
        super.onResume()
        OverlayService.isYaraForeground = true
        OverlayState.inApp = true
        OverlayService.hide(this)
        intent?.getStringExtra("quick_reply")?.let { pendingQuick = it }
    }

    override fun onPause() { super.onPause(); OverlayService.isYaraForeground = false
        OverlayState.inApp = false }

    companion object { @Volatile var pendingQuick: String? = null }
}

@Composable
fun YaraNav(store: MemoryStore, api: ApiManager) {
    val nav = rememberNavController()
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { store.load() }
        ready = true
    }
    if (!ready) { LoadingScreen(); return }
    NavHost(nav, startDestination = "chat") {
        composable("chat") { ChatScreen(nav, store, api) }
        composable("settings") { SettingsScreen(nav, store) }
        composable("api") { ApiKeysScreen(nav, store, api) }
        composable("memories") { MemoriesScreen(nav, store) }
        composable("dashboard") { DashboardScreen(nav, store) }
        composable("traits") { TraitsScreen(nav, store) }
        composable("about") { AboutScreen(nav) }
        composable("voice") { VoiceSettingsScreen(nav, store) }
        composable("identity") { IdentityScreen(nav, store) }
        composable("assistant") { AssistantScreen(nav, store) }
        composable("avatar") { AvatarScreen(nav, store) }
        composable("games") { GameCenterScreen() }
    }
}
