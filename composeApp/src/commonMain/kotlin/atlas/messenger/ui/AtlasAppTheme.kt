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

private fun generateColorScheme(accentHex: Int, contrast: Float, preset: ColorPreset): androidx.compose.material3.ColorScheme {
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

    val t = (contrast - 1.0f).coerceIn(-0.5f, 0.5f) / 0.5f

    fun dark(h: Float, s: Float, l: Float) = hslToColor(h, s, l)
    fun light(h: Float, s: Float, l: Float) = hslToColor(h, s, l)
    fun mix(dark: Color, light: Color) = dark.copy(
        red = dark.red.lerp(light.red, t),
        green = dark.green.lerp(light.green, t),
        blue = dark.blue.lerp(light.blue, t),
    )

    val primaryD = dark(hue, sat * 0.8f, 0.70f)
    val primaryL = light(hue, sat * 0.9f, 0.40f)
    val primary = mix(primaryD, primaryL)

    val onPrimaryD = dark(hue, sat * 0.1f, 0.15f)
    val onPrimaryL = Color.White
    val onPrimary = mix(onPrimaryD, onPrimaryL)

    val primaryContainerD = dark(hue, sat * 0.4f, 0.20f)
    val primaryContainerL = light(hue, sat * 0.2f, 0.90f)
    val primaryContainer = mix(primaryContainerD, primaryContainerL)

    val onPrimaryContainerD = dark(hue, sat * 0.2f, 0.95f)
    val onPrimaryContainerL = light(hue, sat * 0.6f, 0.10f)
    val onPrimaryContainer = mix(onPrimaryContainerD, onPrimaryContainerL)

    val secondaryD = dark(hue, sat * 0.3f, 0.7f)
    val secondaryL = light(hue, sat * 0.2f, 0.4f)
    val secondary = mix(secondaryD, secondaryL)

    val onSecondaryD = dark(hue, sat * 0.1f, 0.15f)
    val onSecondaryL = Color.White
    val onSecondary = mix(onSecondaryD, onSecondaryL)

    val secondaryContainerD = dark(hue, sat * 0.35f, 0.25f)
    val secondaryContainerL = light(hue, sat * 0.25f, 0.85f)
    val secondaryContainer = mix(secondaryContainerD, secondaryContainerL)

    val onSecondaryContainerD = dark(hue, sat * 0.25f, 0.85f)
    val onSecondaryContainerL = light(hue, sat * 0.35f, 0.20f)
    val onSecondaryContainer = mix(onSecondaryContainerD, onSecondaryContainerL)

    val backgroundD = dark(hue, sat * 0.05f, 0.05f)
    val backgroundL = Color.White
    val background = mix(backgroundD, backgroundL)

    val surfaceD = dark(hue, sat * 0.08f, 0.10f)
    val surfaceL = light(hue, sat * 0.02f, 1.0f)
    val surface = mix(surfaceD, surfaceL)

    val surfaceVariantD = dark(hue, sat * 0.1f, 0.15f)
    val surfaceVariantL = light(hue, sat * 0.05f, 0.95f)
    val surfaceVariant = mix(surfaceVariantD, surfaceVariantL)

    val surfaceContainerD = dark(hue, sat * 0.1f, 0.12f)
    val surfaceContainerL = light(hue, sat * 0.05f, 0.94f)
    val surfaceContainer = mix(surfaceContainerD, surfaceContainerL)

    val onSurfaceD = Color.White
    val onSurfaceL = Color(0xFF1C1B1F)
    val onSurface = mix(onSurfaceD, onSurfaceL)

    val onSurfaceVariantD = Color(0xFFCAC4D0)
    val onSurfaceVariantL = Color(0xFF49454F)
    val onSurfaceVariant = mix(onSurfaceVariantD, onSurfaceVariantL)

    val onBackgroundD = Color.White
    val onBackgroundL = Color(0xFF1C1B1F)
    val onBackground = mix(onBackgroundD, onBackgroundL)

    val outlineD = dark(hue, sat * 0.15f, 0.40f)
    val outlineL = light(hue, sat * 0.12f, 0.60f)
    val outline = mix(outlineD, outlineL)

    val outlineVariantD = dark(hue, sat * 0.10f, 0.25f)
    val outlineVariantL = light(hue, sat * 0.08f, 0.80f)
    val outlineVariant = mix(outlineVariantD, outlineVariantL)

    val surfaceContainerLowD = dark(hue, sat * 0.25f, 0.14f)
    val surfaceContainerLowL = light(hue, sat * 0.15f, 0.94f)
    val surfaceContainerLow = mix(surfaceContainerLowD, surfaceContainerLowL)

    val surfaceContainerHighD = dark(hue, sat * 0.35f, 0.20f)
    val surfaceContainerHighL = light(hue, sat * 0.20f, 0.87f)
    val surfaceContainerHigh = mix(surfaceContainerHighD, surfaceContainerHighL)

    val errorD = Color(0xFFEF5350)
    val errorL = Color(0xFFBA1A1A)
    val error = mix(errorD, errorL)

    val onErrorD = Color.White
    val onErrorL = Color.White
    val onError = mix(onErrorD, onErrorL)

    val errorContainerD = dark(0f, 0.6f, 0.25f)
    val errorContainerL = light(0f, 0.7f, 0.92f)
    val errorContainer = mix(errorContainerD, errorContainerL)

    val onErrorContainerD = dark(0f, 0.5f, 0.85f)
    val onErrorContainerL = light(0f, 0.5f, 0.15f)
    val onErrorContainer = mix(onErrorContainerD, onErrorContainerL)

    return darkColorScheme(
        primary              = primary,
        onPrimary            = onPrimary,
        primaryContainer     = primaryContainer,
        onPrimaryContainer   = onPrimaryContainer,
        secondary            = secondary,
        onSecondary          = onSecondary,
        secondaryContainer   = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        background           = background,
        onBackground         = onBackground,
        surface              = surface,
        onSurface            = onSurface,
        surfaceVariant       = surfaceVariant,
        onSurfaceVariant     = onSurfaceVariant,
        surfaceContainer     = surfaceContainer,
        surfaceContainerLow  = surfaceContainerLow,
        surfaceContainerHigh = surfaceContainerHigh,
        outline              = outline,
        outlineVariant       = outlineVariant,
        error                = error,
        onError              = onError,
        errorContainer       = errorContainer,
        onErrorContainer     = onErrorContainer,
    )
}

private fun Float.lerp(target: Float, t: Float) = this + (target - this) * t

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtlasAppTheme(textScale: Float = 1.0f, accentColor: Int = 0xFF2196F3.toInt(), contrast: Float = 1.0f, colorPreset: ColorPreset = ColorPreset.VIBRANT, content: @Composable () -> Unit) {
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

    MaterialTheme(
        colorScheme = generateColorScheme(accentColor, contrast, colorPreset),
        typography  = typography,
        content     = content,
    )
}

private val LocalTextScale = staticCompositionLocalOf { 1.0f }
