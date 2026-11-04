package com.brenninhotools.musicstuff.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import com.brenninhotools.musicstuff.convert.FlpToFlmConverter
import com.brenninhotools.musicstuff.flp.FlpArrangement
import com.brenninhotools.musicstuff.flp.FlpChannel
import com.brenninhotools.musicstuff.flp.FlpChannelKind
import com.brenninhotools.musicstuff.flp.FlpClip
import com.brenninhotools.musicstuff.flp.FlpNote
import com.brenninhotools.musicstuff.flp.FlpPattern
import com.brenninhotools.musicstuff.flp.FlpProject
import com.brenninhotools.musicstuff.generate.SongGenerator
import com.brenninhotools.musicstuff.generate.SongSettings
import com.brenninhotools.musicstuff.ui.AboutScreen
import com.brenninhotools.musicstuff.ui.AppScreen
import com.brenninhotools.musicstuff.ui.AppShell
import com.brenninhotools.musicstuff.ui.ConvertScreen
import com.brenninhotools.musicstuff.ui.ConvertStage
import com.brenninhotools.musicstuff.ui.ConverterState
import com.brenninhotools.musicstuff.ui.GeneratorActions
import com.brenninhotools.musicstuff.ui.GeneratorScreen
import com.brenninhotools.musicstuff.ui.GeneratorState
import com.brenninhotools.musicstuff.ui.LocalPlatform
import com.brenninhotools.musicstuff.ui.MusicStuffTheme
import org.jetbrains.skia.EncodedImageFormat
import org.junit.Test
import java.io.File

class ScreensTest {
    private val platform = desktopPlatform { null }

    private val notes = listOf(0, 1, 2).flatMap { channel ->
        (0 until 90).map { step ->
            FlpNote(
                position = step * 96L * (channel + 1) + channel * 48L,
                length = 48,
                key = 60 + step % 12,
                velocity = 100,
                release = 64,
                channelIid = channel,
                slide = false,
            )
        }
    }

    private val project = FlpProject(
        title = "Who FLP Recreation",
        version = "26.1.6.5639",
        ppq = 96,
        tempo = 162.0,
        timeSigNumerator = 4,
        timeSigDenominator = 4,
        channels = listOf("BobTTS Chromatic Final", "ley chrom 3", "nouny chrom").mapIndexed { index, name ->
            FlpChannel(index, name, FlpChannelKind.NATIVE)
        },
        patterns = listOf(FlpPattern(1, "Pattern 1", notes)),
        arrangements = listOf(FlpArrangement(0, "Arrangement", listOf(FlpClip(0, 72000, 1, 0f)))),
        warnings = listOf("3 audio or automation clip(s) were skipped. Only pattern notes are converted."),
    )

    private val ready = ConverterState.Ready(
        fileName = "who-flp.flp",
        project = project,
        arrangement = project.arrangements.first(),
        conversion = FlpToFlmConverter.convert(project),
    )

    private val working = ConverterState.Working(
        fileName = "who-flp.flp",
        stages = listOf(ConvertStage.PLACING, ConvertStage.WRITING),
        current = ConvertStage.WRITING,
    )

    private val generated = GeneratorState(SongGenerator.generate(SongSettings(seed = 2026)))

    private val actions = GeneratorActions({}, {}, {}, {}, {}, {}, {}, {})

    private fun render(name: String, width: Int, height: Int, content: @Composable () -> Unit) {
        val output = File("build/screens/$name.png")
        output.parentFile.mkdirs()
        ImageComposeScene(width, height, Density(2f)) {
            CompositionLocalProvider(LocalPlatform provides platform) {
                MusicStuffTheme(platform.font, content)
            }
        }.use { scene ->
            scene.render(0L)
            val image = scene.render(1_200_000_000L)
            output.writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
    }

    private fun screens(prefix: String, width: Int, height: Int) {
        val convert = @Composable { state: ConverterState ->
            AppShell(AppScreen.CONVERTER, {}) { modifier -> ConvertScreen(state, {}, {}, {}, modifier) }
        }
        render("$prefix-idle", width, height) { convert(ConverterState.Idle) }
        render("$prefix-failed", width, height) { convert(ConverterState.Failed("This is a .zip archive. Extract the .flp from it first.")) }
        render("$prefix-loading", width, height) { convert(working) }
        render("$prefix-ready", width, height) { convert(ready) }
        render("$prefix-saved", width, height) { convert(ready.copy(saved = true)) }
        render("$prefix-generator", width, height) {
            AppShell(AppScreen.GENERATOR, {}) { modifier -> GeneratorScreen(generated, actions, modifier) }
        }
        render("$prefix-generator-saving", width, height) {
            AppShell(AppScreen.GENERATOR, {}) { modifier -> GeneratorScreen(generated.copy(saving = true), actions, modifier) }
        }
        render("$prefix-about", width, height) {
            AppShell(AppScreen.ABOUT, {}) { modifier -> AboutScreen(modifier) }
        }
    }

    @Test
    fun rendersPhoneScreens() = screens("phone", 720, 1560)

    @Test
    fun rendersDesktopScreens() = screens("desktop", 2160, 1440)
}
