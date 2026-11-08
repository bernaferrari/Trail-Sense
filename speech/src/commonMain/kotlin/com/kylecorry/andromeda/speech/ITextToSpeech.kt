package com.kylecorry.andromeda.speech

interface ITextToSpeech {
    /** Speaks using the given BCP 47 language tag, or the platform default. */
    fun speak(text: String, locale: String? = null)
    fun cancel()
}
