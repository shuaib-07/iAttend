package com.iattend.app.core.ui

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieAnimatable
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty

/**
 * One-shot morph icon (IconScout "hover" pack: plays 0->1 forward) that only actually plays on a
 * real tap-driven selection change, not on every recomposition/remount. Nav bar tabs get removed
 * from composition while on a subpage (see AppNavHost's showBottomBar), so a naive "animate
 * whenever selected" approach replayed the whole animation every time the bar remounted, even
 * though nothing was actually tapped. Instead: detect selected flipping false->true *after* this
 * instance is already composed (a genuine tap) and only animate that case; every other case
 * (fresh mount already selected, or selected turning false) snaps instantly with no animation.
 *
 * Recolored uniformly to [tint] via a dynamic color filter, since the source files bake in a
 * fixed near-black fill/stroke that wouldn't track the app's selected/unselected or theme colors.
 *
 * [assetPath] is relative to app/src/main/assets, e.g. "lottie/home.json".
 */
@Composable
fun LottieMorphIcon(
    assetPath: String,
    selected: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    contentDescription: String? = null
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset(assetPath))
    val animatable = rememberLottieAnimatable()
    var previousSelected by remember { mutableStateOf(selected) }

    LaunchedEffect(selected, composition) {
        val comp = composition ?: return@LaunchedEffect
        when {
            selected && !previousSelected -> animatable.animate(comp, iterations = 1, speed = 1f)
            selected -> animatable.snapTo(comp, progress = 1f)
            else -> animatable.snapTo(comp, progress = 0f)
        }
        previousSelected = selected
    }

    val dynamicProperties = rememberLottieDynamicProperties(
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR_FILTER,
            value = PorterDuffColorFilter(tint.toArgb(), PorterDuff.Mode.SRC_ATOP),
            "**"
        )
    )
    LottieAnimation(
        composition = composition,
        progress = { animatable.progress },
        dynamicProperties = dynamicProperties,
        modifier = modifier
            .size(size)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                }
            )
    )
}
