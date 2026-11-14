package com.kylecorry.andromeda.print

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSThread
import platform.Foundation.create
import platform.UIKit.UIPrintInfo
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice
import platform.UIKit.UIUserInterfaceIdiomPad
import platform.UIKit.UIPrintInteractionController

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
public fun pdfPrinter(): PdfPrinter = PdfPrinter { pdf, jobName ->
    check(NSThread.isMainThread) { "The print dialog must be presented on the main thread" }
    require(pdf.isNotEmpty())
    val data = pdf.usePinned { NSData.create(bytes = it.addressOf(0), length = pdf.size.toULong()) }
    presentPrintItem(data, jobName)
}

@OptIn(ExperimentalForeignApi::class)
internal fun presentPrintItem(item: Any, jobName: String) {
    val controller = UIPrintInteractionController.sharedPrintController
    val info = UIPrintInfo.printInfo().apply { this.jobName = jobName }
    controller.printInfo = info
    controller.printingItem = item
    presentPrintController(controller)
}

@OptIn(ExperimentalForeignApi::class)
internal fun presentPrintController(controller: UIPrintInteractionController) {
    if (UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad) {
        val view = UIApplication.sharedApplication.keyWindow?.rootViewController?.view
            ?: error("No window is available to present the print dialog")
        controller.presentFromRect(view.bounds, view, true, completionHandler = null)
    } else {
        controller.presentAnimated(true, completionHandler = null)
    }
}
