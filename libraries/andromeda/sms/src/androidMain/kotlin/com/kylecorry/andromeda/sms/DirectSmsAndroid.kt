package com.kylecorry.andromeda.sms

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.ksensor.core.context.KSensorContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

public actual val directSmsSupported: Boolean
    get() {
        val features = KSensorContext.get().packageManager
        return if (Build.VERSION.SDK_INT >= 33) features.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_MESSAGING)
        else features.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
    }

@SuppressLint("MissingPermission")
@Suppress("DEPRECATION")
public actual fun sendSms(recipient: String, message: String) {
    require(recipient.isNotBlank() && message.isNotEmpty())
    val context = KSensorContext.get()
    val manager = checkNotNull(if (Build.VERSION.SDK_INT >= 31) context.getSystemService(SmsManager::class.java) else SmsManager.getDefault()) {
        "SMS is unavailable"
    }
    val parts = manager.divideMessage(message)
    if (parts.size == 1) manager.sendTextMessage(recipient, null, message, null, null)
    else manager.sendMultipartTextMessage(recipient, null, parts, null, null)
}

/** Decodes an SMS broadcast; usable by a host's manifest receiver when the app is not running. */
public fun decodeSmsBroadcast(intent: Intent): List<IncomingSmsMessage> {
    if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return emptyList()
    return Telephony.Sms.Intents.getMessagesFromIntent(intent).map { message ->
        IncomingSmsMessage(message.originatingAddress.orEmpty(), message.messageBody.orEmpty())
    }
}

public actual fun incomingSms(): Flow<IncomingSmsMessage> = callbackFlow {
    val context = KSensorContext.get()
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
            try {
                decodeSmsBroadcast(intent).forEach { trySend(it) }
            } catch (error: SecurityException) {
                close(error)
            }
        }
    }
    ContextCompat.registerReceiver(
        context, receiver, IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION),
        Manifest.permission.BROADCAST_SMS, null, ContextCompat.RECEIVER_EXPORTED,
    )
    awaitClose { context.unregisterReceiver(receiver) }
}
