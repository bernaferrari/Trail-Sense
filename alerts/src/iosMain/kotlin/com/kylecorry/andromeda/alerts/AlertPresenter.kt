package com.kylecorry.andromeda.alerts

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.UIKit.*
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume

fun alertPresenter(viewController: UIViewController): AlertPresenter = object : AlertPresenter {
    override suspend fun confirm(title: String, message: String, confirm: String, cancel: String): Boolean =
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val alert = UIAlertController.alertControllerWithTitle(title, message, UIAlertControllerStyleAlert)
                alert.addAction(UIAlertAction.actionWithTitle(confirm, UIAlertActionStyleDefault) {
                    if (continuation.isActive) continuation.resume(true)
                })
                alert.addAction(UIAlertAction.actionWithTitle(cancel, UIAlertActionStyleCancel) {
                    if (continuation.isActive) continuation.resume(false)
                })
                continuation.invokeOnCancellation {
                    dispatch_async(dispatch_get_main_queue()) { alert.dismissViewControllerAnimated(true, null) }
                }
                viewController.presentViewController(alert, animated = true, completion = null)
            }
        }
}
