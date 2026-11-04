package com.brenninhotools.musicstuff.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp

private const val SOURCE_URL = "https://github.com/BrenninhoTools/Music-Stuff"

private val Generator = listOf(
    "Makes drums, bass, chords and a melody in the style and key you pick",
    "Shuffle one part at a time until the song sounds right",
    "The preview uses simple sounds. In FL Studio Mobile, you pick the real instruments",
)

private val Converted = listOf(
    "Title, tempo and time signature",
    "Channels that have notes, with their names",
    "Pattern notes, placed where the playlist puts them",
)

private val NotConverted = listOf(
    "Plugins and samples, so you'll pick instruments again in FL Studio Mobile",
    "Audio clips and automation",
    "Mixer settings",
    "Slides, which become normal notes",
)

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val version = LocalPlatform.current.version
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxWidth(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("About", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
                    version?.let {
                        Text(
                            text = "Version $it",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item { ListPanel("Generator", Generator, Sun) }
            item { ListPanel("What converts", Converted, Cyan) }
            item { ListPanel("What doesn't convert", NotConverted, Pink) }
            item {
                Text(
                    text = "The .flm format isn't published, so the writer follows a real FL Studio Mobile project. " +
                        "Some projects may not open the way you expect.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Button(
                    onClick = { runCatching { uriHandler.openUri(SOURCE_URL) } },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("View source on GitHub", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun ListPanel(title: String, lines: List<String>, color: androidx.compose.ui.graphics.Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = color)
        lines.forEach {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
