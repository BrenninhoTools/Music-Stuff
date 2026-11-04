package com.brenninhotools.musicstuff.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.brenninhotools.musicstuff.ui.theme.Grape
import com.brenninhotools.musicstuff.ui.theme.GrapeLight
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val RAY_COUNT = 14
private const val RAY_START = -0.13

fun Modifier.sunburst(
    centerFraction: Float = 0.24f,
    base: Color = Grape,
    ray: Color = GrapeLight,
): Modifier = drawWithCache {
    val center = Offset(size.width / 2f, size.height * centerFraction)
    val reach = size.maxDimension * 1.5f
    val rays = Path()
    repeat(RAY_COUNT) { index ->
        val start = 2 * PI * index / RAY_COUNT + RAY_START
        val end = start + PI / RAY_COUNT
        rays.moveTo(center.x, center.y)
        rays.lineTo(center.x + reach * cos(start).toFloat(), center.y + reach * sin(start).toFloat())
        rays.lineTo(center.x + reach * cos(end).toFloat(), center.y + reach * sin(end).toFloat())
        rays.close()
    }
    onDrawBehind {
        drawRect(base)
        drawPath(rays, ray)
    }
}
