package com.yara.ai.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.yara.ai.ui.theme.YaraTheme

class VideoCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { YaraTheme { VideoCallScreen(this) { finish() } } }
    }
}

@Composable
fun VideoCallScreen(owner: LifecycleOwner, onEnd: () -> Unit) {
    var muted by remember { mutableStateOf(false) }
    var camOn by remember { mutableStateOf(true) }

    Box(Modifier.fillMaxSize()) {
        if (camOn) {
            AndroidView(factory = { c ->
                PreviewView(c).apply {
                    val future = ProcessCameraProvider.getInstance(c)
                    future.addListener({
                        try {
                            val provider = future.get()
                            val preview = Preview.Builder().build().also { it.setSurfaceProvider(surfaceProvider) }
                            provider.unbindAll()
                            provider.bindToLifecycle(owner, CameraSelector.DEFAULT_FRONT_CAMERA, preview)
                        } catch (e: Exception) { }
                    }, ContextCompat.getMainExecutor(c))
                }
            }, modifier = Modifier.fillMaxSize())
        } else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Camera off", color = Color.White) }

        Row(Modifier.align(Alignment.BottomCenter).padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledIconButton(onClick = { muted = !muted }, modifier = Modifier.size(56.dp)) { Icon(if (muted) Icons.Default.MicOff else Icons.Default.Mic, null) }
            FilledIconButton(onClick = onEnd, colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFE53935)), modifier = Modifier.size(64.dp)) {
                Icon(Icons.Default.CallEnd, "end", tint = Color.White)
            }
            FilledIconButton(onClick = { camOn = !camOn }, modifier = Modifier.size(56.dp)) { Icon(if (camOn) Icons.Default.Videocam else Icons.Default.VideocamOff, null) }
        }
    }
}
