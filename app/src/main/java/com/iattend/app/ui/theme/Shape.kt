package com.iattend.app.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Shapes
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.toPath

/** Higher = flatter, more superellipse-like corners (design.md's "smoother than a circular arc" squircle). */
private const val SQUIRCLE_SMOOTHING = 0.6f

/**
 * Squircle (superellipse) corners per design.md, built on Google's own smoothed-corner
 * implementation (androidx.graphics.shapes) instead of hand-rolled Bezier math - that's
 * exactly the primitive Material3 Expressive itself uses for this same "smoother than a
 * circular arc" look, so it's already correct and well-tested rather than approximated.
 */
class SquircleShape(
    topStart: CornerSize,
    topEnd: CornerSize,
    bottomEnd: CornerSize,
    bottomStart: CornerSize
) : CornerBasedShape(topStart, topEnd, bottomEnd, bottomStart) {

    constructor(radius: Dp) : this(
        topStart = CornerSize(radius),
        topEnd = CornerSize(radius),
        bottomEnd = CornerSize(radius),
        bottomStart = CornerSize(radius)
    )

    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection
    ): Outline {
        val maxRadius = minOf(size.width, size.height) / 2f
        fun rounding(radius: Float) = CornerRounding(radius.coerceAtMost(maxRadius), SQUIRCLE_SMOOTHING)

        val polygon = RoundedPolygon.rectangle(
            width = size.width,
            height = size.height,
            perVertexRounding = listOf(
                rounding(topEnd),
                rounding(bottomEnd),
                rounding(bottomStart),
                rounding(topStart)
            ),
            centerX = size.width / 2f,
            centerY = size.height / 2f
        )
        return Outline.Generic(polygon.toPath().asComposePath())
    }

    override fun copy(
        topStart: CornerSize,
        topEnd: CornerSize,
        bottomEnd: CornerSize,
        bottomStart: CornerSize
    ): CornerBasedShape = SquircleShape(topStart, topEnd, bottomEnd, bottomStart)
}

val Shapes = Shapes(
    extraSmall = SquircleShape(8.dp),
    small = SquircleShape(12.dp),
    medium = SquircleShape(16.dp),
    large = SquircleShape(24.dp),
    extraLarge = SquircleShape(32.dp)
)
