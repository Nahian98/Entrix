package com.apexbytestudio.entrix.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Entrix palette: an emerald accent on neutral (barely tinted) greys.
//
// - Emerald reads as "present / on track", which is what the app reports, and pairs with the
//   error red used for "tracking paused" as a clear good/attention pair.
// - Surfaces are near-neutral so the monochrome floating nav bar sits naturally on them.
// - Light uses a deep emerald (≈6:1 on white) for text-safe contrast; dark uses a brighter mint
//   accent and deep green containers so status cards don't glare.
//
// Every role the UI touches is set explicitly, including the surfaceContainer* family used for
// cards, so nothing falls back to Material's default purple-tinted baseline.

internal val LightColors = lightColorScheme(
    primary = Color(0xFF006C4C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC9F2DD),
    onPrimaryContainer = Color(0xFF002115),
    inversePrimary = Color(0xFF6CDBA9),

    secondary = Color(0xFF4C635A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE3ECE7),
    onSecondaryContainer = Color(0xFF17231E),

    tertiary = Color(0xFF3D6373),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC1E8FB),
    onTertiaryContainer = Color(0xFF001F29),

    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    background = Color(0xFFF8F9F8),
    onBackground = Color(0xFF191C1B),
    surface = Color(0xFFF8F9F8),
    onSurface = Color(0xFF191C1B),
    surfaceVariant = Color(0xFFDDE4E0),
    onSurfaceVariant = Color(0xFF444B47),
    surfaceTint = Color(0xFF006C4C),
    inverseSurface = Color(0xFF2E3130),
    inverseOnSurface = Color(0xFFEFF1F0),
    outline = Color(0xFF737A76),
    outlineVariant = Color(0xFFC2C8C4),
    scrim = Color(0xFF000000),

    surfaceBright = Color(0xFFF8F9F8),
    surfaceDim = Color(0xFFD8DBD9),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F4F3),
    surfaceContainer = Color(0xFFECEFED),
    surfaceContainerHigh = Color(0xFFE6E9E7),
    surfaceContainerHighest = Color(0xFFE1E3E2),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFF5FDDA6),
    onPrimary = Color(0xFF003825),
    primaryContainer = Color(0xFF0E4D36),
    onPrimaryContainer = Color(0xFFA9F4CF),
    inversePrimary = Color(0xFF006C4C),

    secondary = Color(0xFFB3CCBF),
    onSecondary = Color(0xFF1F352B),
    secondaryContainer = Color(0xFF29332E),
    onSecondaryContainer = Color(0xFFD5E3DB),

    tertiary = Color(0xFFA5CCDE),
    onTertiary = Color(0xFF073543),
    tertiaryContainer = Color(0xFF244C5A),
    onTertiaryContainer = Color(0xFFC1E8FB),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF5C1714),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color(0xFF0F1211),
    onBackground = Color(0xFFE1E3E1),
    surface = Color(0xFF0F1211),
    onSurface = Color(0xFFE1E3E1),
    surfaceVariant = Color(0xFF3B433F),
    onSurfaceVariant = Color(0xFFC0C8C3),
    surfaceTint = Color(0xFF5FDDA6),
    inverseSurface = Color(0xFFE1E3E1),
    inverseOnSurface = Color(0xFF2E3130),
    outline = Color(0xFF8A928E),
    outlineVariant = Color(0xFF404845),
    scrim = Color(0xFF000000),

    surfaceBright = Color(0xFF353837),
    surfaceDim = Color(0xFF0F1211),
    surfaceContainerLowest = Color(0xFF0A0D0C),
    surfaceContainerLow = Color(0xFF171A19),
    surfaceContainer = Color(0xFF1B1F1D),
    surfaceContainerHigh = Color(0xFF252927),
    surfaceContainerHighest = Color(0xFF303432),
)
