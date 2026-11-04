package com.brenninhotools.musicstuff.android

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.brenninhotools.musicstuff.R
import com.brenninhotools.musicstuff.ui.AppPlatform
import com.brenninhotools.musicstuff.ui.ProjectFile
import com.brenninhotools.musicstuff.ui.SaveTarget
import java.io.IOException

private const val FLM_MIME_TYPE = "application/octet-stream"

private class UriProjectFile(
    private val resolver: ContentResolver,
    private val uri: Uri,
) : ProjectFile {
    override val name: String = displayName()

    override fun read(): ByteArray =
        resolver.openInputStream(uri)?.use { it.readBytes() } ?: throw IOException("The file couldn't be opened.")

    private fun displayName(): String {
        val fallback = uri.lastPathSegment?.substringAfterLast('/') ?: "project.flp"
        return try {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: fallback
        } catch (error: SecurityException) {
            fallback
        }
    }
}

@Composable
fun rememberAndroidPlatform(): AppPlatform {
    val context = LocalContext.current
    val resolver = context.contentResolver
    val openCallback = remember { mutableStateOf<(ProjectFile) -> Unit>({}) }
    val saveCallback = remember { mutableStateOf<(SaveTarget) -> Unit>({}) }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) openCallback.value(UriProjectFile(resolver, uri))
    }
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(FLM_MIME_TYPE)) { uri ->
        if (uri != null) {
            saveCallback.value(
                SaveTarget { bytes ->
                    val stream = resolver.openOutputStream(uri, "wt") ?: throw IOException("The file couldn't be created.")
                    stream.use { it.write(bytes) }
                },
            )
        }
    }
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
    }
    return remember(open, create, version) {
        AppPlatform(
            font = FontFamily(Font(R.font.lilita_one)),
            version = version,
            audio = AndroidAudioPlayer(),
            pickProject = { onPicked ->
                openCallback.value = onPicked
                open.launch(arrayOf("*/*"))
            },
            pickSaveTarget = { name, onPicked ->
                saveCallback.value = onPicked
                create.launch(name)
            },
            mascot = { painterResource(R.drawable.ic_launcher_foreground) },
            backHandler = { enabled, onBack -> BackHandler(enabled, onBack) },
        )
    }
}
