package com.fintechforge.zenimintfunds.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Configuración para MODO OSCURO
private val DarkColorScheme = darkColorScheme(
    primary = MintPrimary,
    onPrimary = DarkBackground,       // Texto sobre color primario
    secondary = MintLight,
    onSecondary = DarkBackground,
    tertiary = GoldWarning,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    error = RedError
)

// Configuración para MODO CLARO
private val LightColorScheme = lightColorScheme(
    primary = MintDark,               // Usamos el oscuro para contraste en fondo blanco
    onPrimary = LightSurface,
    secondary = MintPrimary,
    onSecondary = LightSurface,
    tertiary = GoldWarning,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    error = RedError
)

@Composable
fun ZenimintFundsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // <--- IMPORTANTE: False para forzar nuestra marca
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Pintamos la barra de estado (donde está la hora) del color de fondo
            window.statusBarColor = colorScheme.background.toArgb()
            // Controlamos si los íconos de la barra son blancos o negros
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}