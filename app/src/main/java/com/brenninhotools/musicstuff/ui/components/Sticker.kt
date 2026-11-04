package com.brenninhotools.musicstuff.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.brenninhotools.musicstuff.R
import com.brenninhotools.musicstuff.ui.theme.Ink
import com.brenninhotools.musicstuff.ui.theme.Paper

private val OutlineWidth = 3.dp
private val Depth = 5.dp

@Composable
private fun Sticker(
    color: Color,
    shape: Shape,
    onClick: (() -> Unit)?,
    enabled: Boolean,
    role: Role,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val sink by animateDpAsState(
        targetValue = if (pressed) Depth else 0.dp,
        animationSpec = tween(durationMillis = 70),
        label = "sticker press",
    )
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = source,
            indication = ripple(),
            enabled = enabled,
            role = role,
            onClick = onClick,
        )
    } else {
        Modifier
    }
    Box(modifier.padding(bottom = Depth)) {
        Box(
            Modifier
                .matchParentSize()
                .offset(y = Depth)
                .background(Ink, shape),
        )
        Box(
            Modifier
                .offset(y = sink)
                .clip(shape)
                .background(color)
                .border(OutlineWidth, Ink, shape)
                .then(clickModifier),
        ) {
            CompositionLocalProvider(LocalContentColor provides Ink) {
                content()
            }
        }
    }
}

@Composable
fun StickerCard(
    modifier: Modifier = Modifier,
    color: Color = Paper,
    onClick: (() -> Unit)? = null,
    spacing: Dp = 10.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Sticker(
        color = color,
        shape = RoundedCornerShape(22.dp),
        onClick = onClick,
        enabled = true,
        role = Role.Button,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(spacing),
            content = content,
        )
    }
}

@Composable
fun StickerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
) {
    Sticker(
        color = if (enabled) color else MaterialTheme.colorScheme.outlineVariant,
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
        enabled = enabled,
        role = Role.Button,
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun StickerBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Sticker(
        color = MaterialTheme.colorScheme.primary,
        shape = CircleShape,
        onClick = onClick,
        enabled = true,
        role = Role.Button,
        modifier = modifier,
    ) {
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_back),
                contentDescription = "Back",
                tint = Color.Unspecified,
            )
        }
    }
}
