package com.guesswhere.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val NightScheme = darkColorScheme(
    primary = TealPrimary,
    onPrimary = TealOnPrimary,
    primaryContainer = TealPrimaryContainer,
    onPrimaryContainer = TealOnPrimaryContainer,
    secondary = MistSecondary,
    onSecondary = MistOnSecondary,
    secondaryContainer = MistSecondaryContainer,
    onSecondaryContainer = MistOnSecondaryContainer,
    tertiary = CoralTertiary,
    onTertiary = CoralOnTertiary,
    tertiaryContainer = CoralTertiaryContainer,
    onTertiaryContainer = CoralOnTertiaryContainer,
    background = NightBackground,
    onBackground = NightOnSurface,
    surface = NightSurface,
    onSurface = NightOnSurface,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = NightOnSurfaceVariant,
    outline = NightOutline,
    outlineVariant = NightOutlineVariant,
    error = NightError,
    onError = NightOnError,
    errorContainer = NightErrorContainer,
    onErrorContainer = NightOnErrorContainer,
    scrim = NightScrim,
)

private val DayScheme = lightColorScheme(
    primary = DayPrimary,
    onPrimary = DayOnPrimary,
    primaryContainer = DayPrimaryContainer,
    onPrimaryContainer = DayOnPrimaryContainer,
    secondary = DaySecondary,
    onSecondary = DayOnSecondary,
    secondaryContainer = DaySecondaryContainer,
    onSecondaryContainer = DayOnSecondaryContainer,
    tertiary = DayTertiary,
    onTertiary = DayOnTertiary,
    tertiaryContainer = DayTertiaryContainer,
    onTertiaryContainer = DayOnTertiaryContainer,
    background = DayBackground,
    onBackground = DayOnSurface,
    surface = DaySurface,
    onSurface = DayOnSurface,
    surfaceVariant = DaySurfaceVariant,
    onSurfaceVariant = DayOnSurfaceVariant,
    outline = DayOutline,
    outlineVariant = DayOutlineVariant,
    error = DayError,
    onError = DayOnError,
    errorContainer = DayErrorContainer,
    onErrorContainer = DayOnErrorContainer,
    scrim = DayScrim,
)

// Expressive shapes: extra-large radii give the pill buttons and cards the
// rounded, toy-like feel M3 Expressive pushes as its default.
private val GuessWhereShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(20.dp),
    medium = RoundedCornerShape(28.dp),
    large = RoundedCornerShape(36.dp),
    extraLarge = RoundedCornerShape(48.dp),
)

@Composable
fun GuessWhereTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) NightScheme else DayScheme,
        typography = GuessWhereTypography,
        shapes = GuessWhereShapes,
        content = content,
    )
}