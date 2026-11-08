package com.kylecorry.andromeda.alerts

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

fun alertPresenter(context: Context): AlertPresenter = object : AlertPresenter {
    override suspend fun confirm(title: String, message: String, confirm: String, cancel: String): Boolean =
        withContext(Dispatchers.Main.immediate) {
            suspendCancellableCoroutine { continuation ->
                val dialog = AlertDialog.Builder(context)
                    .setTitle(title).setMessage(message)
                    .setPositiveButton(confirm) { _, _ -> if (continuation.isActive) continuation.resume(true) }
                    .setNegativeButton(cancel) { _, _ -> if (continuation.isActive) continuation.resume(false) }
                    .setOnCancelListener { if (continuation.isActive) continuation.resume(false) }
                    .create()
                continuation.invokeOnCancellation { Handler(Looper.getMainLooper()).post { dialog.dismiss() } }
                dialog.show()
            }
        }
}
