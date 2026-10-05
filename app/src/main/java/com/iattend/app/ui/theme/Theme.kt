package com.iattend.app.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import com.iattend.app.core.datastore.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = ForestPrimary,
    onPrimary = ForestOnPrimary,
    primaryContainer = ForestPrimaryContainer,
    onPrimaryContainer = ForestOnPrimaryContainer,
    secondary = ForestSecondary,
    onSecondary = ForestOnSecondary,
    secondaryContainer = ForestSecondaryContainer,
    onSecondaryContainer = ForestOnSecondaryContainer,
    tertiary = ForestTertiary,
    onTertiary = ForestOnTertiary,
    tertiaryContainer = ForestTertiaryContainer,
    onTertiaryContainer = ForestOnTertiaryContainer,
    error = ForestError,
    onError = ForestOnError,
    errorContainer = ForestErrorContainer,
    onErrorContainer = ForestOnErrorContainer,
    background = ForestBackground,
    onBackground = ForestOnBackground,
    surface = ForestSurface,
    onSurface = ForestOnSurface,
    surfaceVariant = ForestSurfaceVariant,
    onSurfaceVariant = ForestOnSurfaceVariant,
    outline = ForestOutline,
    outlineVariant = Color(0xFFBCC8BD),
    surfaceDim = Color(0xFFDCE5DB),
    surfaceBright = ForestSurface,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF2F6EF),
    surfaceContainer = Color(0xFFEAF0E7),
    surfaceContainerHigh = Color(0xFFE2EADF),
    surfaceContainerHighest = Color(0xFFDCE5DB)
)

private val DarkColorScheme = darkColorScheme(
    primary = ForestPrimaryDark,
    onPrimary = ForestOnPrimaryDark,
    primaryContainer = ForestPrimaryContainerDark,
    onPrimaryContainer = ForestOnPrimaryContainerDark,
    secondary = ForestSecondaryDark,
    onSecondary = ForestOnSecondaryDark,
    secondaryContainer = ForestSecondaryContainerDark,
    onSecondaryContainer = ForestOnSecondaryContainerDark,
    tertiary = ForestTertiaryDark,
    onTertiary = ForestOnTertiaryDark,
    tertiaryContainer = ForestTertiaryContainerDark,
    onTertiaryContainer = ForestOnTertiaryContainerDark,
    error = ForestErrorDark,
    onError = ForestOnErrorDark,
    errorContainer = ForestErrorContainerDark,
    onErrorContainer = ForestOnErrorContainerDark,
    background = ForestBackgroundDark,
    onBackground = ForestOnBackgroundDark,
    surface = ForestSurfaceDark,
    onSurface = ForestOnSurfaceDark,
    surfaceVariant = ForestSurfaceVariantDark,
    onSurfaceVariant = ForestOnSurfaceVariantDark,
    outline = ForestOutlineDark,
    outlineVariant = Color(0xFF536055),
    surfaceDim = Color(0xFF101410),
    surfaceBright = Color(0xFF363E37),
    surfaceContainerLowest = Color(0xFF0D110E),
    surfaceContainerLow = Color(0xFF1B211C),
    surfaceContainer = Color(0xFF232B24),
    surfaceContainerHigh = Color(0xFF2D362E),
    surfaceContainerHighest = Color(0xFF374238)
)

// 10 Cashiro accents selected (5 Catppuccin + 5 Rosé Pine) for integration spec 7B
val AvailableAccents = listOf("FOREST", "ROSEWATER", "BLUE", "GREEN", "MAUVE", "PEACH", "PINE_ROSE", "PINE_IRIS", "PINE_PINE", "PINE_GOLD", "PINE_FOAM")

private fun getCustomLightScheme(accent: String): ColorScheme = when (accent) {
    // Rosewater/Peach are light enough that the default White onPrimary fails contrast against
    // their own primary (e.g. Rosewater's ~1.6:1 vs White, needs ~3:1) - same dark-onPrimary
    // pattern the Pine accents below already use for their own light pills.
    "ROSEWATER" -> accentLight(primary = Latte_Rosewater, secondary = Latte_Rosewater_secondary, tertiary = Latte_Rosewater_tertiary, isPine = false, onPrimary = Color(0xFF1a1b20))
    "BLUE" -> accentLight(primary = Latte_Blue, secondary = Latte_Blue_secondary, tertiary = Latte_Blue_tertiary, isPine = false)
    "GREEN" -> accentLight(primary = Latte_Green, secondary = Latte_Green_secondary, tertiary = Latte_Green_tertiary, isPine = false)
    "MAUVE" -> accentLight(primary = Latte_Mauve, secondary = Latte_Mauve_secondary, tertiary = Latte_Mauve_tertiary, isPine = false)
    "PEACH" -> accentLight(primary = Latte_Peach, secondary = Latte_Peach_secondary, tertiary = Latte_Peach_tertiary, isPine = false, onPrimary = Color(0xFF1a1b20))
    "PINE_ROSE" -> accentLight(primary = Dawn_Rose, secondary = Dawn_Rose_secondary, tertiary = Dawn_Rose_tertiary, isPine = true, onPrimary = Dawn_OnBackground)
    "PINE_IRIS" -> accentLight(primary = Dawn_Iris, secondary = Dawn_Iris_secondary, tertiary = Dawn_Iris_tertiary, isPine = true, onPrimary = Dawn_Surface_Base)
    "PINE_PINE" -> accentLight(primary = Dawn_Pine, secondary = Dawn_Pine_secondary, tertiary = Dawn_Pine_tertiary, isPine = true, onPrimary = Dawn_Surface_Base)
    "PINE_GOLD" -> accentLight(primary = Dawn_Gold, secondary = Dawn_Gold_secondary, tertiary = Dawn_Gold_tertiary, isPine = true, onPrimary = Dawn_OnBackground)
    "PINE_FOAM" -> accentLight(primary = Dawn_Foam, secondary = Dawn_Foam_secondary, tertiary = Dawn_Foam_tertiary, isPine = true, onPrimary = Dawn_Surface_Base)
    else -> LightColorScheme
}

private fun getCustomDarkScheme(accent: String): ColorScheme = when (accent) {
    "ROSEWATER" -> accentDark(primary = Macchiato_Rosewater, secondary = Macchiato_Rosewater_dim_secondary, tertiary = Macchiato_Rosewater_dim_tertiary, isPine = false)
    "BLUE" -> accentDark(primary = Macchiato_Blue, secondary = Macchiato_Blue_dim_secondary, tertiary = Macchiato_Blue_dim_tertiary, isPine = false)
    "GREEN" -> accentDark(primary = Macchiato_Green, secondary = Macchiato_Green_dim_secondary, tertiary = Macchiato_Green_dim_tertiary, isPine = false)
    "MAUVE" -> accentDark(primary = Macchiato_Mauve, secondary = Macchiato_Mauve_dim_secondary, tertiary = Macchiato_Mauve_dim_tertiary, isPine = false)
    "PEACH" -> accentDark(primary = Macchiato_Peach, secondary = Macchiato_Peach_dim_secondary, tertiary = Macchiato_Peach_dim_tertiary, isPine = false)
    "PINE_ROSE" -> accentDark(primary = RosePine_Rose, secondary = RosePine_Rose_secondary, tertiary = RosePine_Rose_tertiary, isPine = true)
    "PINE_IRIS" -> accentDark(primary = RosePine_Iris, secondary = RosePine_Iris_secondary, tertiary = RosePine_Iris_tertiary, isPine = true)
    "PINE_PINE" -> accentDark(primary = RosePine_Pine, secondary = RosePine_Pine_secondary, tertiary = RosePine_Pine_tertiary, isPine = true)
    "PINE_GOLD" -> accentDark(primary = RosePine_Gold, secondary = RosePine_Gold_secondary, tertiary = RosePine_Gold_tertiary, isPine = true)
    "PINE_FOAM" -> accentDark(primary = RosePine_Foam, secondary = RosePine_Foam_secondary, tertiary = RosePine_Foam_tertiary, isPine = true)
    else -> DarkColorScheme
}

private fun accentLight(primary: Color, secondary: Color, tertiary: Color, isPine: Boolean, onPrimary: Color = Color.White): ColorScheme {
    return lightColorScheme(
        primary = primary, onPrimary = onPrimary, primaryContainer = primary, onPrimaryContainer = onPrimary,
        secondary = secondary, onSecondary = Color.White, secondaryContainer = secondary, onSecondaryContainer = Color.White,
        tertiary = tertiary, onTertiary = Color.White, tertiaryContainer = tertiary, onTertiaryContainer = Color.White,
        background = if (isPine) Dawn_Background else Color(0xFFe2e2e9),
        onBackground = if (isPine) Dawn_OnBackground else Color(0xFF1a1b20),
        surface = if (isPine) Dawn_Background else Color(0xFFE5E5EA),
        onSurface = if (isPine) Dawn_OnSurface else Color(0xFF1a1b20),
        surfaceVariant = if (isPine) Dawn_SurfaceVariant else Color(0xFFc4c6d0),
        onSurfaceVariant = if (isPine) Dawn_OnSurfaceVariant else Color(0xFF44474f),
        inverseSurface = if (isPine) Color(0xFF26233A) else Color(0xFF2f3036),
        inverseOnSurface = if (isPine) Color(0xFFE0DEF4) else Color(0xFFf0f0f7),
        error = Color(0xFFBA1A1A), onError = Color.White,
        surfaceBright = if (isPine) Dawn_Surface_Base else Color(0xFFE8E9EC),
        surfaceDim = if (isPine) Dawn_SurfaceVariant else Color(0xFFd9d9e0),
        surfaceContainer = if (isPine) Dawn_Background else Color(0xFFf0f0f7),
        surfaceContainerHigh = if (isPine) Dawn_SurfaceVariant else Color(0xFFe8e7ee),
        surfaceContainerHighest = if (isPine) Color(0xFFE6DDD5) else Color(0xFFe2e2e9),
        surfaceContainerLow = if (isPine) Dawn_Surface_Base else Color(0xFFf9f9ff),
        surfaceContainerLowest = Color.White
    )
}

private fun accentDark(primary: Color, secondary: Color, tertiary: Color, isPine: Boolean): ColorScheme {
    return darkColorScheme(
        primary = primary, onPrimary = Color.White, primaryContainer = primary, onPrimaryContainer = Color.White,
        secondary = secondary, onSecondary = Color.White, secondaryContainer = secondary, onSecondaryContainer = Color.White,
        tertiary = tertiary, onTertiary = Color.White, tertiaryContainer = tertiary, onTertiaryContainer = Color.White,
        background = if (isPine) RosePine_Background else Color(0xFF111318),
        onBackground = if (isPine) RosePine_OnBackground else Color(0xFFe2e2e9),
        surface = if (isPine) RosePine_Background else Color(0xFF111318),
        onSurface = if (isPine) RosePine_OnSurface else Color(0xFFe2e2e9),
        surfaceVariant = if (isPine) RosePine_SurfaceVariant else Color(0xFF1e1f25),
        onSurfaceVariant = if (isPine) RosePine_OnSurfaceVariant else Color(0xFFc4c6d0),
        inverseSurface = if (isPine) Color(0xFFE0DEF4) else Color(0xFFe2e2e9),
        inverseOnSurface = if (isPine) Color(0xFF26233A) else Color(0xFF2f3036),
        error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
        surfaceBright = if (isPine) RosePine_SurfaceVariant else Color(0xFF37393e),
        surfaceDim = if (isPine) RosePine_Background else Color(0xFF0c0e13),
        surfaceContainer = if (isPine) Color(0xFF292637) else Color(0xFF24262C),
        surfaceContainerHigh = if (isPine) Color(0xFF322E43) else Color(0xFF282a2f),
        surfaceContainerHighest = if (isPine) Color(0xFF403D52) else Color(0xFF33353a),
        surfaceContainerLow = if (isPine) RosePine_Surface_Base else Color(0xFF1e1f25),
        surfaceContainerLowest = if (isPine) Color(0xFF12101B) else Color(0xFF0C0E13)
    )
}

internal fun colorContrast(first: Color, second: Color): Float {
    val a = first.luminance()
    val b = second.luminance()
    return (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
}

/** Keep the chosen hue; change its lightness only when a semantic role needs contrast. */
private fun Color.readableOn(background: Color, minimum: Float = 4.5f): Color {
    if (colorContrast(this, background) >= minimum) return this
    val target = if (background.luminance() < 0.18f) Color.White else Color(0xFF111318)
    var low = 0f
    var high = 1f
    repeat(16) {
        val middle = (low + high) / 2
        if (colorContrast(lerp(this, target, middle), background) >= minimum) high = middle else low = middle
    }
    return lerp(this, target, high)
}

internal fun foregroundFor(fill: Color): Color = when {
    colorContrast(Color(0xFF111318), fill) >= 4.5f -> Color(0xFF111318)
    colorContrast(Color.White, fill) >= 4.5f -> Color.White
    else -> Color.Black
}

/** All custom palettes share semantic contrast requirements, including secondary/tertiary fills. */
internal fun customColorScheme(accent: String, dark: Boolean): ColorScheme {
    val base = if (dark) getCustomDarkScheme(accent) else getCustomLightScheme(accent)
    val textSurface = base.surfaceContainerHighest
    return base.copy(
        primary = base.primary.readableOn(textSurface),
        onPrimary = foregroundFor(base.primary.readableOn(textSurface)),
        onPrimaryContainer = foregroundFor(base.primaryContainer),
        secondary = base.secondary.readableOn(textSurface),
        onSecondary = foregroundFor(base.secondary.readableOn(textSurface)),
        onSecondaryContainer = foregroundFor(base.secondaryContainer),
        tertiary = base.tertiary.readableOn(textSurface),
        onTertiary = foregroundFor(base.tertiary.readableOn(textSurface)),
        onTertiaryContainer = foregroundFor(base.tertiaryContainer),
        onSurfaceVariant = base.onSurfaceVariant.readableOn(textSurface),
        outline = base.onSurfaceVariant.readableOn(textSurface, 3f),
        outlineVariant = lerp(base.surfaceContainerHighest, base.onSurfaceVariant, 0.45f)
    )
}

/** Black canvas with explicit near-black elevation; cards must not disappear into the canvas. */
internal fun ColorScheme.amoled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceBright = Color(0xFF363940),
    surfaceContainerLowest = Color(0xFF090A0C),
    surfaceContainerLow = Color(0xFF14161A),
    surfaceContainer = Color(0xFF1D2025),
    surfaceContainerHigh = Color(0xFF272B31),
    surfaceContainerHighest = Color(0xFF333840),
    surfaceVariant = Color(0xFF333840),
    onBackground = Color(0xFFF5F5F7),
    onSurface = Color(0xFFF5F5F7),
    onSurfaceVariant = Color(0xFFCCD0D8),
    outline = Color(0xFF9299A5),
    outlineVariant = Color(0xFF5B626D),
    primary = primary.readableOn(Color(0xFF333840)),
    onPrimary = foregroundFor(primary.readableOn(Color(0xFF333840))),
    secondary = secondary.readableOn(Color(0xFF333840)),
    onSecondary = foregroundFor(secondary.readableOn(Color(0xFF333840))),
    tertiary = tertiary.readableOn(Color(0xFF333840)),
    onTertiary = foregroundFor(tertiary.readableOn(Color(0xFF333840)))
)

/** Soft tinted canvas, brighter cards and stronger grouped surfaces instead of a white wash. */
internal fun ColorScheme.lightSurfaces(): ColorScheme {
    fun tinted(value: Long) = lerp(Color(value), primary, 0.045f)
    val groupedSurface = tinted(0xFFC9D3CB)
    return copy(
        background = tinted(0xFFD9E0DA),
        surface = tinted(0xFFE5EBE6),
        surfaceDim = groupedSurface,
        surfaceBright = tinted(0xFFF1F4F1),
        surfaceContainerLowest = tinted(0xFFF3F5F2),
        surfaceContainerLow = tinted(0xFFE7ECE8),
        surfaceContainer = tinted(0xFFDDE4DE),
        surfaceContainerHigh = tinted(0xFFD4DDD5),
        surfaceContainerHighest = groupedSurface,
        surfaceVariant = groupedSurface,
        onBackground = Color(0xFF171D19),
        onSurface = Color(0xFF171D19),
        onSurfaceVariant = Color(0xFF354139),
        outline = Color(0xFF59675D),
        outlineVariant = tinted(0xFF89978C),
        primary = primary.readableOn(groupedSurface),
        onPrimary = foregroundFor(primary.readableOn(groupedSurface)),
        secondary = secondary.readableOn(groupedSurface),
        onSecondary = foregroundFor(secondary.readableOn(groupedSurface)),
        tertiary = tertiary.readableOn(groupedSurface),
        onTertiary = foregroundFor(tertiary.readableOn(groupedSurface))
    )
}

/** Plain (non-@Composable) scheme resolution, factored out of [IAttendTheme] so non-Compose
 * surfaces - namely the home screen widget, which renders via Glance/RemoteViews, not Compose UI -
 * can mirror the exact same theme mode / accent / dynamic-color logic instead of hardcoding colors. */
fun resolveColorScheme(
    themeMode: ThemeMode,
    dynamicColorEnabled: Boolean,
    accentColor: String,
    context: Context,
    systemDark: Boolean
): ColorScheme {
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
        ThemeMode.SYSTEM -> systemDark
    }
    val useDynamicColor = dynamicColorEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val base = when {
        useDynamicColor && isDark -> dynamicDarkColorScheme(context)
        useDynamicColor && !isDark -> dynamicLightColorScheme(context)
        else -> customColorScheme(accentColor, isDark)
    }
    return when {
        themeMode == ThemeMode.AMOLED -> base.amoled()
        !isDark -> base.lightSurfaces()
        else -> base
    }
}

@Composable
fun IAttendTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColorEnabled: Boolean = true,
    accentColor: String = "FOREST",
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val targetColorScheme = resolveColorScheme(themeMode, dynamicColorEnabled, accentColor, context, systemDark)

    MaterialTheme(
        colorScheme = targetColorScheme.animate(),
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

/** Crossfades every color role instead of snapping, so switching theme mode/dynamic color eases in. */
@Composable
private fun ColorScheme.animate(): ColorScheme {
    val spec = Motion.mediumTween<Color>()
    @Composable fun color(value: Color) = animateColorAsState(value, spec, label = "themeColor").value
    return copy(
        primary = color(primary),
        onPrimary = color(onPrimary),
        primaryContainer = color(primaryContainer),
        onPrimaryContainer = color(onPrimaryContainer),
        inversePrimary = color(inversePrimary),
        secondary = color(secondary),
        onSecondary = color(onSecondary),
        secondaryContainer = color(secondaryContainer),
        onSecondaryContainer = color(onSecondaryContainer),
        tertiary = color(tertiary),
        onTertiary = color(onTertiary),
        tertiaryContainer = color(tertiaryContainer),
        onTertiaryContainer = color(onTertiaryContainer),
        background = color(background),
        onBackground = color(onBackground),
        surface = color(surface),
        onSurface = color(onSurface),
        surfaceVariant = color(surfaceVariant),
        onSurfaceVariant = color(onSurfaceVariant),
        surfaceTint = color(surfaceTint),
        inverseSurface = color(inverseSurface),
        inverseOnSurface = color(inverseOnSurface),
        error = color(error),
        onError = color(onError),
        errorContainer = color(errorContainer),
        onErrorContainer = color(onErrorContainer),
        outline = color(outline),
        outlineVariant = color(outlineVariant),
        scrim = color(scrim),
        surfaceBright = color(surfaceBright),
        surfaceDim = color(surfaceDim),
        surfaceContainer = color(surfaceContainer),
        surfaceContainerHigh = color(surfaceContainerHigh),
        surfaceContainerHighest = color(surfaceContainerHighest),
        surfaceContainerLow = color(surfaceContainerLow),
        surfaceContainerLowest = color(surfaceContainerLowest)
    )
}
