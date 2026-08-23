@file:Suppress("FunctionName")

package com.iattend.app.core.ui.modalsheet

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.iattend.app.ui.theme.SquircleShape
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * A right-anchored companion to [ModalSheetLayout]/[ModalSheet], built on the same ported
 * SwipeableV2State engine (already orientation-agnostic) and the same [Scrim]. Two states only
 * (Hidden/Expanded) - a fixed-width side panel doesn't have a meaningful "half expanded" the way
 * a bottom sheet does.
 */
@ExperimentalMaterial3Api
enum class SideSheetValue { Hidden, Expanded }

@ExperimentalMaterial3Api
@OptIn(ExperimentalMaterial3Api::class)
class SideSheetState(
    initialValue: SideSheetValue,
    animationSpec: AnimationSpec<Float> = SwipeableV2Defaults.AnimationSpec,
    confirmValueChange: (SideSheetValue) -> Boolean = { true }
) {
    val swipeableState = SwipeableV2State(
        initialValue = initialValue,
        animationSpec = animationSpec,
        confirmValueChange = confirmValueChange,
        positionalThreshold = { 56.dp.toPx() },
        velocityThreshold = 125.dp
    )

    val currentValue: SideSheetValue get() = swipeableState.currentValue
    val isVisible: Boolean get() = swipeableState.currentValue != SideSheetValue.Hidden

    suspend fun show() = swipeableState.animateTo(SideSheetValue.Expanded)
    suspend fun hide() = swipeableState.animateTo(SideSheetValue.Hidden)

    companion object {
        fun Saver(
            animationSpec: AnimationSpec<Float>,
            confirmValueChange: (SideSheetValue) -> Boolean,
        ): Saver<SideSheetState, SideSheetValue> = Saver(
            save = { it.currentValue },
            restore = { SideSheetState(it, animationSpec, confirmValueChange) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalMaterial3Api
@Composable
fun rememberSideSheetState(
    initialValue: SideSheetValue = SideSheetValue.Hidden,
    animationSpec: AnimationSpec<Float> = SwipeableV2Defaults.AnimationSpec,
    confirmValueChange: (SideSheetValue) -> Boolean = { true },
): SideSheetState = rememberSaveable(
    saver = SideSheetState.Saver(animationSpec, confirmValueChange)
) {
    SideSheetState(initialValue, animationSpec, confirmValueChange)
}

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalMaterial3Api
@Composable
fun SideSheetLayout(
    sheetContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SideSheetState = rememberSideSheetState(),
    sheetWidth: Dp = 360.dp,
    sheetShape: Shape = SquircleShape(32.dp),
    sheetElevation: Dp = BottomSheetDefaults.Elevation,
    sheetContainerColor: Color = BottomSheetDefaults.ContainerColor,
    sheetContentColor: Color = contentColorFor(sheetContainerColor),
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    Box(modifier.fillMaxSize()) {
        content()
        Scrim(
            color = scrimColor,
            onDismiss = {
                if (sheetState.swipeableState.confirmValueChange(SideSheetValue.Hidden)) {
                    scope.launch { sheetState.hide() }
                }
            },
            visible = sheetState.swipeableState.targetValue != SideSheetValue.Hidden
        )
        Surface(
            Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(sheetWidth)
                .offset {
                    IntOffset(sheetState.swipeableState.requireOffset().roundToInt(), 0)
                }
                .swipeableV2(
                    state = sheetState.swipeableState,
                    orientation = Orientation.Horizontal,
                    enabled = sheetState.swipeableState.currentValue != SideSheetValue.Hidden,
                )
                .swipeAnchors(
                    state = sheetState.swipeableState,
                    possibleValues = setOf(SideSheetValue.Hidden, SideSheetValue.Expanded)
                ) { state, sheetSize ->
                    when (state) {
                        SideSheetValue.Hidden -> sheetSize.width.toFloat()
                        SideSheetValue.Expanded -> 0f
                    }
                }
                .semantics {
                    if (sheetState.isVisible) {
                        dismiss {
                            if (sheetState.swipeableState.confirmValueChange(SideSheetValue.Hidden)) {
                                scope.launch { sheetState.hide() }
                            }
                            true
                        }
                    }
                },
            shape = sheetShape,
            tonalElevation = sheetElevation,
            color = sheetContainerColor,
            contentColor = sheetContentColor
        ) {
            Column(
                modifier = Modifier.fillMaxHeight().statusBarsPadding().navigationBarsPadding().padding(16.dp),
                content = sheetContent
            )
        }
    }
}

/**
 * Simple visible/onVisibleChange entry point, mirroring [ModalSheet]'s ergonomics.
 */
@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalMaterial3Api
@Composable
fun SideSheet(
    visible: Boolean,
    onVisibleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    cancelable: Boolean = true,
    sheetWidth: Dp = 360.dp,
    shape: Shape = SquircleShape(32.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberSideSheetState(
        confirmValueChange = {
            if (it == SideSheetValue.Hidden && !cancelable) {
                return@rememberSideSheetState false
            }
            onVisibleChange(it == SideSheetValue.Expanded)
            true
        }
    )

    LaunchedEffect(visible) {
        if (visible) sheetState.show() else sheetState.hide()
    }

    if (!visible && sheetState.currentValue == SideSheetValue.Hidden && !sheetState.isVisible) {
        return
    }

    FullscreenPopup(
        onDismiss = if (cancelable) {
            { onVisibleChange(false) }
        } else null
    ) {
        SideSheetLayout(
            modifier = modifier,
            sheetState = sheetState,
            sheetWidth = sheetWidth,
            sheetShape = shape,
            sheetContent = content,
            content = {}
        )
    }
}
