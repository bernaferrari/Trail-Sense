package com.kylecorry.andromeda.core.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Observes Android broadcasts until collection ends. Export only when broadcasts come from other apps. */
fun broadcasts(context: Context, filter: IntentFilter, exported: Boolean = false, includeSticky: Boolean = true): Flow<Intent> = callbackFlow {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null && (includeSticky || !isInitialStickyBroadcast)) trySend(intent)
        }
    }
    val flags = if (exported) ContextCompat.RECEIVER_EXPORTED else ContextCompat.RECEIVER_NOT_EXPORTED
    ContextCompat.registerReceiver(context.applicationContext, receiver, filter, flags)
    awaitClose { context.applicationContext.unregisterReceiver(receiver) }
}
