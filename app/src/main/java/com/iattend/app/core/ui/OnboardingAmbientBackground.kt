package com.iattend.app.core.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import kotlinx.coroutines.delay

private val ambientIllustrations = listOf(
    "lottie/Student.lottie",
    "lottie/Girl Studying on Laptop..lottie",
    "lottie/Academic Hut banner.lottie",
    "lottie/Online Learning Platform.lottie"
)

/** Low-opacity illustration, cycling with a smooth crossfade, sitting behind onboarding content on
 * every step - see [rememberThemedLottieComposition] for why each illustration is recolored to the
 * current theme's primary instead of using LottieMorphIcon's uniform-tint trick. */
@Composable
fun OnboardingAmbientBackground(modifier: Modifier = Modifier) {
    var index by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(6000)
            index = (index + 1) % ambientIllustrations.size
        }
    }

    Crossfade(targetState = index, animationSpec = tween(1200), modifier = modifier, label = "ambientIllustration") { i ->
        val accent = MaterialTheme.colorScheme.primary
        val composition by rememberThemedLottieComposition(ambientIllustrations[i], accent)
        val progress by animateLottieCompositionAsState(composition, iterations = LottieConstants.IterateForever)
        LottieAnimation(
            composition = composition,
            progress = { progress },
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(24.dp).alpha(0.10f)
        )
    }
}
