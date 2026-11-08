package com.kylecorry.andromeda.speech.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.kylecorry.andromeda.speech.ITextToSpeech
import com.kylecorry.andromeda.speech.TextToSpeech

@Composable
actual fun rememberTextToSpeech(): ITextToSpeech {
    val context = LocalContext.current.applicationContext
    val speech = remember(context) { TextToSpeech(context) }
    DisposableEffect(speech) { onDispose { speech.cancel() } }
    return speech
}
