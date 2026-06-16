package atlas.messenger.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import atlas.messenger.generated.resources.NotoSans_Bold
import atlas.messenger.generated.resources.NotoSans_Medium
import atlas.messenger.generated.resources.NotoSans_Regular
import atlas.messenger.generated.resources.Res
import atlas.messenger.viewmodel.ColorPreset
import org.jetbrains.compose.resources.Font

private fun Color.toHsl(): Triple<Float, Float, Float> {
    val r = red
    val g = green
    val b = blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val l = (max + min) / 2f
    var h = 0f
    var s = 0f
    if (max != min) {
        val d = max - min
        s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
        h = when (max) {
            r -> ((g - b) / d + if (g < b) 6f else 0f) / 6f
            g -> ((b - r) / d + 2f) / 6f
            else -> ((r - g) / d + 4f) / 6f
        }
    }
    return Triple(h, s, l)
}

private fun hslToColor(h: Float, s: Float, l: Float): Color {
    val c = (1 - kotlin.math.abs(2 * l - 1)) * s
    val x = c * (1 - kotlin.math.abs((h * 6) % 2 - 1))
    val m = l - c / 2
    val (r, g, b) = when {
        h < 1f / 6 -> Triple(c, x, 0f)
        h < 2f / 6 -> Triple(x, c, 0f)
        h < 3f / 6 -> Triple(0f, c, x)
        h < 4f / 6 -> Triple(0f, x, c)
        h < 5f / 6 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(r + m, g + m, b + m)
}

private fun Color.lerp(target: Color, t: Float): Color = Color(
    red = this.red + (target.red - this.red) * t,
    green = this.green + (target.green - this.green) * t,
    blue = this.blue + (target.blue - this.blue) * t,
    alpha = this.alpha + (target.alpha - this.alpha) * t,
)

// converts a user's chosen accent into a ColorScheme family, following the M3
// Expressive tonal palette. Backgrounds/surfaces/outlines stay neutral so the
// accent drives the visual energy without overwhelming the screen.
private fun accentToColorScheme(
    accentHex: Int,
    preset: ColorPreset,
    isDark: Boolean,
): androidx.compose.material3.ColorScheme {
    val accent = Color(accentHex)
    val (hue, rawSat, rawLit) = accent.toHsl()

    val (sat, lit) = when (preset) {
        ColorPreset.DEFAULT -> Pair(rawSat, rawLit)
        ColorPreset.VIBRANT -> Pair(
            (rawSat * 2.0f).coerceIn(0.9f, 1.0f),
            (rawLit * 0.85f).coerceIn(0.25f, 0.5f)
        )
        ColorPreset.MUTED -> Pair(rawSat * 0.4f, rawLit * 0.9f)
        ColorPreset.PASTEL -> Pair(rawSat * 0.3f, (rawLit * 1.2f).coerceIn(0.7f, 0.9f))
    }

    // primary / secondary / tertiary tonal stops follow the M3 light/dark palette
    if (!isDark) {
        // Primary40: 40% tone, Primary90: 90% tone, Primary10: 10% tone
        val primary = hslToColor(hue, sat, 0.40f)
        val onPrimary = Color.White
        val primaryContainer = hslToColor(hue, (sat * 0.3f).coerceAtMost(0.4f), 0.92f)
        val onPrimaryContainer = hslToColor(hue, sat, 0.10f)
        // Secondary uses same hue with much lower saturation (M3 secondary is "less colorful")
        val secondary = hslToColor(hue, (sat * 0.4f).coerceAtMost(0.5f), 0.40f)
        val onSecondary = Color.White
        val secondaryContainer = hslToColor(hue, (sat * 0.2f).coerceAtMost(0.3f), 0.92f)
        val onSecondaryContainer = hslToColor(hue, (sat * 0.4f).coerceAtMost(0.5f), 0.15f)
        // Tertiary uses a slightly shifted hue (complementary) for M3 trio
        val tertiaryHue = (hue + 0.0833f) % 1f
        val tertiary = hslToColor(tertiaryHue, (sat * 0.6f).coerceAtMost(0.6f), 0.45f)
        val onTertiary = Color.White
        val tertiaryContainer = hslToColor(tertiaryHue, (sat * 0.3f).coerceAtMost(0.4f), 0.93f)
        val onTertiaryContainer = hslToColor(tertiaryHue, (sat * 0.6f).coerceAtMost(0.6f), 0.15f)

        return lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
        )
    } else {
        // dark mode: invert the tonal stops
        val primary = hslToColor(hue, (sat * 0.6f).coerceAtMost(0.7f), 0.78f)
        val onPrimary = hslToColor(hue, (sat * 0.5f).coerceAtMost(0.6f), 0.15f)
        val primaryContainer = hslToColor(hue, sat, 0.30f)
        val onPrimaryContainer = hslToColor(hue, (sat * 0.3f).coerceAtMost(0.4f), 0.92f)
        val secondary = hslToColor(hue, (sat * 0.3f).coerceAtMost(0.4f), 0.80f)
        val onSecondary = hslToColor(hue, (sat * 0.3f).coerceAtMost(0.4f), 0.18f)
        val secondaryContainer = hslToColor(hue, (sat * 0.4f).coerceAtMost(0.5f), 0.30f)
        val onSecondaryContainer = hslToColor(hue, (sat * 0.2f).coerceAtMost(0.3f), 0.92f)
        val tertiaryHue = (hue + 0.0833f) % 1f
        val tertiary = hslToColor(tertiaryHue, (sat * 0.5f).coerceAtMost(0.5f), 0.80f)
        val onTertiary = hslToColor(tertiaryHue, (sat * 0.5f).coerceAtMost(0.5f), 0.18f)
        val tertiaryContainer = hslToColor(tertiaryHue, (sat * 0.6f).coerceAtMost(0.6f), 0.32f)
        val onTertiaryContainer = hslToColor(tertiaryHue, (sat * 0.3f).coerceAtMost(0.4f), 0.92f)

        return androidx.compose.material3.darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AtlasAppTheme(
    textScale: Float = 1.0f,
    accentColor: Int = 0xFF2196F3.toInt(),
    contrast: Float = 1.0f,
    colorPreset: ColorPreset = ColorPreset.VIBRANT,
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    // a real bundled sans keeps atlas consistent without the weird broken font from before
    val appFont = FontFamily(
        Font(Res.font.NotoSans_Regular, FontWeight.Normal),
        Font(Res.font.NotoSans_Medium, FontWeight.Medium),
        Font(Res.font.NotoSans_Medium, FontWeight.SemiBold),
        Font(Res.font.NotoSans_Bold, FontWeight.Bold),
    )

    val typography = remember(textScale) {
        Typography(
            headlineLarge = TextStyle(fontFamily = appFont, fontWeight = FontWeight.Bold,     fontSize = 32.sp * textScale, letterSpacing = 0.sp),
            headlineMedium= TextStyle(fontFamily = appFont, fontWeight = FontWeight.Bold,     fontSize = 28.sp * textScale, letterSpacing = 0.sp),
            titleLarge    = TextStyle(fontFamily = appFont, fontWeight = FontWeight.SemiBold, fontSize = 22.sp * textScale, letterSpacing = 0.sp),
            titleMedium   = TextStyle(fontFamily = appFont, fontWeight = FontWeight.SemiBold, fontSize = 18.sp * textScale, letterSpacing = 0.sp),
            titleSmall    = TextStyle(fontFamily = appFont, fontWeight = FontWeight.Medium,   fontSize = 14.sp * textScale, letterSpacing = 0.sp),
            bodyLarge     = TextStyle(fontFamily = appFont, fontWeight = FontWeight.Normal,   fontSize = 16.sp * textScale, lineHeight = 24.sp, letterSpacing = 0.sp),
            bodyMedium    = TextStyle(fontFamily = appFont, fontWeight = FontWeight.Normal,   fontSize = 14.sp * textScale, lineHeight = 20.sp, letterSpacing = 0.sp),
            bodySmall     = TextStyle(fontFamily = appFont, fontWeight = FontWeight.Normal,   fontSize = 12.sp * textScale, lineHeight = 16.sp, letterSpacing = 0.sp),
            labelLarge    = TextStyle(fontFamily = appFont, fontWeight = FontWeight.Medium,   fontSize = 14.sp * textScale, letterSpacing = 0.sp),
            labelSmall    = TextStyle(fontFamily = appFont, fontWeight = FontWeight.Medium,   fontSize = 11.sp * textScale, letterSpacing = 0.sp),
        )
    }

    // start from the official M3 light/dark scheme, then tint the primary/secondary/tertiary
    // family toward the user's accent. Neutral surfaces stay neutral.
    val base = if (darkTheme) androidx.compose.material3.darkColorScheme() else androidx.compose.material3.lightColorScheme()
    val accent = accentToColorScheme(accentColor, colorPreset, isDark = darkTheme)
    val colorScheme = base.copy(
        primary = accent.primary,
        onPrimary = accent.onPrimary,
        primaryContainer = accent.primaryContainer,
        onPrimaryContainer = accent.onPrimaryContainer,
        secondary = accent.secondary,
        onSecondary = accent.onSecondary,
        secondaryContainer = accent.secondaryContainer,
        onSecondaryContainer = accent.onSecondaryContainer,
        tertiary = accent.tertiary,
        onTertiary = accent.onTertiary,
        tertiaryContainer = accent.tertiaryContainer,
        onTertiaryContainer = accent.onTertiaryContainer,
    )

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = typography,
        content     = content,
    )
}

private val LocalTextScale = staticCompositionLocalOf { 1.0f }
