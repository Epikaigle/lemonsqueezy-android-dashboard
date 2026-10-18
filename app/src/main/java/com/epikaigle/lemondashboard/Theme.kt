package com.epikaigle.lemondashboard

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Modern Fintech Palette for Lemon Squeezy
val Lemon = Color(0xFFFFD600) // Electric Lemon Yellow
val LemonLight = Color(0xFFFFF275)
val LemonDark = Color(0xFFE5B800)
val LemonContainer = Color(0xFF332B00)
val LemonOnContainer = Color(0xFFFFF076)

// Deep Sleek Canvas Palette
val DarkBackground = Color(0xFF0C1015)
val DarkSurface = Color(0xFF141A22)
val DarkSurfaceVariant = Color(0xFF1B232D)
val DarkSurfaceContainer = Color(0xFF202A36)
val DarkSurfaceContainerHigh = Color(0xFF273342)
val DarkBorder = Color(0xFF232D3B)
val DarkBorderHighlight = Color(0xFF334155)

// Typography & Content Colors
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

// Semantic Accents
val MintSuccess = Color(0xFF10B981)
val MintSuccessLight = Color(0xFF34D399)
val MintSuccessContainer = Color(0xFF064E3B)

val CoralDanger = Color(0xFFF43F5E)
val CoralDangerLight = Color(0xFFFB7185)
val CoralDangerContainer = Color(0xFF4C0519)

val AmberWarning = Color(0xFFF59E0B)
val AmberWarningLight = Color(0xFFFBBF24)
val AmberWarningContainer = Color(0xFF451A03)

val PurpleAccent = Color(0xFF8B5CF6)
val PurpleAccentContainer = Color(0xFF2E1065)

// Material 3 Dark Color Scheme
val LemonDashboardColorScheme = darkColorScheme(
    primary = Lemon,
    onPrimary = Color(0xFF1B1600),
    primaryContainer = LemonContainer,
    onPrimaryContainer = LemonOnContainer,
    secondary = MintSuccessLight,
    onSecondary = DarkBackground,
    secondaryContainer = MintSuccessContainer.copy(alpha = 0.4f),
    onSecondaryContainer = MintSuccessLight,
    tertiary = PurpleAccent,
    onTertiary = TextPrimary,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = CoralDanger,
    onError = TextPrimary,
    errorContainer = CoralDangerContainer.copy(alpha = 0.5f),
    onErrorContainer = CoralDangerLight,
    outline = DarkBorder,
    outlineVariant = DarkBorderHighlight
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
