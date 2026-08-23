package com.iattend.app.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.iattend.app.MainActivity
import com.iattend.app.core.data.db.ClassOccurrence
import com.iattend.app.core.data.db.OccurrenceStatus
import com.iattend.app.core.data.db.Subject
import com.iattend.app.core.ui.formatTime
import com.iattend.app.ui.theme.resolveColorScheme
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime

/** Reads today's remaining classes (falling back to tomorrow's once today's are all over) and
 * shows them read-only - tapping anywhere opens the app, where marking already has a full UI.
 * Colors mirror the app's own theme setting (mode/accent/dynamic color) via [resolveColorScheme] -
 * see that function's doc for why it's factored out as a plain, non-@Composable function.
 * ponytail: Android's periodic-update floor is 30min (see the widget info xml), plus an explicit
 * updateAll() call from NotificationActionReceiver/MidnightRefreshReceiver/theme-setting changes -
 * in-app marks (not via notification) only catch up on the next periodic tick, not instantly. Wire
 * updateAll() into the ViewModels' own mark-status calls too if that staleness turns out to matter.
 */
class UpcomingClassesWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val today = LocalDate.now()

        val todayRemaining = entryPoint.classOccurrenceDao().getForDateOnce(today)
            .filter { it.endTime == null || it.endTime.isAfter(LocalTime.now()) }
        val (listDate, occurrences) = if (todayRemaining.isNotEmpty()) {
            today to todayRemaining
        } else {
            val tomorrow = today.plusDays(1)
            tomorrow to entryPoint.classOccurrenceDao().getForDateOnce(tomorrow)
        }
        val subjectsById = entryPoint.subjectDao().getAllOnce().associateBy { it.id }

        val settings = entryPoint.settingsRepository().settings.first()
        val systemDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val scheme = resolveColorScheme(settings.themeMode, settings.dynamicColorEnabled, settings.accentColor, context, systemDark)
        val palette = WidgetPalette(
            background = ColorProvider(scheme.surfaceContainer),
            onBackground = ColorProvider(scheme.onSurface),
            onBackgroundVariant = ColorProvider(scheme.onSurfaceVariant)
        )

        provideContent {
            WidgetContent(listDate, occurrences.take(8), subjectsById, palette)
        }
    }
}

private data class WidgetPalette(val background: ColorProvider, val onBackground: ColorProvider, val onBackgroundVariant: ColorProvider)

@Composable
private fun WidgetContent(listDate: LocalDate, occurrences: List<ClassOccurrence>, subjectsById: Map<Long, Subject>, palette: WidgetPalette) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(palette.background)
            .cornerRadius(20.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
            .padding(12.dp)
    ) {
        Text(
            if (listDate == LocalDate.now()) "Today" else "Tomorrow",
            style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = palette.onBackground)
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        if (occurrences.isEmpty()) {
            Text("No more classes", style = TextStyle(color = palette.onBackgroundVariant))
        } else {
            occurrences.forEach { occurrence ->
                ClassRow(occurrence, subjectsById[occurrence.subjectId], palette)
                Spacer(modifier = GlanceModifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun ClassRow(occurrence: ClassOccurrence, subject: Subject?, palette: WidgetPalette) {
    val cancelled = occurrence.status == OccurrenceStatus.CANCELLED
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = GlanceModifier
                .size(20.dp)
                .background(Color(subject?.colorArgb ?: 0xFF888888.toInt()))
                .cornerRadius(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(occurrence.classType.letter, style = TextStyle(color = ColorProvider(Color.White), fontSize = 11.sp))
        }
        Spacer(modifier = GlanceModifier.width(8.dp))
        Column {
            Text(
                subject?.name ?: "?",
                style = TextStyle(
                    fontWeight = FontWeight.Medium,
                    color = palette.onBackground,
                    textDecoration = if (cancelled) TextDecoration.LineThrough else TextDecoration.None
                )
            )
            val timeRange = occurrence.startTime?.let { start ->
                formatTime(start) + (occurrence.endTime?.let { " - ${formatTime(it)}" } ?: "")
            } ?: ""
            Text(
                timeRange + (subject?.teacherName?.let { "  $it" } ?: ""),
                style = TextStyle(fontSize = 12.sp, color = palette.onBackgroundVariant)
            )
        }
    }
}
