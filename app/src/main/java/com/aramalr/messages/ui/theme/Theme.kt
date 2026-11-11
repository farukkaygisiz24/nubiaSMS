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

/** MyOS Settings night card (`mfv_common_card_bg_color`). */
val MyOsCardSurface = Color(0xFF1B1B1B)
val MyOsCardSurfaceLight = Color(0xFFFFFFFF)
/** MyOS search night (`mfvc_common_search_bg`). */
val MyOsSearchSurface = Color(0xFF2D2D2D)
val MyOsSearchSurfaceLight = Color(0xFFEBEBEB)
val MyOsSecondaryText = Color(0xFFD0D0D0)
val MyOsAccentReadable = Color(0xFFB4C0FF)
val MyOsSecondaryTextLight = Color(0xFF5C5C5C)

private val FallbackDark =
    darkColorScheme(
        background = Color.Black,
        surface = MyOsCardSurface,
        surfaceVariant = MyOsSearchSurface,
        onBackground = Color.White,
        onSurface = Color.White,
        onSurfaceVariant = MyOsSecondaryText,
        primary = Color(0xFF8C9EFF),
        onPrimary = Color.Black,
        primaryContainer = Color(0xFF3A3F5C),
        onPrimaryContainer = Color.White,
    )

private val FallbackLight =
    lightColorScheme(
        background = Color(0xFFF2F2F2),
        surface = MyOsCardSurfaceLight,
        surfaceVariant = MyOsSearchSurfaceLight,
        onBackground = Color(0xFF111111),
        onSurface = Color(0xFF111111),
        onSurfaceVariant = MyOsSecondaryTextLight,
    )

@Composable
fun AramalrTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme =
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> {
                val dynamic = dynamicDarkColorScheme(context)
                dynamic.copy(
                    background = Color.Black,
                    surface = MyOsCardSurface,
                    surfaceVariant = MyOsSearchSurface,
                    onBackground = Color.White,
                    onSurface = Color.White,
                    onSurfaceVariant = MyOsSecondaryText,
                    primary = MyOsAccentReadable,
                    onPrimary = Color.Black,
                    onPrimaryContainer = Color.White,
                    onSecondary = Color.Black,
                    onSecondaryContainer = Color.White,
                    onTertiary = Color.Black,
                    onTertiaryContainer = Color.White,
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !dark -> {
                val dynamic = dynamicLightColorScheme(context)
                dynamic.copy(
                    background = Color(0xFFF2F2F2),
                    surface = MyOsCardSurfaceLight,
                    surfaceVariant = MyOsSearchSurfaceLight,
                    onBackground = Color(0xFF111111),
                    onSurface = Color(0xFF111111),
                    onSurfaceVariant = MyOsSecondaryTextLight,
                )
            }
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
