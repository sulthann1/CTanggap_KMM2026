package id.ctanggap.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palet mengikuti desain Figma
val Red = Color(0xFF8A0A0A)
val Cream = Color(0xFFFADDBF)
val AppBg = Color(0xFFF5F6F8)
val Muted = Color(0xFF5B5B5B)
val OkGreen = Color(0xFF8EE0A0)
val WarnOrange = Color(0xFFFFD27F)
val InfoBlue = Color(0xFFBBD6F5)
val GreyChip = Color(0xFFECECEF)

@Composable
fun CTanggapTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Red, onPrimary = Color.White,
            secondary = Cream, onSecondary = Red,
            background = AppBg, surface = Color.White, error = Red
        ),
        content = content
    )
}
