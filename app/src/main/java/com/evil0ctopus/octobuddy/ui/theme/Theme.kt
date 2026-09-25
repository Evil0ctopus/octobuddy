package com.evil0ctopus.octobuddy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OceanDeep = Color(0xFF0B1D2A)
private val OceanMid = Color(0xFF123447)
private val Coral = Color(0xFFFF6B6B)
private val Teal = Color(0xFF2EC4B6)
private val Foam = Color(0xFFE8F4F8)
private val Sand = Color(0xFFFFF6E8)

private val DarkColors = darkColorScheme(
    primary = Teal,
    onPrimary = OceanDeep,
    secondary = Coral,
    onSecondary = OceanDeep,
    background = OceanDeep,
    onBackground = Foam,
    surface = OceanMid,
    onSurface = Foam,
    onSurfaceVariant = Foam.copy(alpha = 0.75f),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0D7377),
    onPrimary = Color.White,
    secondary = Coral,
    onSecondary = Color.White,
    background = Sand,
    onBackground = OceanDeep,
    surface = Color.White,
    onSurface = OceanDeep,
    onSurfaceVariant = OceanDeep.copy(alpha = 0.65f),
)

@Composable
fun OctoBuddyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}
