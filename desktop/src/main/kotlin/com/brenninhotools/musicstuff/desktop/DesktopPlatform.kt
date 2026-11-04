package com.brenninhotools.musicstuff.desktop

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import com.brenninhotools.musicstuff.ui.AppPlatform
import com.brenninhotools.musicstuff.ui.ProjectFile
import com.brenninhotools.musicstuff.ui.SaveTarget
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

const val APP_VERSION = "0.0.3"

private class DiskProjectFile(private val file: File) : ProjectFile {
    override val name: String = file.name

    override fun read(): ByteArray = file.readBytes()
}

private fun chooseFile(owner: Frame?, title: String, mode: Int, name: String, extension: String): File? {
    val dialog = FileDialog(owner, title, mode)
    dialog.file = name
    dialog.setFilenameFilter { _, file -> file.lowercase().endsWith(extension) }
    dialog.isVisible = true
    val selected = dialog.file ?: return null
    return File(dialog.directory, selected)
}

private fun withExtension(file: File, extension: String): File =
    if (file.name.lowercase().endsWith(extension)) file else File(file.parentFile, file.name + extension)

fun desktopPlatform(owner: () -> Frame?): AppPlatform = AppPlatform(
    font = FontFamily(Font(resource = "lilita_one.ttf")),
    version = APP_VERSION,
    pickProject = { extension, onPicked ->
        chooseFile(owner(), "Choose a project", FileDialog.LOAD, "*$extension", extension)?.let {
            onPicked(DiskProjectFile(it))
        }
    },
    pickSaveTarget = { name, onPicked ->
        val extension = "." + name.substringAfterLast('.')
        chooseFile(owner(), "Save the project", FileDialog.SAVE, name, extension)?.let { file ->
            onPicked(SaveTarget { bytes -> withExtension(file, extension).writeBytes(bytes) })
        }
    },
    backHandler = { _, _ -> },
)
