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
    primary = Color(0xFFA6C8FF),
    onPrimary = Color(0xFF003060),
    primaryContainer = Color(0xFF004787),
    onPrimaryContainer = Color(0xFFD5E3FF),
    secondary = Color(0xFFBDC7DC),
    onSecondary = Color(0xFF273141),
    secondaryContainer = Color(0xFF3D4758),
    onSecondaryContainer = Color(0xFFD8E3F8),
    tertiary = SynapseCyanLight,
    onTertiary = NeuroDarkNavy,
    tertiaryContainer = Color(0xFF004C6E),
    onTertiaryContainer = SynapseCyanLight,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = ProfessionalPrimary,
    onPrimary = ProfessionalOnPrimary,
    primaryContainer = ProfessionalPrimaryContainer,
    onPrimaryContainer = ProfessionalOnPrimaryContainer,
    secondary = ProfessionalSecondary,
    onSecondary = ProfessionalOnSecondary,
    secondaryContainer = ProfessionalSecondaryContainer,
    onSecondaryContainer = ProfessionalOnSecondaryContainer,
    tertiary = ProfessionalTertiary,
    onTertiary = ProfessionalOnTertiary,
    tertiaryContainer = ProfessionalTertiaryContainer,
    onTertiaryContainer = ProfessionalOnTertiaryContainer,
    error = ProfessionalError,
    onError = ProfessionalOnError,
    errorContainer = ProfessionalErrorContainer,
    onErrorContainer = ProfessionalOnErrorContainer,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Use custom Professional Polish theme by default
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

