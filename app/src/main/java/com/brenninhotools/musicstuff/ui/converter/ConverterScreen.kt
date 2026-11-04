package com.brenninhotools.musicstuff.ui.converter

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.brenninhotools.musicstuff.R
import com.brenninhotools.musicstuff.convert.FlmLayout
import com.brenninhotools.musicstuff.flp.FlpArrangement
import com.brenninhotools.musicstuff.flp.FlpChannel
import com.brenninhotools.musicstuff.flp.FlpChannelKind
import java.util.Locale

private const val FLM_MIME_TYPE = "application/octet-stream"

@Composable
fun ConverterScreen(viewModel: ConverterViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::open)
    }
    val createFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(FLM_MIME_TYPE)) { uri ->
        uri?.let(viewModel::save)
    }
    val pickFile = { openFile.launch(arrayOf("*/*")) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { insets ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = insets.calculateTopPadding() + 16.dp,
                bottom = insets.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Header() }
            when (val current = state) {
                ConverterUiState.Idle -> item { PickFileCard(onPick = pickFile) }
                ConverterUiState.Working -> item { WorkingCard() }
                is ConverterUiState.Failed -> {
                    item { MessageCard("Couldn't open that file", current.message, MaterialTheme.colorScheme.error) }
                    item { PickFileCard(onPick = pickFile) }
                }
                is ConverterUiState.Ready -> readyContent(
                    state = current,
                    onPick = pickFile,
                    onSelectArrangement = viewModel::selectArrangement,
                    onConvert = { createFile.launch(flmFileName(current.fileName)) },
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.readyContent(
    state: ConverterUiState.Ready,
    onPick: () -> Unit,
    onSelectArrangement: (FlpArrangement) -> Unit,
    onConvert: () -> Unit,
) {
    val arrangements = state.project.arrangements.filter { it.patternClips.isNotEmpty() }
    val warnings = (state.project.warnings + state.conversion.warnings).distinct()
    val channels = state.project.channels.filter { it.kind != FlpChannelKind.AUTOMATION }

    item { ProjectCard(state) }
    if (arrangements.size > 1) {
        item { ArrangementCard(arrangements, state.arrangement, onSelectArrangement) }
    }
    item { ActionsCard(state, onPick = onPick, onConvert = onConvert) }
    if (warnings.isNotEmpty()) {
        item { WarningsCard(warnings) }
    }
    item {
        Text(
            text = "Channels (${channels.size})",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
    items(channels, key = { it.iid }) { channel ->
        ChannelRow(channel, state.project.noteCountFor(channel))
    }
}

@Composable
private fun Header() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp)),
        ) {
            Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.fillMaxSize())
            Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.fillMaxSize())
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = "Music Stuff",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "FLP to FLM converter",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun PickFileCard(onPick: () -> Unit) {
    SectionCard {
        Text("Open a project", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Pick an .flp from FL Studio Desktop. It's converted on this device and nothing is uploaded.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onPick, modifier = Modifier.fillMaxWidth()) {
            Text("Choose .flp file")
        }
    }
}

@Composable
private fun WorkingCard() {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
            Spacer(Modifier.width(12.dp))
            Text("Reading project…")
        }
    }
}

@Composable
private fun MessageCard(title: String, message: String, color: Color) {
    SectionCard {
        Text(title, style = MaterialTheme.typography.titleMedium, color = color)
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectCard(state: ConverterUiState.Ready) {
    val project = state.project
    SectionCard {
        Text(
            text = project.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = state.fileName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Stat("Tempo", String.format(Locale.US, "%.2f BPM", project.tempo))
            Stat("Time", "${project.timeSigNumerator}/${project.timeSigDenominator}")
            Stat("Notes", project.noteCount.toString())
            Stat("Patterns", project.patterns.count { it.notes.isNotEmpty() }.toString())
            if (project.version.isNotEmpty()) Stat("FL Studio", project.version)
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ArrangementCard(
    arrangements: List<FlpArrangement>,
    selected: FlpArrangement?,
    onSelect: (FlpArrangement) -> Unit,
) {
    SectionCard {
        Text("Arrangement", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            arrangements.forEach { arrangement ->
                FilterChip(
                    selected = arrangement.id == selected?.id,
                    onClick = { onSelect(arrangement) },
                    label = { Text(arrangement.name) },
                )
            }
        }
    }
}

@Composable
private fun ActionsCard(state: ConverterUiState.Ready, onPick: () -> Unit, onConvert: () -> Unit) {
    val conversion = state.conversion
    val layout = when (conversion.layout) {
        FlmLayout.ARRANGEMENT -> "placed from the playlist"
        FlmLayout.SEQUENTIAL -> "placed in pattern order"
    }
    SectionCard {
        Text(
            text = if (conversion.noteCount > 0) {
                "${conversion.noteCount} notes on ${conversion.trackCount} channels, $layout."
            } else {
                "This project has no notes to convert."
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            onClick = onConvert,
            enabled = conversion.noteCount > 0,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Convert to .flm")
        }
        OutlinedButton(onClick = onPick, modifier = Modifier.fillMaxWidth()) {
            Text("Choose another file")
        }
        if (state.saved) {
            Text(
                text = "Saved. Open it in FL Studio Mobile and pick instruments for each channel.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        state.saveError?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun WarningsCard(warnings: List<String>) {
    SectionCard {
        Text("Conversion notes", style = MaterialTheme.typography.titleMedium)
        warnings.forEach {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ChannelRow(channel: FlpChannel, noteCount: Int) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(channel.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = channel.kind.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = if (noteCount == 1) "1 note" else "$noteCount notes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

private fun flmFileName(fileName: String): String =
    fileName.substringBeforeLast('.', fileName).ifEmpty { "project" } + ".flm"
