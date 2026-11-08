package com.kylecorry.andromeda.sms.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.kylecorry.andromeda.sms.SmsComposer
import com.kylecorry.andromeda.sms.smsComposer

@Composable
actual fun rememberSmsComposer(): SmsComposer {
    val context = LocalContext.current
    return remember(context) { smsComposer(context) }
}
