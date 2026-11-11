package com.aramalr.messages.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val FallbackDark =
    darkColorScheme(
        background = Color.Black,
        surface = Color(0xFF1A1A1A),
        surfaceVariant = Color(0xFF2A2A2A),
        onBackground = Color.White,
        onSurface = Color.White,
        onSurfaceVariant = Color(0xFFB0B0B0),
    )

private val FallbackLight =
    lightColorScheme(
        background = Color(0xFFF2F2F2),
        surface = Color.White,
        surfaceVariant = Color(0xFFE8E8E8),
    )

@Composable
fun AramalrTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme =
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark ->
                dynamicDarkColorScheme(context).copy(background = Color.Black)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !dark ->
                dynamicLightColorScheme(context)
            dark -> FallbackDark
            else -> FallbackLight
        }

    MaterialTheme(
        colorScheme = scheme,
        typography = AramalrTypography,
        shapes = AramalrShapes,
        content = content,
    )
}
