package com.kylecorry.andromeda.sms.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.kylecorry.andromeda.sms.SmsComposer
import com.kylecorry.andromeda.sms.smsComposer

@Composable
actual fun rememberSmsComposer(): SmsComposer = remember { smsComposer() }
