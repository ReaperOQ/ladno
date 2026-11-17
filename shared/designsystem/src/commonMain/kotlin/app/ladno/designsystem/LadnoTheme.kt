package app.ladno.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    background = Color(0xFFF5F4F0),
    onBackground = Color(0xFF172522),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF172522),
    primary = Color(0xFF00574F),
    onPrimary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE6DFF7),
    onSecondaryContainer = Color(0xFF342A48),
    onSurfaceVariant = Color(0xFF4F5F59),
    outline = Color(0xFF74827B),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    background = Color(0xFF121716),
    onBackground = Color(0xFFE4ECE7),
    surface = Color(0xFF1D2422),
    onSurface = Color(0xFFE4ECE7),
    primary = Color(0xFF83D5C6),
    onPrimary = Color(0xFF003A34),
    secondaryContainer = Color(0xFF3C3450),
    onSecondaryContainer = Color(0xFFEAE1FF),
    onSurfaceVariant = Color(0xFFB8C8C0),
    outline = Color(0xFF8A9991),
    error = Color(0xFFFFB4AB),
)

private val LadnoShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LadnoTheme(content: @Composable () -> Unit) {
    MaterialExpressiveTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        shapes = LadnoShapes,
        content = content,
    )
}
