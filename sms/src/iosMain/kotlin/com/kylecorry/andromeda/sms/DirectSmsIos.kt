package com.kylecorry.andromeda.sms

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

actual val directSmsSupported: Boolean = false
actual fun sendSms(recipient: String, message: String): Unit =
    throw UnsupportedOperationException("Direct SMS is unavailable on this platform")
actual fun incomingSms(): Flow<IncomingSmsMessage> = flow {
    throw UnsupportedOperationException("Incoming SMS is unavailable on this platform")
}
