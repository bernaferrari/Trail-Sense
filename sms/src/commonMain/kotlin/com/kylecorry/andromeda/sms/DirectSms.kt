package com.kylecorry.andromeda.sms

import kotlinx.coroutines.flow.Flow

/** Whether the platform exposes direct SMS APIs; permissions and service availability may differ. */
expect val directSmsSupported: Boolean

/** Submits a text message for direct sending; this does not report delivery status. */
expect fun sendSms(recipient: String, message: String)

/** Receives incoming SMS messages while the flow is collected. */
expect fun incomingSms(): Flow<IncomingSmsMessage>
