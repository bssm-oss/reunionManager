package com.bssm.reunionmanager.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = AppAccent,
    onPrimary = AppTextPrimary,
    primaryContainer = AppAccentContainer,
    onPrimaryContainer = AppTextPrimary,
    secondary = AppTextSecondary,
    onSecondary = AppBackground,
    tertiary = AppSuccess,
    onTertiary = AppBackground,
    background = AppBackground,
    onBackground = AppTextPrimary,
    surface = AppPanel,
    onSurface = AppTextPrimary,
    surfaceVariant = AppSurfaceHigh,
    onSurfaceVariant = AppTextSecondary,
    error = AppError,
    onError = AppTextPrimary,
    errorContainer = AppErrorContainer,
    onErrorContainer = AppTextPrimary,
    outline = AppBorder,
    outlineVariant = AppBorderSubtle,
)

@Composable
fun ReunionManagerTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else DarkColors,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
