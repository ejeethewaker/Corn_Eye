// App Theme
// Applies the CornEye Material3 color scheme and system bar styling.
package com.corneye.app.ui.theme
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = White,
    primaryContainer = GreenSurface,
    onPrimaryContainer = GreenDark,
    secondary = YellowAccent,
    onSecondary = TextPrimary,
    background = Background,
    onBackground = TextPrimary,
    surface = CardBackground,
    onSurface = TextPrimary,
    surfaceVariant = Background,
    onSurfaceVariant = TextSecondary,
    outline = DividerColor,
    error = StatusError,
    onError = White
)

@Composable
fun CornEyeTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = 0x80000000.toInt()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
