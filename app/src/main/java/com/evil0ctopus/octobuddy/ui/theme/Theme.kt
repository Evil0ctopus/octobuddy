package com.evil0ctopus.octobuddy.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Always-dark cyber-ocean theme matching the Evil0ctopus brand mark
 * (navy / cyan / copper). Light mode is intentionally not offered —
 * the companion lives in the deep.
 */
private val BrandDark = darkColorScheme(
    primary = Brand.Cyan,
    onPrimary = Brand.NavyDeep,
    primaryContainer = Brand.CyanDim,
    onPrimaryContainer = Brand.Foam,
    secondary = Brand.Copper,
    onSecondary = Brand.NavyDeep,
    secondaryContainer = Brand.Copper.copy(alpha = 0.25f),
    onSecondaryContainer = Brand.CopperBright,
    tertiary = Brand.CyanSoft,
    onTertiary = Brand.NavyDeep,
    background = Brand.NavyDeep,
    onBackground = Brand.Foam,
    surface = Brand.Navy,
    onSurface = Brand.Foam,
    surfaceVariant = Brand.NavyCard,
    onSurfaceVariant = Brand.FoamDim,
    outline = Brand.CyanDim,
    error = Brand.Danger,
    onError = Brand.NavyDeep,
)

@Composable
fun OctoBuddyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BrandDark,
        typography = Typography,
        content = content,
    )
}
