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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.brenninhotools.musicstuff.flm.FlmTrack
import com.brenninhotools.musicstuff.flp.FlpArrangement
import kotlin.math.roundToLong

private val EmptyPads = List(ACTIVITY_SLICES) { false }

private val DemoPads = listOf(
    listOf(0, 4, 8, 12),
    listOf(2, 6, 7, 10, 14),
    listOf(0, 3, 6, 9, 12, 15),
).map { lit -> List(ACTIVITY_SLICES) { it in lit } }

@Composable
fun ConvertScreen(
    state: ConverterState,
    direction: ConvertDirection,
    onDirection: (ConvertDirection) -> Unit,
    onPick: () -> Unit,
    onConvert: () -> Unit,
    onSelectArrangement: (FlpArrangement) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        ConverterState.Idle -> IdleScreen(direction, onDirection, onPick, null, modifier)
        is ConverterState.Failed -> IdleScreen(direction, onDirection, onPick, state.message, modifier)
        is ConverterState.Working -> LoadingScreen(state, modifier)
        is ConverterState.Ready -> ReadyScreen(state, direction, onDirection, onPick, onConvert, onSelectArrangement, modifier)
    }
}

@Composable
private fun DirectionPicker(selected: ConvertDirection, onSelect: (ConvertDirection) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ConvertDirection.entries.forEach { direction ->
            val active = direction == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (active) Sun else MaterialTheme.colorScheme.surface)
                    .selectable(selected = active, role = Role.RadioButton, onClick = { onSelect(direction) })
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = direction.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun IdleScreen(
    direction: ConvertDirection,
    onDirection: (ConvertDirection) -> Unit,
    onPick: () -> Unit,
    error: String?,
    modifier: Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            DirectionPicker(direction, onDirection)
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
                text = when (direction) {
                    ConvertDirection.FLP_TO_FLM -> "Bring a desktop project to your phone"
                    ConvertDirection.FLM_TO_FLP -> "Bring a phone project to your desktop"
                },
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Choose an ${direction.source} and Music Stuff writes an ${direction.target} " +
                    "for ${direction.targetApp}. " +
                    "It all happens on this device.",
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
                Text("Choose ${direction.source} file", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ReadyScreen(
    state: ConverterState.Ready,
    direction: ConvertDirection,
    onDirection: (ConvertDirection) -> Unit,
    onPick: () -> Unit,
    onConvert: () -> Unit,
    onSelectArrangement: (FlpArrangement) -> Unit,
    modifier: Modifier,
) {
    val summary = state.summary
    val end = summary.preview.endBeats()
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
                    item { ProjectHeader(summary, direction) }
                    rack(summary.preview.tracks, end)
                }
                Column(
                    modifier = Modifier
                        .width(290.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface)
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    DirectionPicker(direction, onDirection)
                    Actions(state, direction, onPick, onConvert, onSelectArrangement)
                    Notes(summary.warnings)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { DirectionPicker(direction, onDirection) }
                item { ProjectHeader(summary, direction) }
                item { Actions(state, direction, onPick, onConvert, onSelectArrangement) }
                rack(summary.preview.tracks, end)
                if (summary.warnings.isNotEmpty()) item { Notes(summary.warnings) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectHeader(summary: ProjectSummary, direction: ConvertDirection) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = summary.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(22.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Stat(tempoLabel(summary.tempo), "BPM")
            Stat("${summary.timeSigNumerator}/${summary.timeSigDenominator}", "Time")
            Stat(summary.noteCount.toString(), "Notes")
            if (summary.sourceVersion.isNotEmpty()) Stat(summary.sourceVersion, direction.sourceApp)
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
    direction: ConvertDirection,
    onPick: () -> Unit,
    onConvert: () -> Unit,
    onSelectArrangement: (FlpArrangement) -> Unit,
) {
    val summary = state.summary
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (summary.arrangements.size > 1) {
            Text("Arrangement", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                summary.arrangements.forEach { arrangement ->
                    FilterChip(
                        selected = arrangement.id == state.arrangement?.id,
                        onClick = { onSelectArrangement(arrangement) },
                        label = { Text(arrangement.name) },
                    )
                }
            }
        }
        Text(
            text = summary.placement,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onConvert,
            enabled = summary.noteCount > 0,
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Convert to ${direction.target}", style = MaterialTheme.typography.labelLarge)
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
                    text = "Saved. Pick instruments for each channel in ${direction.targetApp}.",
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

private fun tempoLabel(tempo: Double): String {
    val hundredths = (tempo * 100).roundToLong()
    return "${hundredths / 100}.${(hundredths % 100).toString().padStart(2, '0')}"
}
