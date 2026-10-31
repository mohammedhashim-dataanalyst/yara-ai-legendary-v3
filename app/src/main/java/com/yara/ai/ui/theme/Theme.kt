package com.yara.ai.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Dark = darkColorScheme(
    primary = Color(0xFFFF4D8D),
    onPrimary = Color.White,
    secondary = Color(0xFFBB86FC),
    background = Color(0xFF0A0A0F),
    surface = Color(0xFF14141C),
    onBackground = Color(0xFFE8E8F0),
    onSurface = Color(0xFFE8E8F0),
    surfaceVariant = Color(0xFF1E1E2A),
    onSurfaceVariant = Color(0xFFC8C8D8)
)

@Composable
fun YaraTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Dark, typography = Typography(), content = content)
}
