package com.kylecorry.andromeda.sms

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

public actual val directSmsSupported: Boolean = false
public actual fun sendSms(recipient: String, message: String): Unit =
    throw UnsupportedOperationException("Direct SMS is unavailable on this platform")
public actual fun incomingSms(): Flow<IncomingSmsMessage> = flow {
    throw UnsupportedOperationException("Incoming SMS is unavailable on this platform")
}
