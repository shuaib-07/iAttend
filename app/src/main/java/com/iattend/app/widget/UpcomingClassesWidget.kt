package com.iattend.app.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.annotation.Keep
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.currentState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.iattend.app.MainActivity
import com.iattend.app.R
import com.iattend.app.core.data.db.ClassOccurrence
import com.iattend.app.core.data.db.OccurrenceSource
import com.iattend.app.core.data.db.OccurrenceStatus
import com.iattend.app.core.data.db.Subject
import com.iattend.app.core.ui.formatTime
import com.iattend.app.ui.theme.resolveColorScheme
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.time.LocalDate

val WIDGET_DAY_OFFSET_KEY = intPreferencesKey("widget_day_offset")
val DAY_OFFSET_PARAM = ActionParameters.Key<Int>("day_offset_param")

@Keep
class UpcomingClassesWidget : GlanceAppWidget() {
    override var stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)

        // Pre-fetch all occurrences and subjects in provideGlance so day navigation is instantaneous
        val occurrencesByDate = entryPoint.classOccurrenceDao().getAllOnce()
            .groupBy { it.date }
        val subjectsById = entryPoint.subjectDao().getAllOnce().associateBy { it.id }

        val settings = entryPoint.settingsRepository().settings.first()
        val systemDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val scheme = resolveColorScheme(settings.themeMode, settings.dynamicColorEnabled, settings.accentColor, context, systemDark)

        val palette = WidgetPalette(
            background = ColorProvider(scheme.surfaceContainer),
            onBackground = ColorProvider(scheme.onSurface),
            onBackgroundVariant = ColorProvider(scheme.onSurfaceVariant),
            buttonBackground = ColorProvider(scheme.surfaceContainerHighest),
            presentBackground = ColorProvider(if (systemDark) Color(0xFF1B382B) else Color(0xFFE8F5E9)),
            presentText = ColorProvider(if (systemDark) Color(0xFF81C784) else Color(0xFF2E7D32)),
            absentBackground = ColorProvider(if (systemDark) Color(0xFF381C22) else Color(0xFFFFEBEE)),
            absentText = ColorProvider(if (systemDark) Color(0xFFEF9A9A) else Color(0xFFC62828)),
            cancelledBackground = ColorProvider(if (systemDark) Color(0xFF26242E) else Color(0xFFF5F5F5)),
            cancelledText = ColorProvider(if (systemDark) Color(0xFF9E9E9E) else Color(0xFF757575)),
            extraBadgeBg = ColorProvider(if (systemDark) Color(0xFF4A3416) else Color(0xFFFFE0B2)),
            extraBadgeText = ColorProvider(if (systemDark) Color(0xFFFFB74D) else Color(0xFFE65100))
        )

        provideContent {
            val prefs = currentState<Preferences>()
            val dayOffset = prefs[WIDGET_DAY_OFFSET_KEY] ?: 0
            val today = LocalDate.now()
            val targetDate = today.plusDays(dayOffset.toLong())
            val occurrences = occurrencesByDate[targetDate].orEmpty()
                .sortedWith(compareBy({ it.startTime }, { it.id }))

            WidgetContent(targetDate, today, dayOffset, occurrences, subjectsById, palette)
        }
    }
}

@Keep
class ChangeDayActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val targetOffset = parameters[DAY_OFFSET_PARAM] ?: 0
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[WIDGET_DAY_OFFSET_KEY] = targetOffset
        }
        UpcomingClassesWidget().update(context, glanceId)
    }
}

private data class WidgetPalette(
    val background: ColorProvider,
    val onBackground: ColorProvider,
    val onBackgroundVariant: ColorProvider,
    val buttonBackground: ColorProvider,
    val presentBackground: ColorProvider,
    val presentText: ColorProvider,
    val absentBackground: ColorProvider,
    val absentText: ColorProvider,
    val cancelledBackground: ColorProvider,
    val cancelledText: ColorProvider,
    val extraBadgeBg: ColorProvider,
    val extraBadgeText: ColorProvider
)

private fun formatWidgetDateTitle(targetDate: LocalDate, today: LocalDate): String {
    val dayOfMonth = targetDate.dayOfMonth
    val suffix = when {
        dayOfMonth in 11..13 -> "th"
        dayOfMonth % 10 == 1 -> "st"
        dayOfMonth % 10 == 2 -> "nd"
        dayOfMonth % 10 == 3 -> "rd"
        else -> "th"
    }
    val monthStr = targetDate.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    val formattedDate = "${dayOfMonth}${suffix} $monthStr ${targetDate.year}"

    return when (targetDate) {
        today -> "Today • $formattedDate"
        today.plusDays(1) -> "Tomorrow • $formattedDate"
        today.minusDays(1) -> "Yesterday • $formattedDate"
        else -> "${targetDate.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} • $formattedDate"
    }
}

@Composable
private fun WidgetContent(
    targetDate: LocalDate,
    today: LocalDate,
    dayOffset: Int,
    occurrences: List<ClassOccurrence>,
    subjectsById: Map<Long, Subject>,
    palette: WidgetPalette
) {
    val context = LocalContext.current
    val openAppIntent = actionStartActivity(Intent(context, MainActivity::class.java))

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(palette.background)
            .cornerRadius(20.dp)
            .padding(12.dp)
    ) {
        // Header Row: Date Title on left, Left/Right arrow navigation on right
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val titleText = formatWidgetDateTitle(targetDate, today)
            Text(
                text = titleText,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = palette.onBackground
                ),
                modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(
                        if (dayOffset != 0)
                            actionRunCallback<ChangeDayActionCallback>(actionParametersOf(DAY_OFFSET_PARAM to 0))
                        else
                            openAppIntent
                    )
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Previous Day Arrow
                Box(
                    modifier = GlanceModifier
                        .size(32.dp)
                        .background(palette.buttonBackground)
                        .cornerRadius(16.dp)
                        .clickable(actionRunCallback<ChangeDayActionCallback>(actionParametersOf(DAY_OFFSET_PARAM to dayOffset - 1))),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_chevron_left),
                        contentDescription = "Previous Day",
                        modifier = GlanceModifier.size(18.dp),
                        colorFilter = ColorFilter.tint(palette.onBackground)
                    )
                }

                Spacer(modifier = GlanceModifier.width(8.dp))

                // Next Day Arrow
                Box(
                    modifier = GlanceModifier
                        .size(32.dp)
                        .background(palette.buttonBackground)
                        .cornerRadius(16.dp)
                        .clickable(actionRunCallback<ChangeDayActionCallback>(actionParametersOf(DAY_OFFSET_PARAM to dayOffset + 1))),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_chevron_right),
                        contentDescription = "Next Day",
                        modifier = GlanceModifier.size(18.dp),
                        colorFilter = ColorFilter.tint(palette.onBackground)
                    )
                }
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        if (occurrences.isEmpty()) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .clickable(openAppIntent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No classes scheduled",
                    style = TextStyle(fontSize = 12.sp, color = palette.onBackgroundVariant)
                )
            }
        } else {
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .clickable(openAppIntent)
            ) {
                occurrences.take(7).forEach { occurrence ->
                    ClassRow(occurrence, subjectsById[occurrence.subjectId], palette)
                    Spacer(modifier = GlanceModifier.height(5.dp))
                }
            }
        }
    }
}

@Composable
private fun ClassRow(occurrence: ClassOccurrence, subject: Subject?, palette: WidgetPalette) {
    val isPresent = occurrence.status == OccurrenceStatus.PRESENT
    val isAbsent = occurrence.status == OccurrenceStatus.ABSENT
    val isCancelled = occurrence.status == OccurrenceStatus.CANCELLED
    val isExtra = occurrence.source == OccurrenceSource.EXTRA

    val rowModifier = when {
        isPresent -> GlanceModifier
            .fillMaxWidth()
            .background(palette.presentBackground)
            .cornerRadius(10.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp)
        isAbsent -> GlanceModifier
            .fillMaxWidth()
            .background(palette.absentBackground)
            .cornerRadius(10.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp)
        isCancelled -> GlanceModifier
            .fillMaxWidth()
            .background(palette.cancelledBackground)
            .cornerRadius(10.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp)
        else -> GlanceModifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
    }

    Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
        // Subject Color Swatch with ClassType Letter
        Row(
            modifier = GlanceModifier
                .size(20.dp)
                .background(Color(subject?.colorArgb ?: 0xFF888888.toInt()))
                .cornerRadius(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                occurrence.classType.letter,
                style = TextStyle(color = ColorProvider(Color.White), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = GlanceModifier.width(8.dp))

        Column(modifier = GlanceModifier.defaultWeight()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    subject?.name ?: "?",
                    style = TextStyle(
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = if (isCancelled) palette.cancelledText else palette.onBackground,
                        textDecoration = if (isCancelled) TextDecoration.LineThrough else TextDecoration.None
                    )
                )
                if (isExtra) {
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Box(
                        modifier = GlanceModifier
                            .background(palette.extraBadgeBg)
                            .cornerRadius(4.dp)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            "EXTRA",
                            style = TextStyle(
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.extraBadgeText
                            )
                        )
                    }
                }
            }

            val timeRange = occurrence.startTime?.let { start ->
                formatTime(start) + (occurrence.endTime?.let { " - ${formatTime(it)}" } ?: "")
            } ?: ""
            val subtitle = timeRange + (subject?.teacherName?.let { " • $it" } ?: "")
            Text(
                subtitle,
                style = TextStyle(
                    fontSize = 11.sp,
                    color = if (isCancelled) palette.cancelledText else palette.onBackgroundVariant,
                    textDecoration = if (isCancelled) TextDecoration.LineThrough else TextDecoration.None
                )
            )
        }

        // Status Indicators on the trailing side
        if (isPresent) {
            Spacer(modifier = GlanceModifier.width(4.dp))
            Text(
                "✓",
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.presentText
                )
            )
        } else if (isAbsent) {
            Spacer(modifier = GlanceModifier.width(4.dp))
            Text(
                "✗",
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.absentText
                )
            )
        } else if (isCancelled) {
            Spacer(modifier = GlanceModifier.width(4.dp))
            Text(
                "Off",
                style = TextStyle(
                    fontSize = 10.sp,
                    color = palette.cancelledText
                )
            )
        }
    }
}
