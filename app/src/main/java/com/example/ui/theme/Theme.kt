package com.example.ui.theme

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

private val DarkColorScheme =
  darkColorScheme(
    primary = ClinTealLight,
    onPrimary = Color.Black,
    secondary = ClinNavyDark,
    onSecondary = Color.White,
    tertiary = ClinMutedDark,
    background = ClinDarkBg,
    surface = ClinDarkSurface,
    onBackground = Color(0xFFE8EEF4),
    onSurface = Color(0xFFE8EEF4),
    error = Color(0xFFEF5350),
  )

private val LightColorScheme =
  lightColorScheme(
    primary = ClinTeal,
    onPrimary = Color.White,
    secondary = ClinNavy,
    onSecondary = Color.White,
    tertiary = ClinMuted,
    background = ClinLightBg,
    surface = ClinLightSurface,
    onBackground = ClinNavy,
    onSurface = ClinNavy,
    error = ClinRed,
  )

@Composable
fun ClinNoteTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
