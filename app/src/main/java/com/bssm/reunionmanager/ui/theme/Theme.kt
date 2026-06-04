package com.bssm.reunionmanager.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = AppAccent,
    onPrimary = AppPanel,
    primaryContainer = AppAccentContainer,
    onPrimaryContainer = AppAccentActive,
    secondary = AppTextSecondary,
    onSecondary = AppPanel,
    tertiary = AppSuccess,
    onTertiary = AppPanel,
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
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
