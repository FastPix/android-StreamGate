package dev.streamgate.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AmberSecondary,
    onPrimary = Color(0xFF5F1500),
    primaryContainer = OrangePrimaryContainer,
    onPrimaryContainer = OnOrangePrimaryContainer,
    secondary = Color(0xFFFFB786),
    onSecondary = Color(0xFF4D2700),
    secondaryContainer = AmberSecondaryContainer,
    onSecondaryContainer = OnAmberSecondaryContainer,
    tertiary = SlateTertiary,
    onTertiary = Color(0xFF282C34),
    tertiaryContainer = SlateTertiaryContainer,
    onTertiaryContainer = OnSlateTertiaryContainer,
    background = DeepBlack,
    onBackground = OffWhite,
    surface = DarkSurface,
    onSurface = OffWhite,
    surfaceVariant = Color(0xFF44474E),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8E9099)
)

private val LightColorScheme = lightColorScheme(
    primary = OrangePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCF),
    onPrimaryContainer = Color(0xFF350B00),
    secondary = AmberSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDCCF),
    onSecondaryContainer = Color(0xFF301400),
    tertiary = Color(0xFF5E626B),
    onTertiary = Color.White,
    background = LightSurface,
    onBackground = DeepBlack,
    surface = LightSurface,
    onSurface = DeepBlack,
    surfaceVariant = Color(0xFFE1E2EC),
    onSurfaceVariant = Color(0xFF44474E),
    outline = Color(0xFF74777F)
)

@Composable
fun StreamGateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
      colorScheme = colorScheme,
      typography = StreamgateTypography,
      content = content
    )
}