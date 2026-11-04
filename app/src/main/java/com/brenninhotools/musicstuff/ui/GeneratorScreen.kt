package com.brenninhotools.musicstuff.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.brenninhotools.musicstuff.generate.GeneratedTrack
import com.brenninhotools.musicstuff.generate.KEY_NAMES
import com.brenninhotools.musicstuff.generate.SONG_LENGTHS
import com.brenninhotools.musicstuff.generate.SongGenerator
import com.brenninhotools.musicstuff.generate.SongPart
import com.brenninhotools.musicstuff.generate.SongSettings
import com.brenninhotools.musicstuff.generate.SongStyle

private const val MIN_BPM = 60f
private const val MAX_BPM = 220f

class GeneratorActions(
    val onStyle: (SongStyle) -> Unit,
    val onRoot: (Int) -> Unit,
    val onBpm: (Int) -> Unit,
    val onBars: (Int) -> Unit,
    val onNewSong: () -> Unit,
    val onShuffle: (SongPart) -> Unit,
    val onTogglePlayback: () -> Unit,
    val onSave: () -> Unit,
)

@Composable
fun GeneratorScreen(state: GeneratorState, actions: GeneratorActions, modifier: Modifier = Modifier) {
    if (state.saving) {
        LoadingScreen(
            ConverterState.Working(
                fileName = songFileName(state.song.settings),
                stages = listOf(ConvertStage.WRITING),
                current = ConvertStage.WRITING,
            ),
            modifier,
        )
        return
    }
    BoxWithConstraints(modifier.fillMaxSize()) {
        if (maxWidth >= WIDE_LAYOUT) {
            Row(Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { SongHeader(state) }
                    tracks(state, actions)
                }
                Column(
                    modifier = Modifier
                        .width(310.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface)
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Settings(state.song.settings, actions)
                    SongActions(state, actions)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { SongHeader(state) }
                item { SongActions(state, actions) }
                tracks(state, actions)
                item { Settings(state.song.settings, actions) }
            }
        }
    }
}

@Composable
private fun SongHeader(state: GeneratorState) {
    val settings = state.song.settings
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = settings.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${settings.bpm} BPM, ${settings.bars} bars",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun LazyListScope.tracks(state: GeneratorState, actions: GeneratorActions) {
    val end = SongGenerator.endBeats(state.song.settings)
    itemsIndexed(state.song.tracks) { index, generated ->
        TrackRow(generated, end, index, actions)
    }
}

@Composable
private fun TrackRow(generated: GeneratedTrack, end: Double, index: Int, actions: GeneratorActions) {
    ChannelPanel(
        title = generated.part.label,
        lit = generated.track.activity(end),
        color = ChannelColors[index % ChannelColors.size],
    ) {
        TextButton(onClick = { actions.onShuffle(generated.part) }) {
            Text("Shuffle", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Settings(settings: SongSettings, actions: GeneratorActions) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Style", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SongStyle.entries.forEach { style ->
                FilterChip(
                    selected = style == settings.style,
                    onClick = { actions.onStyle(style) },
                    label = { Text(style.label) },
                )
            }
        }
        Text("Key", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            KEY_NAMES.forEachIndexed { root, name ->
                FilterChip(
                    selected = root == settings.root,
                    onClick = { actions.onRoot(root) },
                    label = { Text(name) },
                )
            }
        }
        Text("Tempo: ${settings.bpm} BPM", style = MaterialTheme.typography.titleMedium)
        Slider(
            value = settings.bpm.toFloat(),
            onValueChange = { actions.onBpm(it.toInt()) },
            valueRange = MIN_BPM..MAX_BPM,
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Length", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SONG_LENGTHS.forEach { bars ->
                FilterChip(
                    selected = bars == settings.bars,
                    onClick = { actions.onBars(bars) },
                    label = { Text("$bars bars") },
                )
            }
        }
    }
}

@Composable
private fun SongActions(state: GeneratorState, actions: GeneratorActions) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = actions.onNewSong,
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("New song", style = MaterialTheme.typography.labelLarge)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = actions.onTogglePlayback,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(if (state.playing) "Stop" else "Play", style = MaterialTheme.typography.labelLarge)
            }
            OutlinedButton(
                onClick = actions.onSave,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text("Save .flm", style = MaterialTheme.typography.labelLarge)
            }
        }
        if (state.saved) {
            Text(
                text = "Saved. Pick instruments for each channel in FL Studio Mobile.",
                style = MaterialTheme.typography.bodyMedium,
                color = Cyan,
            )
        }
        state.error?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
    }
}

fun songFileName(settings: SongSettings): String =
    settings.title.replace("'", "").replace('#', 's') + ".flm"
