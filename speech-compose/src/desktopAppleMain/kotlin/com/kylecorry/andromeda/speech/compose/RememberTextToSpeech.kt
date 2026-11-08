package com.kylecorry.andromeda.speech.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.kylecorry.andromeda.speech.ITextToSpeech
import com.kylecorry.andromeda.speech.TextToSpeech

@Composable
actual fun rememberTextToSpeech(): ITextToSpeech {
    val speech = remember { TextToSpeech() }
    DisposableEffect(speech) { onDispose { speech.cancel() } }
    return speech
}
