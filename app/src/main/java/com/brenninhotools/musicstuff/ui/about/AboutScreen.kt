package com.brenninhotools.musicstuff.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.brenninhotools.musicstuff.ui.components.ScreenHeader
import com.brenninhotools.musicstuff.ui.components.StickerButton
import com.brenninhotools.musicstuff.ui.components.StickerCard

private const val SOURCE_URL = "https://github.com/BrenninhoTools/Music-Stuff"

private val Converted = listOf(
    "Title, tempo and time signature",
    "Instrument channels and their names",
    "Pattern notes, placed where the playlist puts them",
)

private val NotConverted = listOf(
    "Plugins and samples, so you'll pick instruments again in FL Studio Mobile",
    "Audio clips and automation",
    "Mixer settings",
    "Slides, which become normal notes",
)

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val direction = LocalLayoutDirection.current
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = insets.calculateStartPadding(direction) + 16.dp,
            end = insets.calculateEndPadding(direction) + 16.dp,
            top = insets.calculateTopPadding() + 12.dp,
            bottom = insets.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenHeader(title = "About", onBack = onBack) }
        item {
            StickerCard {
                Text("Music Stuff", style = MaterialTheme.typography.titleLarge)
                if (version != null) {
                    Text(
                        text = "Version $version",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "Projects are converted on this device. Nothing is uploaded.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        item { ListCard("What converts", Converted) }
        item { ListCard("What doesn't convert", NotConverted) }
        item {
            StickerCard {
                Text(
                    text = "The .flm format isn't published, so the writer is based on reverse-engineering. " +
                        "Some projects may not open the way you expect.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        item {
            StickerButton(
                text = "View source on GitHub",
                onClick = { runCatching { uriHandler.openUri(SOURCE_URL) } },
            )
        }
    }
}

@Composable
private fun ListCard(title: String, lines: List<String>) {
    StickerCard(spacing = 6.dp) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        lines.forEach { line ->
            Text(text = "• $line", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
