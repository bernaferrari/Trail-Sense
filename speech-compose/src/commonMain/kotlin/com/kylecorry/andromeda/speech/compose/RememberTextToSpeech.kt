package com.kylecorry.andromeda.speech.compose

import androidx.compose.runtime.Composable
import com.kylecorry.andromeda.speech.ITextToSpeech

/** Remembers a platform speech engine and cancels any current utterance when leaving composition. */
@Composable
expect fun rememberTextToSpeech(): ITextToSpeech
