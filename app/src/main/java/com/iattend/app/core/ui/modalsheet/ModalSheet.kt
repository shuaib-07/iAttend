package com.iattend.app.core.ui.modalsheet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.iattend.app.ui.theme.SquircleShape

/**
 * Adapted from T8RIN/ModalSheet (com.t8rin.modalsheet). Modal sheet that behaves like a bottom
 * sheet and draws over system UI (via [FullscreenPopup], see its doc for why). Should be used
 * with content that isn't dependent on outer data; for dynamic content use the [ModalSheetState]
 * overload below.
 *
 * @param visible True if the modal should be visible.
 * @param onVisibleChange Called when visibility changes.
 * @param cancelable When true, this modal sheet can be closed with a swipe gesture, a tap on the
 * scrim, or the hardware back button.
 * @param shape The shape of the bottom sheet. Defaults to this app's SquircleShape at the
 * extraLarge/32dp role (design.md).
 */
@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalMaterial3Api
@Composable
fun ModalSheet(
    visible: Boolean,
    onVisibleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    sheetModifier: Modifier = Modifier,
    cancelable: Boolean = true,
    skipHalfExpanded: Boolean = true,
    shape: Shape = SquircleShape(32.dp),
    elevation: Dp = BottomSheetDefaults.Elevation,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(
        skipHalfExpanded = skipHalfExpanded,
        initialValue = ModalBottomSheetValue.Hidden,
        confirmValueChange = {
            if (it == ModalBottomSheetValue.Hidden && !cancelable) {
                return@rememberModalBottomSheetState false
            }
            onVisibleChange(it == ModalBottomSheetValue.Expanded)
            true
        },
    )

    LaunchedEffect(visible) {
        if (visible) {
            sheetState.show()
        } else {
            sheetState.hide()
        }
    }

    if (!visible && sheetState.currentValue == sheetState.targetValue && !sheetState.isVisible) {
        return
    }

    ModalSheet(
        sheetState = sheetState,
        onDismiss = {
            if (cancelable) {
                onVisibleChange(false)
            }
        },
        sheetModifier = sheetModifier,
        modifier = modifier,
        shape = shape,
        elevation = elevation,
        containerColor = containerColor,
        contentColor = contentColor,
        scrimColor = scrimColor,
        content = content,
    )
}

/**
 * Modal sheet that takes [ModalSheetState] directly, to fine-tune sheet behavior.
 *
 * @param onDismiss Called when the user taps the scrim/back button.
 */
@ExperimentalMaterial3Api
@Composable
fun ModalSheet(
    modifier: Modifier = Modifier,
    sheetModifier: Modifier = Modifier,
    sheetState: ModalSheetState,
    onDismiss: (() -> Unit)?,
    shape: Shape = SquircleShape(32.dp),
    elevation: Dp = BottomSheetDefaults.Elevation,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    content: @Composable ColumnScope.() -> Unit,
) {
    FullscreenPopup(
        onDismiss = onDismiss,
    ) {
        Box(Modifier.fillMaxSize()) {
            ModalSheetLayout(
                sheetModifier = sheetModifier,
                modifier = modifier.align(Alignment.BottomCenter),
                sheetState = sheetState,
                sheetShape = shape,
                sheetElevation = elevation,
                sheetContainerColor = containerColor,
                sheetContentColor = contentColor,
                scrimColor = scrimColor,
                sheetContent = content,
                content = {}
            )
        }
    }
}
