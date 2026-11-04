package com.brenninhotools.musicstuff.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val BAR_LENGTH = 4

@Composable
fun PadRow(
    lit: List<Boolean>,
    color: Color,
    modifier: Modifier = Modifier,
    gap: Dp = 3.dp,
    corner: Dp = 3.dp,
    ratio: Float = 0.8f,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(gap),
    ) {
        lit.forEachIndexed { index, on ->
            val offColor = if (index / BAR_LENGTH % 2 == 0) PadOff else Panel.copy(alpha = 0.0f).compositeOver(PadOff, 0.72f)
            Box(
                Modifier
                    .weight(1f)
                    .aspectRatio(ratio)
                    .clip(RoundedCornerShape(corner))
                    .background(if (on) color else offColor),
            )
        }
    }
}

@Composable
fun ChannelPanel(
    title: String,
    lit: List<Boolean>,
    color: Color,
    trailing: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(10.dp))
            trailing()
        }
        PadRow(lit, color, ratio = 1.15f)
    }
}

private fun Color.compositeOver(base: Color, amount: Float): Color = Color(
    red = base.red * amount + Aubergine.red * (1 - amount),
    green = base.green * amount + Aubergine.green * (1 - amount),
    blue = base.blue * amount + Aubergine.blue * (1 - amount),
    alpha = 1f,
)
