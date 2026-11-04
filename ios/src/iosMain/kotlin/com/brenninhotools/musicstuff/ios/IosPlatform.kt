package com.brenninhotools.musicstuff.ios

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import com.brenninhotools.musicstuff.platform.IoFailure
import com.brenninhotools.musicstuff.ui.AppPlatform
import com.brenninhotools.musicstuff.ui.ProjectFile
import com.brenninhotools.musicstuff.ui.SaveTarget
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.dataWithBytes
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.writeToFile
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTTypeItem
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

const val APP_VERSION = "0.0.3"

fun ByteArray.toNSData(): NSData {
    if (isEmpty()) return NSData()
    return usePinned { pinned -> NSData.dataWithBytes(pinned.addressOf(0), size.toULong()) }
}

private fun NSData.toByteArray(): ByteArray {
    val count = length.toInt()
    if (count == 0) return ByteArray(0)
    val output = ByteArray(count)
    output.usePinned { pinned -> memcpy(pinned.addressOf(0), bytes, length) }
    return output
}

private class PickerDelegate(private val done: (List<NSURL>) -> Unit) : NSObject(), UIDocumentPickerDelegateProtocol {
    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        Pickers.release(this)
        done(didPickDocumentsAtURLs.filterIsInstance<NSURL>())
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        Pickers.release(this)
    }
}

private object Pickers {
    private val active = mutableListOf<NSObject>()

    fun release(delegate: NSObject) {
        active.remove(delegate)
    }

    private fun top(): UIViewController? {
        var controller = UIApplication.sharedApplication.keyWindow?.rootViewController
        while (controller?.presentedViewController != null) controller = controller.presentedViewController
        return controller
    }

    fun present(picker: UIDocumentPickerViewController, done: (List<NSURL>) -> Unit) {
        val delegate = PickerDelegate(done)
        active += delegate
        picker.delegate = delegate
        top()?.presentViewController(picker, animated = true, completion = null)
    }
}

private class UrlProjectFile(private val url: NSURL) : ProjectFile {
    override val name: String = url.lastPathComponent ?: "project"

    override fun read(): ByteArray {
        val data = NSData.dataWithContentsOfURL(url) ?: throw IoFailure("The file couldn't be opened.")
        return data.toByteArray()
    }
}

private fun exportFile(name: String, bytes: ByteArray) {
    val path = NSTemporaryDirectory() + name.replace('/', '_')
    if (!bytes.toNSData().writeToFile(path, atomically = true)) throw IoFailure("The file couldn't be created.")
    dispatch_async(dispatch_get_main_queue()) {
        val url = NSURL.fileURLWithPath(path)
        Pickers.present(UIDocumentPickerViewController(forExportingURLs = listOf(url), asCopy = true)) { }
    }
}

private fun loadFont(): FontFamily {
    val path = NSBundle.mainBundle.pathForResource("lilita_one", ofType = "ttf", inDirectory = "font")
    val data = path?.let { NSData.create(contentsOfFile = it) } ?: return FontFamily.Default
    return FontFamily(Font("lilita_one", data.toByteArray()))
}

fun iosPlatform(): AppPlatform = AppPlatform(
    font = loadFont(),
    version = APP_VERSION,
    pickProject = { _, onPicked ->
        val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeItem), asCopy = true)
        Pickers.present(picker) { urls -> urls.firstOrNull()?.let { onPicked(UrlProjectFile(it)) } }
    },
    pickSaveTarget = { name, onPicked ->
        onPicked(SaveTarget { bytes -> exportFile(name, bytes) })
    },
    backHandler = { _, _ -> },
)
