package com.iattend.app.core.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.iattend.app.core.datastore.NavBarStyle
import com.iattend.app.core.ui.BrandHeader
import com.iattend.app.core.ui.modalsheet.ModalSheet
import com.iattend.app.core.updates.UpdateAvailableModal
import com.iattend.app.feature.notifications.NotificationSettingsSheetContent
import com.iattend.app.ui.modifiers.BlurDirection
import com.iattend.app.ui.modifiers.progressiveBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import com.iattend.app.feature.assessment.AllAssessmentsScreen
import com.iattend.app.feature.assessment.AssessmentEditorScreen
import com.iattend.app.feature.assessment.AssessmentListScreen
import com.iattend.app.feature.calendar.CalendarScreen
import com.iattend.app.feature.holidays.HolidayListScreen
import com.iattend.app.feature.home.HomeScreen
import com.iattend.app.feature.home.InsightsScreen
import com.iattend.app.feature.onboarding.OnboardingScreen
import com.iattend.app.feature.settings.AboutScreen
import com.iattend.app.feature.settings.AppearanceScreen
import com.iattend.app.feature.settings.LicensesScreen
import com.iattend.app.feature.settings.SettingsScreen
import com.iattend.app.feature.subject.SubjectDetailScreen
import com.iattend.app.feature.subject.SubjectEditorScreen
import com.iattend.app.feature.subject.SubjectListScreen
import com.iattend.app.feature.timetable.BackdatedFillScreen
import com.iattend.app.feature.timetable.ExtraClassesScreen
import com.iattend.app.feature.timetable.ScheduleManagerScreen
import com.iattend.app.feature.timetable.TimetableVersionEditorScreen
import com.iattend.app.feature.timetable.TimetableVersionListScreen
import com.iattend.app.feature.holidays.RecurringHolidayScreen

/**
 * RvSystem-style scaffold:
 * Box(background surfaceContainer) -> Box(background surfaceContainer)
 * -> Surface RoundedCornerShape(top 32dp) holding NavHost.
 * Header is SimpleTopAppBar-style (via BrandHeader) with haze blur 10dp, rendered as a plain
 * overlay (not a Scaffold topBar) so its fade is purely visual and never affects content offset.
 * All root tabs share the rounded card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost(startDestination: Any) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val showBottomBar = backStackEntry?.destination?.let { dest ->
        dest.hasRoute<HomeRoute>() || dest.hasRoute<CalendarRoute>() ||
            dest.hasRoute<TimetableHubRoute>() || dest.hasRoute<SettingsRoute>() || dest.hasRoute<InsightsRoute>()
    } ?: false
    // Single source of truth for which tab the bottom bar highlights, hoisted here (never
    // unmounted) rather than left to each bar's own currentBackStackEntryAsState() subscription -
    // the bars themselves get removed from composition while showBottomBar is false (subpages),
    // so any "last selected" state kept inside them resets to a default every time they remount,
    // flashing the wrong tab. Only updates while genuinely on a tab page, so it holds the correct
    // destination through every subpage visit and is already right the instant a bar reappears.
    var lastTabDestination by remember { mutableStateOf(backStackEntry?.destination) }
    if (showBottomBar) lastTabDestination = backStackEntry?.destination
    val currentTabLabel = backStackEntry?.destination?.let { dest ->
        when {
            dest.hasRoute<HomeRoute>() -> "Home"
            dest.hasRoute<CalendarRoute>() -> "Calendar"
            dest.hasRoute<TimetableHubRoute>() -> "Timetable"
            dest.hasRoute<InsightsRoute>() -> "Insights"
            dest.hasRoute<SettingsRoute>() -> "Settings"
            else -> ""
        }
    } ?: ""

    val hazeState = rememberHazeState()
    val appViewModel = hiltViewModel<AppViewModel>()
    val navBarStyle by appViewModel.navBarStyle.collectAsState()
    val updateAvailableInfo by appViewModel.updateAvailableInfo.collectAsState()
    val scaffoldBackground = MaterialTheme.colorScheme.surfaceContainer
    val backdropBackground = MaterialTheme.colorScheme.background
    val backdrop = rememberLayerBackdrop {
        drawRect(backdropBackground)
        drawContent()
    }

    var showNotificationSheet by remember { mutableStateOf(false) }
    val bottomScrimHeightPx = with(LocalDensity.current) { 140.dp.toPx() }

    // Content offset must track showBottomBar *instantly*, not animate - a Scaffold topBar
    // that fades the header (AnimatedVisibility, alpha-only) keeps the header's full measured
    // height reserved for the whole fade, so a subpage would sit too low for ~150ms and then
    // snap up once the fade finished. Header height is measured once (it's effectively
    // constant) and reused for the instant offset; the header itself is a plain overlay on top
    // of content so its own fade is purely visual and never affects layout.
    var headerHeightPx by remember { mutableStateOf(0) }
    val headerHeightDp = with(LocalDensity.current) { headerHeightPx.toDp() }

    Box(modifier = Modifier.fillMaxSize().background(scaffoldBackground)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = if (showBottomBar) headerHeightDp else 0.dp)
                .background(scaffoldBackground)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier
                            .fillMaxSize()
                            .hazeSource(hazeState)
                            .then(if (navBarStyle == NavBarStyle.CAPSULE) Modifier.layerBackdrop(backdrop) else Modifier)
                            .then(
                                if (showBottomBar) {
                                    Modifier.progressiveBlur(
                                        blurRadius = 40f,
                                        height = bottomScrimHeightPx,
                                        direction = BlurDirection.BOTTOM
                                    )
                                } else Modifier
                            ),
                        // minus-style transition (see AppNavGraph.kt in the minus reference repo): a
                        // small 30px slide + staggered fade. Their forward/backward detection compares
                        // raw NavDestination ids, which only happens to work for their simple enum-like
                        // routes - our type-safe @Serializable routes hash to essentially random ids, so
                        // that comparison would give arbitrary direction per route pair. Using this
                        // NavHost's own enter/exit vs popEnter/popExit slots instead - which already
                        // correctly distinguish push from pop - reproduces the identical visual result
                        // without inheriting that fragile mechanism.
                        enterTransition = {
                            slideInHorizontally(initialOffsetX = { 30 }, animationSpec = tween(300)) +
                                fadeIn(animationSpec = tween(250, delayMillis = 50))
                        },
                        exitTransition = {
                            slideOutHorizontally(targetOffsetX = { -30 }, animationSpec = tween(300)) +
                                fadeOut(animationSpec = tween(200))
                        },
                        popEnterTransition = {
                            slideInHorizontally(initialOffsetX = { -30 }, animationSpec = tween(300)) +
                                fadeIn(animationSpec = tween(250, delayMillis = 50))
                        },
                        popExitTransition = {
                            slideOutHorizontally(targetOffsetX = { 30 }, animationSpec = tween(300)) +
                                fadeOut(animationSpec = tween(200))
                        }
                    ) {
                        composable<OnboardingRoute> {
                            OnboardingScreen(
                                onFinished = { navController.navigate(HomeRoute) { popUpTo(0) } },
                                onFinishedBackdated = { versionId -> navController.navigate(BackdatedFillRoute(versionId)) { popUpTo(0) } }
                            )
                        }
                        composable<HomeRoute> { HomeScreen(onSubjectClick = { id -> navController.navigate(SubjectDetailRoute(id)) }) }
                        composable<SubjectDetailRoute> {
                            SubjectDetailScreen(
                                onBack = { navController.popBackStack() },
                                onEdit = { id -> navController.navigate(SubjectEditorRoute(id)) },
                                onAssessments = { id -> navController.navigate(AssessmentListRoute(id)) }
                            )
                        }
                        composable<AssessmentListRoute> {
                            val route = it.toRoute<AssessmentListRoute>()
                            AssessmentListScreen(
                                onBack = { navController.popBackStack() },
                                onAdd = { navController.navigate(AssessmentEditorRoute(route.subjectId)) },
                                onEdit = { assessmentId -> navController.navigate(AssessmentEditorRoute(route.subjectId, assessmentId)) }
                            )
                        }
                        composable<AssessmentEditorRoute> { AssessmentEditorScreen(onBack = { navController.popBackStack() }) }
                        composable<AllAssessmentsRoute> {
                            AllAssessmentsScreen(
                                onBack = { navController.popBackStack() },
                                onAddForSubject = { subjectId -> navController.navigate(AssessmentEditorRoute(subjectId)) },
                                onEdit = { subjectId, assessmentId -> navController.navigate(AssessmentEditorRoute(subjectId, assessmentId)) }
                            )
                        }
                        composable<CalendarRoute> { CalendarScreen(onAddExtra = { date -> navController.navigate(ExtraClassesRoute(date.toString())) }) }
                        composable<SettingsRoute> {
                            SettingsScreen(
                                onNavigateToAppearance = { navController.navigate(AppearanceRoute) },
                                onNavigateToAbout = { navController.navigate(AboutRoute) }
                            )
                        }
                        composable<AboutRoute> {
                            AboutScreen(
                                onBack = { navController.popBackStack() },
                                onLicenses = { navController.navigate(LicensesRoute) }
                            )
                        }
                        composable<LicensesRoute> { LicensesScreen(onBack = { navController.popBackStack() }) }
                        composable<InsightsRoute> { InsightsScreen() }
                        composable<AppearanceRoute> { AppearanceScreen(onBack = { navController.popBackStack() }) }
                        composable<TimetableHubRoute> {
                            ScheduleManagerScreen(
                                onSubjects = { navController.navigate(SubjectListRoute) },
                                onRecurringHolidays = { navController.navigate(RecurringHolidayRoute) },
                                onManageTimetable = { navController.navigate(TimetableVersionListRoute) },
                                onManageHolidays = { navController.navigate(HolidayListRoute) },
                                onExtraClasses = { navController.navigate(ExtraClassesRoute()) },
                                onExamManager = { navController.navigate(AllAssessmentsRoute) }
                            )
                        }
                        composable<SubjectListRoute> { SubjectListScreen(onBack = { navController.popBackStack() }, onAddSubject = { navController.navigate(SubjectEditorRoute()) }, onEditSubject = { id -> navController.navigate(SubjectEditorRoute(id)) }) }
                        composable<SubjectEditorRoute> { SubjectEditorScreen(onBack = { navController.popBackStack() }) }
                        composable<TimetableVersionListRoute> { TimetableVersionListScreen(onBack = { navController.popBackStack() }, onAddVersion = { navController.navigate(TimetableVersionEditorRoute()) }, onEditVersion = { id -> navController.navigate(TimetableVersionEditorRoute(id)) }) }
                        composable<TimetableVersionEditorRoute> { TimetableVersionEditorScreen(onBack = { navController.popBackStack() }, onBackdatedFill = { versionId -> navController.navigate(BackdatedFillRoute(versionId)) { popUpTo<TimetableVersionListRoute>() } }) }
                        composable<BackdatedFillRoute> { BackdatedFillScreen(onDone = { navController.navigate(HomeRoute) { popUpTo(0) } }) }
                        composable<HolidayListRoute> { HolidayListScreen(onBack = { navController.popBackStack() }) }
                        composable<RecurringHolidayRoute> { RecurringHolidayScreen(onBack = { navController.popBackStack() }) }
                        composable<ExtraClassesRoute> { ExtraClassesScreen(onBack = { navController.popBackStack() }) }
                    }
                }
            }

            if (showBottomBar) {
                when (navBarStyle) {
                    NavBarStyle.PILL -> com.iattend.app.core.navigation.AppBottomBar(navController, destination = lastTabDestination, modifier = Modifier.align(Alignment.BottomCenter))
                    NavBarStyle.CAPSULE -> com.iattend.app.core.navigation.CapsuleBottomBar(navController, backdrop = backdrop, destination = lastTabDestination, modifier = Modifier.align(Alignment.BottomCenter))
                }
            }
        }

        AnimatedVisibility(
            visible = showBottomBar,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(150)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onSizeChanged { headerHeightPx = it.height }
        ) {
            BrandHeader(
                currentTabLabel = currentTabLabel,
                onNotificationClick = { showNotificationSheet = true },
                hazeState = hazeState
            )
        }
    }

    ModalSheet(visible = showNotificationSheet, onVisibleChange = { showNotificationSheet = it }) {
        NotificationSettingsSheetContent(onDismiss = { showNotificationSheet = false })
    }

    updateAvailableInfo?.let { releaseInfo ->
        UpdateAvailableModal(
            releaseInfo = releaseInfo,
            onDismiss = appViewModel::dismissUpdateModal
        )
    }
}
