package com.kylecorry.andromeda.sms

import platform.Foundation.NSThread
import platform.MessageUI.MFMessageComposeViewController
import platform.MessageUI.MFMessageComposeViewControllerDelegateProtocol
import platform.MessageUI.MessageComposeResult
import platform.UIKit.UIApplication
import platform.darwin.NSObject

private val delegates = mutableMapOf<MFMessageComposeViewController, MessageDelegate>()

fun smsComposer(): SmsComposer = SmsComposer { recipient, message ->
    check(NSThread.isMainThread) { "The message composer must be presented on the main thread" }
    check(MFMessageComposeViewController.canSendText()) { "Messaging is not available on this device" }
    val controller = MFMessageComposeViewController()
    val delegate = MessageDelegate()
    controller.recipients = listOf(recipient)
    controller.body = message
    controller.messageComposeDelegate = delegate
    val root = UIApplication.sharedApplication.keyWindow?.rootViewController
        ?: error("No window is available to present the message composer")
    var presenter = root
    while (presenter.presentedViewController != null) presenter = presenter.presentedViewController!!
    delegates[controller] = delegate
    presenter.presentViewController(controller, animated = true, completion = null)
}

private class MessageDelegate : NSObject(), MFMessageComposeViewControllerDelegateProtocol {
    override fun messageComposeViewController(controller: MFMessageComposeViewController, didFinishWithResult: MessageComposeResult) {
        controller.dismissViewControllerAnimated(true, completion = null)
        delegates.remove(controller)
    }
}
