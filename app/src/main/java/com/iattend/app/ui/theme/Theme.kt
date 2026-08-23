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
    outline = ForestOutline
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
    outline = ForestOutlineDark
)

// AMOLED scheme: High contrast black background with Dark Forest accents.
private val AmoledColorScheme = DarkColorScheme.copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainer = Color.Black,
    surfaceContainerLow = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerHigh = Color.Black,
    surfaceContainerHighest = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

// 10 Cashiro accents selected (5 Catppuccin + 5 Rosé Pine) for integration spec 7B
val AvailableAccents = listOf("FOREST", "ROSEWATER", "BLUE", "GREEN", "MAUVE", "PEACH", "PINE_ROSE", "PINE_IRIS", "PINE_PINE", "PINE_GOLD", "PINE_FOAM")

private fun getCustomLightScheme(accent: String): ColorScheme = when (accent) {
    "ROSEWATER" -> accentLight(primary = Latte_Rosewater, secondary = Latte_Rosewater_secondary, tertiary = Latte_Rosewater_tertiary, isPine = false)
    "BLUE" -> accentLight(primary = Latte_Blue, secondary = Latte_Blue_secondary, tertiary = Latte_Blue_tertiary, isPine = false)
    "GREEN" -> accentLight(primary = Latte_Green, secondary = Latte_Green_secondary, tertiary = Latte_Green_tertiary, isPine = false)
    "MAUVE" -> accentLight(primary = Latte_Mauve, secondary = Latte_Mauve_secondary, tertiary = Latte_Mauve_tertiary, isPine = false)
    "PEACH" -> accentLight(primary = Latte_Peach, secondary = Latte_Peach_secondary, tertiary = Latte_Peach_tertiary, isPine = false)
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
        surfaceContainer = if (isPine) Dawn_Background else Color(0xFFf9f9ff),
        surfaceContainerHigh = if (isPine) Dawn_SurfaceVariant else Color(0xFFe8e7ee),
        surfaceContainerHighest = if (isPine) Color(0xFFE6DDD5) else Color(0xFFe2e2e9),
        surfaceContainerLow = if (isPine) Dawn_Surface_Base else Color(0xFFffffff),
        surfaceContainerLowest = Color(0xFFf9f9ff)
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
        surfaceContainer = if (isPine) RosePine_Surface_Base else Color(0xFF1e1f25),
        surfaceContainerHigh = if (isPine) RosePine_SurfaceVariant else Color(0xFF282a2f),
        surfaceContainerHighest = if (isPine) Color(0xFF403D52) else Color(0xFF33353a),
        surfaceContainerLow = if (isPine) RosePine_Surface_Base else Color(0xFF1e1f25),
        surfaceContainerLowest = if (isPine) RosePine_Background else Color(0xFF1a1b20)
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

    return when {
        themeMode == ThemeMode.AMOLED -> {
            // AMOLED overrides even accent: black surfaces, but keep accent primary
            val base = if (accentColor == "FOREST") DarkColorScheme else getCustomDarkScheme(accentColor)
            base.copy(
                background = Color.Black,
                surface = Color.Black,
                surfaceContainer = Color.Black,
                surfaceContainerLow = Color.Black,
                surfaceContainerLowest = Color.Black,
                surfaceContainerHigh = Color.Black,
                surfaceContainerHighest = Color.Black,
                onBackground = Color.White,
                onSurface = Color.White
            )
        }
        useDynamicColor && isDark -> dynamicDarkColorScheme(context)
        useDynamicColor && !isDark -> dynamicLightColorScheme(context)
        isDark -> if (accentColor == "FOREST") DarkColorScheme else getCustomDarkScheme(accentColor)
        else -> if (accentColor == "FOREST") LightColorScheme else getCustomLightScheme(accentColor)
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
    return ColorScheme(
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
