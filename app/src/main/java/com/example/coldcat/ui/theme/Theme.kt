package com.example.coldcat.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CatRed,
    onPrimary = CatWhite,
    primaryContainer = CatRedDim,
    onPrimaryContainer = CatWhite,
    secondary = CatOrange,
    onSecondary = CatBlack,
    tertiary = CatGreen,
    onTertiary = CatBlack,
    background = CatBlack,
    onBackground = CatWhite,
    surface = CatDarkSurface,
    onSurface = CatWhite,
    surfaceVariant = CatCard,
    onSurfaceVariant = CatGray,
    outline = CatBorder,
    error = CatRed,
    onError = CatWhite
)

@Composable
fun ColdCatTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}