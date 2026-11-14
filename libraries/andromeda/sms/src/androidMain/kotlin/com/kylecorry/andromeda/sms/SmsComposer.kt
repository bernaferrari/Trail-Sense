package com.kylecorry.andromeda.sms

import android.content.Context
import android.content.Intent
import android.net.Uri

public fun smsComposer(context: Context): SmsComposer = SmsComposer { recipient, message ->
    val intent = Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", recipient, null))
        .putExtra("sms_body", message)
    if (context !is android.app.Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
