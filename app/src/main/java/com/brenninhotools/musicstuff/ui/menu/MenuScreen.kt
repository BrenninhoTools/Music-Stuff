package com.brenninhotools.musicstuff.ui.menu

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.brenninhotools.musicstuff.R
import com.brenninhotools.musicstuff.ui.components.StickerCard
import com.brenninhotools.musicstuff.ui.components.sunburst
import com.brenninhotools.musicstuff.ui.theme.Ink
import com.brenninhotools.musicstuff.ui.theme.Muted
import com.brenninhotools.musicstuff.ui.theme.Paper
import com.brenninhotools.musicstuff.ui.theme.Sun

private const val TITLE_TILT = -2.5f

@Composable
fun MenuScreen(onOpenConverter: () -> Unit, onOpenAbout: () -> Unit) {
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .sunburst()
            .verticalScroll(rememberScrollState())
            .padding(insets)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Mascot()
        Title()
        Spacer(Modifier.height(28.dp))
        Column(
            modifier = Modifier.widthIn(max = 480.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MenuItem(
                icon = R.drawable.ic_menu_convert,
                title = "FLP to FLM",
                description = "Turn a desktop project into one for FL Studio Mobile",
                color = Sun,
                onClick = onOpenConverter,
            )
            MenuItem(
                icon = R.drawable.ic_menu_about,
                title = "About",
                description = "See what converts, what doesn't, and the source code",
                color = Paper,
                onClick = onOpenAbout,
            )
        }
    }
}

@Composable
private fun Mascot() {
    val bounce = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) {
        bounce.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        )
    }
    Image(
        painter = painterResource(R.drawable.ic_launcher_foreground),
        contentDescription = null,
        modifier = Modifier
            .size(250.dp)
            .scale(bounce.value),
    )
}

@Composable
private fun Title() {
    Text(
        text = "Music Stuff",
        style = MaterialTheme.typography.displayMedium.copy(
            shadow = Shadow(color = Ink, offset = Offset(0f, 10f), blurRadius = 0f),
        ),
        color = Color.White,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .rotate(TITLE_TILT)
            .semantics { heading() },
    )
}

@Composable
private fun MenuItem(
    icon: Int,
    title: String,
    description: String,
    color: Color,
    onClick: () -> Unit,
) {
    StickerCard(color = color, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(52.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (color == Sun) Ink else Muted,
                )
            }
        }
    }
}
