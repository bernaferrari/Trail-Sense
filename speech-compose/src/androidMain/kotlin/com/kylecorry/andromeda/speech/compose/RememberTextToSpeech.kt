package com.kylecorry.andromeda.speech.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.kylecorry.andromeda.speech.SpeechSynthesizer
import com.kylecorry.andromeda.speech.textToSpeech

@Composable
actual fun rememberTextToSpeech(): SpeechSynthesizer {
    val context = LocalContext.current.applicationContext
    val speech = remember(context) { textToSpeech(context) }
    DisposableEffect(speech) { onDispose { speech.cancel() } }
    return speech
}
