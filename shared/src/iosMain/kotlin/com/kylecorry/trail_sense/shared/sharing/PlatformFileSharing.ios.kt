package com.kylecorry.trail_sense.shared.sharing

import com.kylecorry.andromeda.clipboard.clipboardWriter
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.dialogs.shareFile
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.write
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL
import platform.Foundation.NSCharacterSet
import platform.Foundation.NSString
import platform.Foundation.URLQueryAllowedCharacterSet
import platform.Foundation.stringByAddingPercentEncodingWithAllowedCharacters
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.popoverPresentationController

actual fun platformFileUri(file: PlatformFile): String? {
    return file.nsUrl.absoluteString ?: file.path
}

actual suspend fun saveExportFile(
    suggestedName: String,
    extension: String,
    bytes: ByteArray
): Boolean {
    val file = FileKit.openFileSaver(
        suggestedName = suggestedName,
        extension = extension
    ) ?: return false
    file.write(bytes)
    return true
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun shareText(title: String, text: String): Boolean {
    val root = topViewController() ?: return false
    val activityController = UIActivityViewController(
        activityItems = listOf(text),
        applicationActivities = null
    )
    activityController.popoverPresentationController?.let { popover ->
        popover.sourceView = root.view
    }
    root.presentViewController(activityController, animated = true, completion = null)
    return true
}

actual suspend fun shareFile(suggestedName: String, bytes: ByteArray): Boolean {
    val sanitizedName = suggestedName.substringAfterLast('/').ifBlank { "export.dat" }
    val tempFile = PlatformFile(FileKit.cacheDir, sanitizedName)
    tempFile.write(bytes)
    FileKit.shareFile(tempFile)
    return true
}

actual suspend fun openExternalUrl(url: String): Boolean {
    val nsUrl = NSURL.URLWithString(url) ?: return false
    if (!UIApplication.sharedApplication.canOpenURL(nsUrl)) return false
    UIApplication.sharedApplication.openURL(nsUrl, emptyMap<Any?, Any?>(), null)
    return true
}

actual suspend fun openEmail(
    recipient: String,
    subject: String,
    body: String
): Boolean {
    val encodedSubject = encodeQueryComponent(subject)
    val encodedBody = encodeQueryComponent(body)
    return openExternalUrl("mailto:$recipient?subject=$encodedSubject&body=$encodedBody")
}

actual fun copyToClipboard(text: String) {
    clipboardWriter().copy(text)
}

@Suppress("CAST_NEVER_SUCCEEDS")
private fun encodeQueryComponent(value: String): String {
    val nsString = value as NSString
    return nsString.stringByAddingPercentEncodingWithAllowedCharacters(
        NSCharacterSet.URLQueryAllowedCharacterSet
    ) ?: value
}

private fun topViewController(): UIViewController? {
    val application = UIApplication.sharedApplication
    val scenes = application.connectedScenes
    val window = scenes
        .filterIsInstance<UIWindowScene>()
        .flatMap { it.windows.filterIsInstance<UIWindow>() }
        .firstOrNull { it.isKeyWindow() }
        ?: application.keyWindow
    var controller = window?.rootViewController
    while (controller?.presentedViewController != null) {
        controller = controller.presentedViewController
    }
    return controller
}
