package com.iattend.app.core.navigation

import android.provider.Settings
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.navigation.NavHostController
import com.iattend.app.core.ui.hapticClick
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.drawBackdrop
import kotlin.math.hypot
import kotlin.math.roundToInt

private data class ManageAction(val label: String, val icon: ImageVector, val route: Any)

internal fun bloomMenuPosition(anchor: IntRect, window: IntSize, panel: IntSize, margin: Int): IntOffset = IntOffset(
    (anchor.center.x - panel.width / 2).coerceIn(margin, maxOf(margin, window.width - panel.width - margin)),
    (anchor.top - margin / 2 - panel.height).coerceIn(margin, maxOf(margin, window.height - panel.height - margin))
)

/** A single morphing surface; content is clipped rather than stretched with its bounds. */
@Composable
internal fun BloomManageMenu(
    navController: NavHostController,
    triggerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    backdrop: Backdrop? = null,
    glassEffects: BackdropEffectScope.() -> Unit = {},
    trigger: @Composable (onClick: () -> Unit, expanded: Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var mounted by remember { mutableStateOf(false) }
    var pendingRoute by remember { mutableStateOf<Any?>(null) }
    val morph = remember { Animatable(0f) }
    val context = LocalContext.current
    val reduceMotion = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    val actions = remember {
        listOf(
            ManageAction("Subjects", Icons.AutoMirrored.Outlined.MenuBook, SubjectListRoute),
            ManageAction("Timetable versions", Icons.Outlined.Schedule, TimetableVersionListRoute),
            ManageAction("Holidays", Icons.Outlined.BeachAccess, HolidayListRoute),
            ManageAction("Recurring holidays", Icons.Outlined.EventRepeat, RecurringHolidayRoute),
            ManageAction("Extra classes", Icons.Outlined.Add, ExtraClassesRoute()),
            ManageAction("Add test / exam", Icons.Outlined.School, AddAssessmentRoute)
        )
    }
    LaunchedEffect(expanded) {
        if (expanded) {
            morph.animateTo(1f, if (reduceMotion) tween(0) else spring(dampingRatio = 0.92f, stiffness = 333f))
        } else if (mounted) {
            morph.animateTo(0f, tween(if (reduceMotion) 0 else 160, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)))
            mounted = false
            pendingRoute?.let { route ->
                pendingRoute = null
                navController.navigate(route)
            }
        }
    }
    Box {
        // Retain the original FAB's layout slot throughout the morph.
        trigger(hapticClick {
            if (mounted) expanded = false else { mounted = true; expanded = true }
        }, expanded)
        if (mounted) {
            val configuration = LocalConfiguration.current
            val density = LocalDensity.current
            val width = minOf(360.dp, (configuration.screenWidthDp - 32).coerceAtLeast(1).dp)
            val cellHeight = (100 * density.fontScale.coerceAtLeast(1f)).dp
            val height = minOf(56.dp + cellHeight * 2, (configuration.screenHeightDp - 32).coerceAtLeast(1).dp)
            var origin by remember { mutableStateOf(Rect.Zero) }
            var glassPosition by remember { mutableStateOf(IntOffset.Zero) }
            val marginPx = with(density) { 16.dp.roundToPx() }
            val menuSize = with(density) { IntSize(width.roundToPx(), height.roundToPx()) }
            val provider = remember(marginPx, menuSize, backdrop) {
                object : PopupPositionProvider {
                    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
                        val panelSize = if (backdrop != null) menuSize else popupContentSize
                        val (x, y) = bloomMenuPosition(anchorBounds, windowSize, panelSize, marginPx)
                        // Keep the bloom's seed at the panel edge nearest the visible FAB.
                        val seedBottom = minOf(anchorBounds.bottom - y, panelSize.height)
                        origin = Rect((anchorBounds.left - x).toFloat(), (seedBottom - anchorBounds.height).toFloat(),
                            (anchorBounds.right - x).toFloat(), seedBottom.toFloat())
                        glassPosition = IntOffset(x, y)
                        // A full-window glass popup shares the backdrop's window origin, so
                        // blur samples the content beneath the panel rather than the screen's corner.
                        return if (backdrop != null) IntOffset.Zero else IntOffset(x, y)
                    }
                }
            }
            val background = if (backdrop != null) triggerColor else MaterialTheme.colorScheme.primaryContainer
            val foreground = if (backdrop != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimaryContainer
            val divider = foreground.copy(alpha = 0.2f)
            Popup(popupPositionProvider = provider, onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true, clippingEnabled = backdrop == null)) {
                Box(if (backdrop != null) Modifier.fillMaxSize().pointerInput(Unit) {
                    detectTapGestures { expanded = false }
                } else Modifier) {
                Box(
                    Modifier.offset { if (backdrop != null) glassPosition else IntOffset.Zero }
                        .width(width).height(height).semantics { paneTitle = "Manage" }
                        .pointerInput(Unit) { detectTapGestures { /* Keep empty panel space inside the menu. */ } }
                        .drawWithContent {
                            val progress = morph.value.coerceIn(0f, 1f)
                            val source = if (origin == Rect.Zero) Rect(size.width - 64.dp.toPx(), size.height - 64.dp.toPx(), size.width, size.height) else origin
                            val bounds = Rect(
                                source.left * (1 - progress), source.top * (1 - progress),
                                source.right + (size.width - source.right) * progress,
                                source.bottom + (size.height - source.bottom) * progress
                            )
                            val radius = 32.dp.toPx() + (16.dp.toPx() - 32.dp.toPx()) * progress
                            val path = Path().apply { addRoundRect(RoundRect(bounds, CornerRadius(radius))) }
                            clipPath(path) {
                                if (backdrop == null) drawRect(lerp(triggerColor, background, progress))
                                this@drawWithContent.drawContent()
                            }
                        }.then(if (backdrop != null) Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedCornerShape(16.dp) },
                            effects = glassEffects,
                            onDrawSurface = { drawRect(background) }
                        ) else Modifier)
                ) {
                    // The trigger remains crisp inside the shrinking/expanding surface.
                    Icon(Icons.Outlined.Add, contentDescription = null,
                        tint = foreground,
                        modifier = Modifier.offset {
                            IntOffset((origin.center.x - 12.dp.toPx()).roundToInt(), (origin.center.y - 12.dp.toPx()).roundToInt())
                        }.size(24.dp).graphicsLayer { alpha = (1 - morph.value * 3).coerceIn(0f, 1f) })
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        Row(Modifier.fillMaxWidth().height(56.dp).padding(start = 16.dp, end = 4.dp)
                            .graphicsLayer { alpha = ((morph.value - 0.25f) / 0.75f).coerceIn(0f, 1f) },
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Manage", style = MaterialTheme.typography.titleSmall, color = foreground)
                            IconButton(onClick = { expanded = false }, enabled = expanded) {
                                Icon(Icons.Outlined.Close, contentDescription = "Close menu", tint = foreground)
                            }
                        }
                        HorizontalDivider(color = divider)
                        Column(Modifier.drawWithContent {
                            val reveal = if (reduceMotion) 1f else ((morph.value - 0.12f) / 0.88f).coerceIn(0f, 1f)
                            val insetX = size.width * 0.34f * (1 - reveal)
                            val insetY = size.height * 0.45f * (1 - reveal)
                            val path = Path().apply { addRect(Rect(insetX, insetY, size.width - insetX, size.height - insetY)) }
                            clipPath(path) { this@drawWithContent.drawContent() }
                        }) {
                            actions.chunked(3).forEachIndexed { rowIndex, row ->
                                Row(Modifier.fillMaxWidth().height(cellHeight)) {
                                    row.forEachIndexed { colIndex, action ->
                                        if (colIndex > 0) VerticalDivider(color = divider)
                                        val distance = hypot((colIndex - 1).toFloat(), (rowIndex - 0.5f))
                                        val reveal = remember { Animatable(0f) }
                                        LaunchedEffect(expanded, reduceMotion) {
                                            reveal.animateTo(if (expanded) 1f else 0f,
                                                tween(if (reduceMotion) 0 else 180,
                                                    delayMillis = if (reduceMotion || !expanded) 0 else (70 + distance * 55).roundToInt(),
                                                    easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)))
                                        }
                                        Box(Modifier.weight(1f).fillMaxHeight()
                                            .clickable(enabled = expanded, role = Role.Button, onClick = hapticClick {
                                                pendingRoute = action.route
                                                expanded = false
                                            }), contentAlignment = Alignment.Center) {
                                            Column(Modifier.padding(8.dp).graphicsLayer {
                                                alpha = reveal.value
                                                scaleX = 0.85f + 0.15f * reveal.value
                                                scaleY = scaleX
                                                renderEffect = if (Build.VERSION.SDK_INT >= 31 && !reduceMotion && reveal.value < 0.99f) {
                                                    val radius = 6.dp.toPx() * (1 - reveal.value)
                                                    BlurEffect(radius, radius)
                                                } else null
                                            }, horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Icon(action.icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = foreground)
                                                Text(action.label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, color = foreground)
                                            }
                                        }
                                    }
                                }
                                if (rowIndex == 0) HorizontalDivider(color = divider)
                            }
                        }
                    }
                }
                }
            }
        }
    }
}
