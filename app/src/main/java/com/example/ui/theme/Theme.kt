package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = PeacockTeal,
    onPrimary = Color.White,
    primaryContainer = PeacockTealContainer,
    onPrimaryContainer = OnPeacockTealContainer,
    secondary = KonarkSaffron,
    onSecondary = Color.White,
    secondaryContainer = KonarkSaffronContainer,
    onSecondaryContainer = OnKonarkSaffronContainer,
    tertiary = TerracottaCoral,
    onTertiary = Color.White,
    tertiaryContainer = TerracottaContainer,
    onTertiaryContainer = OnTerracottaContainer,
    background = IvoryBackground,
    onBackground = InkDark,
    surface = Color.White,
    onSurface = InkDark,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = InkMuted,
    surfaceContainerLow = WarmSurface,
    outline = OutlineWarm,
    error = DangerRed,
    onError = Color.White,
    errorContainer = DangerRedBg
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPeacock,
    onPrimary = Color(0xFF002023),
    primaryContainer = Color(0xFF064249),
    onPrimaryContainer = Color(0xFFCFF4F7),
    secondary = DarkSaffron,
    onSecondary = Color(0xFF331800),
    secondaryContainer = Color(0xFF5A3200),
    onSecondaryContainer = Color(0xFFFEF3C7),
    tertiary = DarkCoral,
    onTertiary = Color(0xFF380F00),
    tertiaryContainer = Color(0xFF6B2407),
    onTertiaryContainer = Color(0xFFFFEDD5),
    background = DarkBackground,
    onBackground = Color(0xFFEAF2F4),
    surface = DarkSurface,
    onSurface = Color(0xFFEAF2F4),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFB5C4C8),
    surfaceContainerLow = Color(0xFF182528),
    outline = Color(0xFF35494F),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D)
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun OdiaSikshyaSathiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
