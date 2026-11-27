package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val RedeemiDarkColorScheme = darkColorScheme(
    primary = RoyalPurple,
    onPrimary = TextPrimary,
    primaryContainer = DeepPurple,
    onPrimaryContainer = TextPrimary,
    secondary = ElectricBlue,
    onSecondary = TextPrimary,
    secondaryContainer = DeepBlue,
    onSecondaryContainer = TextPrimary,
    tertiary = LuxuryGold,
    onTertiary = ObsidianVoid,
    tertiaryContainer = GoldContainer,
    onTertiaryContainer = BrightGold,
    background = ObsidianVoid,
    onBackground = TextPrimary,
    surface = MidnightSurface,
    onSurface = TextPrimary,
    surfaceVariant = CardSlate,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianVoid,
    surfaceContainerLow = MidnightSurface,
    surfaceContainer = CardSlate,
    surfaceContainerHigh = ElevatedSlate,
    outline = SubtleBorder,
    outlineVariant = SubtleBorder,
    error = CrimsonError,
    onError = TextPrimary,
    errorContainer = CrimsonContainer,
    onErrorContainer = TextPrimary
)

val RedeemiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun RedeemiTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RedeemiDarkColorScheme,
        typography = Typography,
        shapes = RedeemiShapes,
        content = content
    )
}
