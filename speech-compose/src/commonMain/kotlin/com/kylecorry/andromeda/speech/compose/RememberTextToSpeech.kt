package com.kylecorry.andromeda.speech.compose

import androidx.compose.runtime.Composable
import com.kylecorry.andromeda.speech.SpeechSynthesizer

/** Remembers a platform speech engine and cancels any current utterance when leaving composition. */
@Composable
expect fun rememberTextToSpeech(): SpeechSynthesizer
