package com.brenninhotools.musicstuff.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.brenninhotools.musicstuff.convert.FlmLayout
import com.brenninhotools.musicstuff.flm.FlmTrack
import com.brenninhotools.musicstuff.flp.FlpArrangement
import java.util.Locale

private val EmptyPads = List(ACTIVITY_SLICES) { false }

private val DemoPads = listOf(
    listOf(0, 4, 8, 12),
    listOf(2, 6, 7, 10, 14),
    listOf(0, 3, 6, 9, 12, 15),
).map { lit -> List(ACTIVITY_SLICES) { it in lit } }

@Composable
fun ConvertScreen(
    state: ConverterState,
    onPick: () -> Unit,
    onConvert: () -> Unit,
    onSelectArrangement: (FlpArrangement) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        ConverterState.Idle -> IdleScreen(onPick, null, modifier)
        is ConverterState.Failed -> IdleScreen(onPick, state.message, modifier)
        is ConverterState.Working -> LoadingScreen(state, modifier)
        is ConverterState.Ready -> ReadyScreen(state, onPick, onConvert, onSelectArrangement, modifier)
    }
}

@Composable
private fun IdleScreen(onPick: () -> Unit, error: String?, modifier: Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(role = Role.Button, onClickLabel = "Choose a project", onClick = onPick)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                DemoPads.forEachIndexed { index, lit ->
                    PadRow(lit, ChannelColors[index], ratio = 1f)
                }
            }
            Text(
                text = "Bring a desktop project to your phone",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Choose an .flp and Music Stuff writes an .flm for FL Studio Mobile. It all happens on this device.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            error?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                onClick = onPick,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Choose .flp file", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ReadyScreen(
    state: ConverterState.Ready,
    onPick: () -> Unit,
    onConvert: () -> Unit,
    onSelectArrangement: (FlpArrangement) -> Unit,
    modifier: Modifier,
) {
    val end = state.conversion.project.endBeats()
    val warnings = (state.project.warnings + state.conversion.warnings).distinct()
    BoxWithConstraints(modifier.fillMaxSize()) {
        if (maxWidth >= WIDE_LAYOUT) {
            Row(Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item { ProjectHeader(state) }
                    rack(state.conversion.project.tracks, end)
                }
                Column(
                    modifier = Modifier
                        .width(290.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Actions(state, onPick, onConvert, onSelectArrangement)
                    Notes(warnings)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { ProjectHeader(state) }
                item { Actions(state, onPick, onConvert, onSelectArrangement) }
                rack(state.conversion.project.tracks, end)
                if (warnings.isNotEmpty()) item { Notes(warnings) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectHeader(state: ConverterState.Ready) {
    val project = state.project
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = project.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(22.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Stat(String.format(Locale.US, "%.2f", project.tempo), "BPM")
            Stat("${project.timeSigNumerator}/${project.timeSigDenominator}", "Time")
            Stat(state.conversion.noteCount.toString(), "Notes")
            if (project.version.isNotEmpty()) Stat(project.version.split('.').take(3).joinToString("."), "FL Studio")
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleMedium, color = Sun)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun LazyListScope.rack(tracks: List<FlmTrack>, end: Double) {
    itemsIndexed(tracks) { index, track ->
        ChannelRow(track, end, ChannelColors[index % ChannelColors.size])
    }
}

@Composable
private fun ChannelRow(track: FlmTrack, end: Double, color: Color) {
    ChannelPanel(track.name, track.activity(end).ifEmpty { EmptyPads }, color) {
        Text(
            text = if (track.notes.size == 1) "1 note" else "${track.notes.size} notes",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Actions(
    state: ConverterState.Ready,
    onPick: () -> Unit,
    onConvert: () -> Unit,
    onSelectArrangement: (FlpArrangement) -> Unit,
) {
    val conversion = state.conversion
    val arrangements = state.project.arrangements.filter { it.patternClips.isNotEmpty() }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (arrangements.size > 1) {
            Text("Arrangement", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                arrangements.forEach { arrangement ->
                    FilterChip(
                        selected = arrangement.id == state.arrangement?.id,
                        onClick = { onSelectArrangement(arrangement) },
                        label = { Text(arrangement.name) },
                    )
                }
            }
        }
        Text(
            text = when {
                conversion.noteCount == 0 -> "This project has no notes to convert."
                conversion.layout == FlmLayout.ARRANGEMENT -> "Notes are placed where the playlist puts them."
                else -> "Patterns are placed one after another."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onConvert,
            enabled = conversion.noteCount > 0,
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Convert to .flm", style = MaterialTheme.typography.labelLarge)
        }
        OutlinedButton(
            onClick = onPick,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Choose another file", style = MaterialTheme.typography.labelLarge)
        }
        if (state.saved) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Cyan),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Saved. Pick instruments for each channel in FL Studio Mobile.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
        state.saveError?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun Notes(warnings: List<String>) {
    if (warnings.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Spacer(Modifier.height(2.dp))
        Text("Conversion notes", style = MaterialTheme.typography.titleMedium)
        warnings.forEach {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
