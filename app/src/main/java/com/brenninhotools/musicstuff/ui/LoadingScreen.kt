package com.brenninhotools.musicstuff.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private const val LOOP_MILLIS = 1600
private const val BEAT_MILLIS = LOOP_MILLIS / 4

@Composable
fun LoadingScreen(state: ConverterState.Working, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "loading")
    val step by transition.animateFloat(
        initialValue = 0f,
        targetValue = ACTIVITY_SLICES.toFloat(),
        animationSpec = infiniteRepeatable(tween(LOOP_MILLIS, easing = LinearEasing), RepeatMode.Restart),
        label = "playhead",
    )
    val hop by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = BEAT_MILLIS
                0f at 0
                -14f at BEAT_MILLIS / 2
                0f at BEAT_MILLIS
            },
        ),
        label = "hop",
    )
    val playhead = step.toInt().coerceIn(0, ACTIVITY_SLICES - 1)
    val currentIndex = state.stages.indexOf(state.current)

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = LocalPlatform.current.mascot(),
                contentDescription = null,
                modifier = Modifier
                    .size(170.dp)
                    .offset(y = hop.dp),
            )
            Text(
                text = "${state.current.label}…",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            Text(
                text = state.fileName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(24.dp))
            PadRow(
                lit = List(ACTIVITY_SLICES) { it <= playhead },
                color = ChannelColors[currentIndex % ChannelColors.size],
                gap = 4.dp,
                corner = 4.dp,
            )
            Spacer(Modifier.height(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.stages.forEachIndexed { index, stage ->
                    StageRow(stage.label, done = index < currentIndex, active = index == currentIndex)
                }
            }
        }
    }
}

@Composable
private fun StageRow(label: String, done: Boolean, active: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(
                    when {
                        done -> Cyan
                        active -> Sun
                        else -> PadOff
                    },
                ),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (done || active) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
