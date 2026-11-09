package com.kylecorry.andromeda.speech.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.kylecorry.andromeda.speech.SpeechSynthesizer
import com.kylecorry.andromeda.speech.textToSpeech

@Composable
actual fun rememberTextToSpeech(): SpeechSynthesizer {
    val speech = remember { textToSpeech() }
    DisposableEffect(speech) { onDispose { speech.cancel() } }
    return speech
}
