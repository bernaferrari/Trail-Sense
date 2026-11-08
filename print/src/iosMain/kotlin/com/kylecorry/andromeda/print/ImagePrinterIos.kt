@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.kylecorry.andromeda.print

import com.kylecorry.andromeda.bitmaps.encodePng
import kotlinx.cinterop.CValue
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGContextClipToRect
import platform.CoreGraphics.CGContextRestoreGState
import platform.CoreGraphics.CGContextSaveGState
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.NSThread
import platform.Foundation.create
import platform.UIKit.*

fun imagePrinter(): ImagePrinter = ImagePrinter { image, jobName, options ->
    check(NSThread.isMainThread) { "The print dialog must be presented on the main thread" }
    val bytes = image.encodePng()
    val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
    val bitmap = checkNotNull(UIImage.imageWithData(data)) { "Unable to prepare image" }
    val controller = UIPrintInteractionController.sharedPrintController
    controller.printInfo = UIPrintInfo.printInfo().apply {
        this.jobName = jobName
        outputType = if (options.color == PrintColorMode.Monochrome) UIPrintInfoOutputType.UIPrintInfoOutputGrayscale else UIPrintInfoOutputType.UIPrintInfoOutputGeneral
        orientation = when (options.orientation) {
            PrintOrientation.Auto -> if (image.width > image.height) UIPrintInfoOrientation.UIPrintInfoOrientationLandscape else UIPrintInfoOrientation.UIPrintInfoOrientationPortrait
            PrintOrientation.Portrait -> UIPrintInfoOrientation.UIPrintInfoOrientationPortrait
            PrintOrientation.Landscape -> UIPrintInfoOrientation.UIPrintInfoOrientationLandscape
        }
    }
    controller.printPageRenderer = ImagePageRenderer(bitmap, image.width, image.height, options.scale)
    presentPrintController(controller)
}

private class ImagePageRenderer(
    private val image: UIImage,
    private val width: Int,
    private val height: Int,
    private val scale: PrintScaleMode,
) : UIPrintPageRenderer() {
    override fun numberOfPages(): Long = 1

    override fun drawContentForPageAtIndex(pageIndex: Long, inRect: CValue<CGRect>) {
        val context = UIGraphicsGetCurrentContext() ?: return
        inRect.useContents {
            val layout = imagePrintLayout(width, height, size.width, size.height, scale)
            CGContextSaveGState(context)
            try {
                CGContextClipToRect(context, inRect)
                image.drawInRect(CGRectMake(origin.x + layout.x, origin.y + layout.y, layout.width, layout.height))
            } finally { CGContextRestoreGState(context) }
        }
    }
}
