package com.gabriion.betterme.ui.gym.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition

/**
 * Renders a Lottie animation for an exercise.
 *
 * V1 ships one generic looping animation per muscle group in
 * `assets/ex/<group>.json` rather than 80 hand-authored per-exercise files.
 * When [muscleGroup] is provided we load that bundled animation directly,
 * ignoring [assetPath] (per-exercise files don't exist yet). If loading fails
 * or no muscle group is supplied we fall back to a FitnessCenter icon inside
 * a tinted circle.
 */
@Composable
fun LottiePreview(
    assetPath: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    muscleGroup: String? = null,
) {
    val effectivePath = muscleGroup?.let { "ex/${it.lowercase()}.json" } ?: assetPath
    val compositionResult = rememberLottieComposition(LottieCompositionSpec.Asset(effectivePath))
    val composition = compositionResult.value
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (composition != null) {
                LottieAnimation(
                    composition = composition,
                    iterations = LottieConstants.IterateForever,
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
