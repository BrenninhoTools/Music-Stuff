package com.brenninhotools.musicstuff.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val WIDE_LAYOUT: Dp = 720.dp

enum class AppScreen(val label: String) {
    CONVERTER("Converter"),
    GENERATOR("Generator"),
    ABOUT("About"),
}

@Composable
fun MusicStuffApp(converter: ConverterController, generator: GeneratorController) {
    val platform = LocalPlatform.current
    val converterState by converter.state.collectAsState()
    val direction by converter.direction.collectAsState()
    val generatorState by generator.state.collectAsState()
    var screen by rememberSaveable { mutableStateOf(AppScreen.CONVERTER) }
    platform.backHandler(screen != AppScreen.CONVERTER) { screen = AppScreen.CONVERTER }

    val generatorActions = remember(generator, platform) {
        GeneratorActions(
            onStyle = generator::setStyle,
            onRoot = generator::setRoot,
            onBpm = generator::setBpm,
            onBars = generator::setBars,
            onNewSong = generator::newSong,
            onShuffle = generator::shuffle,
            onTogglePlayback = generator::togglePlayback,
            onSave = {
                platform.pickSaveTarget(songFileName(generator.state.value.song.settings)) { target ->
                    generator.save(target)
                }
            },
        )
    }

    AppShell(
        screen = screen,
        onSelectScreen = {
            if (it != AppScreen.GENERATOR) generator.stop()
            screen = it
        },
    ) { modifier ->
        when (screen) {
            AppScreen.CONVERTER -> ConvertScreen(
                state = converterState,
                direction = direction,
                onDirection = converter::setDirection,
                onPick = { platform.pickProject(direction.source, converter::open) },
                onConvert = {
                    val current = converterState as? ConverterState.Ready
                    if (current != null) {
                        platform.pickSaveTarget(outputFileName(current.fileName, direction.target), converter::save)
                    }
                },
                onSelectArrangement = converter::selectArrangement,
                modifier = modifier,
            )
            AppScreen.GENERATOR -> GeneratorScreen(generatorState, generatorActions, modifier)
            AppScreen.ABOUT -> AboutScreen(modifier)
        }
    }
}

@Composable
fun AppShell(
    screen: AppScreen,
    onSelectScreen: (AppScreen) -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        if (maxWidth >= WIDE_LAYOUT) {
            Row(Modifier.fillMaxSize()) {
                Rail(screen, onSelectScreen)
                content(Modifier.weight(1f))
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                TopBar()
                content(Modifier.weight(1f))
                BottomNav(screen, onSelectScreen)
            }
        }
    }
}

@Composable
private fun Mascot(size: Dp) {
    Image(LocalPlatform.current.mascot(), contentDescription = null, modifier = Modifier.size(size))
}

@Composable
private fun TopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Mascot(44.dp)
        Spacer(Modifier.width(6.dp))
        Title()
    }
}

@Composable
private fun Title() {
    Text(
        text = "Music Stuff",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun Rail(screen: AppScreen, onSelect: (AppScreen) -> Unit) {
    Column(
        modifier = Modifier
            .width(190.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Mascot(130.dp)
        Title()
        Spacer(Modifier.height(22.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppScreen.entries.forEach { item ->
                NavItem(item, item == screen, Modifier.fillMaxWidth()) { onSelect(item) }
            }
        }
    }
}

@Composable
private fun BottomNav(screen: AppScreen, onSelect: (AppScreen) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppScreen.entries.forEach { item ->
            NavItem(item, item == screen, Modifier.weight(1f)) { onSelect(item) }
        }
    }
}

@Composable
private fun NavItem(item: AppScreen, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Sun else MaterialTheme.colorScheme.surface)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

fun outputFileName(fileName: String, extension: String): String =
    fileName.substringBeforeLast('.', fileName).ifEmpty { "project" } + extension
