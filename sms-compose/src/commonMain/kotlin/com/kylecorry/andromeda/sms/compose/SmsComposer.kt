package com.kylecorry.andromeda.sms.compose

import androidx.compose.runtime.Composable
import com.kylecorry.andromeda.sms.SmsComposer

/** Remembers a system message composer for the current screen. */
@Composable
expect fun rememberSmsComposer(): SmsComposer
