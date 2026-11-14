package com.kylecorry.andromeda.sms

import kotlinx.coroutines.flow.Flow

/** Whether the platform exposes direct SMS APIs; permissions and service availability may differ. */
public expect val directSmsSupported: Boolean

/** Submits a text message for direct sending; this does not report delivery status. */
public expect fun sendSms(recipient: String, message: String)

/** Receives incoming SMS messages while the flow is collected. */
public expect fun incomingSms(): Flow<IncomingSmsMessage>
