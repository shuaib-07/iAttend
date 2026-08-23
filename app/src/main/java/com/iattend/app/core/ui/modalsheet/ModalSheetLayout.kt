@file:Suppress("FunctionName")

package com.iattend.app.core.ui.modalsheet

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.iattend.app.ui.theme.SquircleShape
import com.iattend.app.core.ui.modalsheet.ModalBottomSheetValue.Expanded
import com.iattend.app.core.ui.modalsheet.ModalBottomSheetValue.HalfExpanded
import com.iattend.app.core.ui.modalsheet.ModalBottomSheetValue.Hidden
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Adapted from T8RIN/ModalSheet (com.t8rin.modalsheet). Two changes from the original:
 * Surface/contentColorFor now come from Material3 instead of the (unused-elsewhere-in-this-app)
 * Material2 library, and the default sheet shape is this app's own SquircleShape at the
 * extraLarge/32dp role (design.md) instead of BottomSheetDefaults.ExpandedShape.
 */
@ExperimentalMaterial3Api
enum class ModalBottomSheetValue {
    Hidden,
    Expanded,
    HalfExpanded
}

@ExperimentalMaterial3Api
@OptIn(ExperimentalMaterial3Api::class)
class ModalSheetState(
    initialValue: ModalBottomSheetValue,
    animationSpec: AnimationSpec<Float> = SwipeableV2Defaults.AnimationSpec,
    confirmValueChange: (ModalBottomSheetValue) -> Boolean = { true },
    val isSkipHalfExpanded: Boolean = false
) {

    val swipeableState = SwipeableV2State(
        initialValue = initialValue,
        animationSpec = animationSpec,
        confirmValueChange = confirmValueChange,
        positionalThreshold = PositionalThreshold,
        velocityThreshold = VelocityThreshold
    )

    val currentValue: ModalBottomSheetValue
        get() = swipeableState.currentValue

    val targetValue: ModalBottomSheetValue
        get() = swipeableState.targetValue

    val isVisible: Boolean
        get() = swipeableState.currentValue != Hidden

    val hasHalfExpandedState: Boolean
        get() = swipeableState.hasAnchorForValue(HalfExpanded)

    init {
        if (isSkipHalfExpanded) {
            require(initialValue != HalfExpanded) {
                "The initial value must not be set to HalfExpanded if skipHalfExpanded is set to" +
                        " true."
            }
        }
    }

    suspend fun show() {
        val targetValue = when {
            hasHalfExpandedState -> HalfExpanded
            else -> Expanded
        }
        animateTo(targetValue)
    }

    suspend fun halfExpand() {
        if (!hasHalfExpandedState) {
            return
        }
        animateTo(HalfExpanded)
    }

    suspend fun expand() {
        if (!swipeableState.hasAnchorForValue(Expanded)) {
            return
        }
        animateTo(Expanded)
    }

    suspend fun hide() = animateTo(Hidden)

    suspend fun animateTo(
        target: ModalBottomSheetValue,
        velocity: Float = swipeableState.lastVelocity
    ) = swipeableState.animateTo(target, velocity)

    suspend fun snapTo(target: ModalBottomSheetValue) = swipeableState.snapTo(target)

    val lastVelocity: Float get() = swipeableState.lastVelocity

    val isAnimationRunning: Boolean get() = swipeableState.isAnimationRunning

    companion object {
        fun Saver(
            animationSpec: AnimationSpec<Float>,
            confirmValueChange: (ModalBottomSheetValue) -> Boolean,
            skipHalfExpanded: Boolean,
        ): Saver<ModalSheetState, *> = Saver(
            save = { it.currentValue },
            restore = {
                ModalSheetState(
                    initialValue = it,
                    animationSpec = animationSpec,
                    isSkipHalfExpanded = skipHalfExpanded,
                    confirmValueChange = confirmValueChange
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalMaterial3Api
@Composable
fun rememberModalBottomSheetState(
    initialValue: ModalBottomSheetValue,
    animationSpec: AnimationSpec<Float> = SwipeableV2Defaults.AnimationSpec,
    confirmValueChange: (ModalBottomSheetValue) -> Boolean = { true },
    skipHalfExpanded: Boolean = false,
): ModalSheetState {
    return key(initialValue) {
        rememberSaveable(
            initialValue, animationSpec, skipHalfExpanded, confirmValueChange,
            saver = ModalSheetState.Saver(
                animationSpec = animationSpec,
                skipHalfExpanded = skipHalfExpanded,
                confirmValueChange = confirmValueChange
            )
        ) {
            ModalSheetState(
                initialValue = initialValue,
                animationSpec = animationSpec,
                isSkipHalfExpanded = skipHalfExpanded,
                confirmValueChange = confirmValueChange
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalMaterial3Api
@Composable
fun ModalSheetLayout(
    sheetContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    sheetModifier: Modifier = Modifier,
    sheetState: ModalSheetState = rememberModalBottomSheetState(Hidden),
    sheetShape: Shape = SquircleShape(32.dp),
    sheetElevation: Dp = BottomSheetDefaults.Elevation,
    sheetContainerColor: Color = BottomSheetDefaults.ContainerColor,
    sheetContentColor: Color = contentColorFor(sheetContainerColor),
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val orientation = Orientation.Vertical
    val anchorChangeHandler = remember(sheetState, scope) {
        ModalBottomSheetAnchorChangeHandler(
            state = sheetState,
            animateTo = { target, velocity ->
                scope.launch { sheetState.animateTo(target, velocity = velocity) }
            },
            snapTo = { target -> scope.launch { sheetState.snapTo(target) } }
        )
    }
    BoxWithConstraints(modifier) {
        val fullHeight = constraints.maxHeight.toFloat()
        Box(Modifier.fillMaxSize()) {
            content()
            Scrim(
                color = scrimColor,
                onDismiss = {
                    if (sheetState.swipeableState.confirmValueChange(Hidden)) {
                        scope.launch { sheetState.hide() }
                    }
                },
                visible = sheetState.swipeableState.targetValue != Hidden
            )
        }
        Surface(
            Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = MaxModalBottomSheetWidth)
                .fillMaxWidth()
                .nestedScroll(
                    remember(sheetState.swipeableState, orientation) {
                        ConsumeSwipeWithinBottomSheetBoundsNestedScrollConnection(
                            state = sheetState.swipeableState,
                            orientation = orientation
                        )
                    }
                )
                .offset {
                    IntOffset(
                        0,
                        sheetState.swipeableState
                            .requireOffset()
                            .roundToInt()
                    )
                }
                .swipeableV2(
                    state = sheetState.swipeableState,
                    orientation = orientation,
                    enabled = sheetState.swipeableState.currentValue != Hidden,
                )
                .swipeAnchors(
                    state = sheetState.swipeableState,
                    possibleValues = setOf(Hidden, HalfExpanded, Expanded),
                    anchorChangeHandler = anchorChangeHandler
                ) { state, sheetSize ->
                    when (state) {
                        Hidden -> fullHeight
                        HalfExpanded -> when {
                            sheetSize.height < fullHeight / 2f -> null
                            sheetState.isSkipHalfExpanded -> null
                            else -> fullHeight / 2f
                        }

                        Expanded -> if (sheetSize.height != 0) {
                            max(0f, fullHeight - sheetSize.height)
                        } else null
                    }
                }
                .semantics {
                    if (sheetState.isVisible) {
                        dismiss {
                            if (sheetState.swipeableState.confirmValueChange(Hidden)) {
                                scope.launch { sheetState.hide() }
                            }
                            true
                        }
                        if (sheetState.swipeableState.currentValue == HalfExpanded) {
                            expand {
                                if (sheetState.swipeableState.confirmValueChange(Expanded)) {
                                    scope.launch { sheetState.expand() }
                                }
                                true
                            }
                        } else if (sheetState.hasHalfExpandedState) {
                            collapse {
                                if (sheetState.swipeableState.confirmValueChange(HalfExpanded)) {
                                    scope.launch { sheetState.halfExpand() }
                                }
                                true
                            }
                        }
                    }
                }
                .then(sheetModifier),
            shape = sheetShape,
            tonalElevation = sheetElevation,
            color = sheetContainerColor,
            contentColor = sheetContentColor
        ) {
            Column(
                modifier = Modifier.navigationBarsPadding().padding(bottom = 16.dp),
                content = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        BottomSheetDefaults.DragHandle()
                    }
                    sheetContent()
                }
            )
        }
    }
}

@Composable
internal fun Scrim(
    color: Color,
    onDismiss: () -> Unit,
    visible: Boolean
) {
    if (color.isSpecified) {
        val alpha by animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = TweenSpec()
        )
        val dismissModifier = if (visible) {
            Modifier
                .pointerInput(onDismiss) { detectTapGestures { onDismiss() } }
                .semantics(mergeDescendants = true) {
                    onClick { onDismiss(); true }
                }
        } else {
            Modifier
        }

        Canvas(
            Modifier
                .fillMaxSize()
                .then(dismissModifier)
        ) {
            drawRect(color = color, alpha = alpha)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun ConsumeSwipeWithinBottomSheetBoundsNestedScrollConnection(
    state: SwipeableV2State<*>,
    orientation: Orientation
): NestedScrollConnection = object : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val delta = available.toFloat()
        return if (delta < 0 && source == NestedScrollSource.UserInput) {
            state.dispatchRawDelta(delta).toOffset()
        } else {
            Offset.Zero
        }
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        return if (source == NestedScrollSource.UserInput) {
            state.dispatchRawDelta(available.toFloat()).toOffset()
        } else {
            Offset.Zero
        }
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val toFling = available.toFloat()
        val currentOffset = state.requireOffset()
        return if (toFling < 0 && currentOffset > state.minOffset) {
            state.settle(velocity = toFling)
            available
        } else {
            Velocity.Zero
        }
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        state.settle(velocity = available.toFloat())
        return available
    }

    private fun Float.toOffset(): Offset = Offset(
        x = if (orientation == Orientation.Horizontal) this else 0f,
        y = if (orientation == Orientation.Vertical) this else 0f
    )

    @JvmName("velocityToFloat")
    private fun Velocity.toFloat() = if (orientation == Orientation.Horizontal) x else y

    @JvmName("offsetToFloat")
    private fun Offset.toFloat(): Float = if (orientation == Orientation.Horizontal) x else y
}

@ExperimentalMaterial3Api
@OptIn(ExperimentalMaterial3Api::class)
private fun ModalBottomSheetAnchorChangeHandler(
    state: ModalSheetState,
    animateTo: (target: ModalBottomSheetValue, velocity: Float) -> Unit,
    snapTo: (target: ModalBottomSheetValue) -> Unit,
) = AnchorChangeHandler<ModalBottomSheetValue> { previousTarget, previousAnchors, newAnchors ->
    val previousTargetOffset = previousAnchors[previousTarget]
    val newTarget = when (previousTarget) {
        Hidden -> Hidden
        HalfExpanded, Expanded -> {
            val hasHalfExpandedState = newAnchors.containsKey(HalfExpanded)
            val newTarget = if (hasHalfExpandedState) HalfExpanded
            else if (newAnchors.containsKey(Expanded)) Expanded else Hidden
            newTarget
        }
    }
    val newTargetOffset = newAnchors.getValue(newTarget)
    if (newTargetOffset != previousTargetOffset) {
        if (state.isAnimationRunning) {
            animateTo(newTarget, state.lastVelocity)
        } else {
            snapTo(newTarget)
        }
    }
}

private val PositionalThreshold: Density.(Float) -> Float = { 56.dp.toPx() }
private val VelocityThreshold = 125.dp
private val MaxModalBottomSheetWidth = 640.dp
