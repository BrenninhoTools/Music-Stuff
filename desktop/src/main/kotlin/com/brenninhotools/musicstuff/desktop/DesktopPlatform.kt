package com.brenninhotools.musicstuff.desktop

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import com.brenninhotools.musicstuff.ui.AppPlatform
import com.brenninhotools.musicstuff.ui.ProjectFile
import com.brenninhotools.musicstuff.ui.SaveTarget
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

const val APP_VERSION = "0.0.2"
private const val PROJECT_EXTENSION = ".flp"
private const val OUTPUT_EXTENSION = ".flm"

private class DiskProjectFile(private val file: File) : ProjectFile {
    override val name: String = file.name

    override fun read(): ByteArray = file.readBytes()
}

private fun chooseFile(owner: Frame?, title: String, mode: Int, name: String?): File? {
    val dialog = FileDialog(owner, title, mode)
    if (name != null) dialog.file = name
    if (mode == FileDialog.LOAD) dialog.setFilenameFilter { _, file -> file.lowercase().endsWith(PROJECT_EXTENSION) }
    dialog.isVisible = true
    val selected = dialog.file ?: return null
    return File(dialog.directory, selected)
}

private fun withExtension(file: File): File =
    if (file.name.lowercase().endsWith(OUTPUT_EXTENSION)) file else File(file.parentFile, file.name + OUTPUT_EXTENSION)

fun desktopPlatform(owner: () -> Frame?): AppPlatform = AppPlatform(
    font = FontFamily(Font(resource = "lilita_one.ttf")),
    version = APP_VERSION,
    pickProject = { onPicked ->
        chooseFile(owner(), "Choose an FL Studio project", FileDialog.LOAD, null)?.let { onPicked(DiskProjectFile(it)) }
    },
    pickSaveTarget = { name, onPicked ->
        chooseFile(owner(), "Save the FL Studio Mobile project", FileDialog.SAVE, name)?.let { file ->
            onPicked(SaveTarget { bytes -> withExtension(file).writeBytes(bytes) })
        }
    },
    mascot = { painterResource("ic_launcher_foreground.xml") },
    backHandler = { _, _ -> },
)
