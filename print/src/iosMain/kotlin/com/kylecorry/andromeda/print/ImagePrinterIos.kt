@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.kylecorry.andromeda.print

import com.kylecorry.andromeda.bitmaps.encodePng
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSThread
import platform.Foundation.create
import platform.UIKit.UIImage

fun imagePrinter(): ImagePrinter = ImagePrinter { image, jobName ->
    check(NSThread.isMainThread) { "The print dialog must be presented on the main thread" }
    val bytes = image.encodePng()
    val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
    presentPrintItem(checkNotNull(UIImage.imageWithData(data)) { "Unable to prepare image" }, jobName)
}
