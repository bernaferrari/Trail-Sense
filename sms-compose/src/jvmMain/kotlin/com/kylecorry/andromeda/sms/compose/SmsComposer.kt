package com.kylecorry.andromeda.sms.compose

import androidx.compose.runtime.Composable
import com.kylecorry.andromeda.sms.SmsComposer

private val unsupportedComposer = SmsComposer { _, _ ->
    throw UnsupportedOperationException("The system message composer is unavailable on desktop")
}

@Composable
actual fun rememberSmsComposer(): SmsComposer = unsupportedComposer
