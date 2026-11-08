package com.kylecorry.andromeda.speech

import android.content.Context
import java.util.*
import android.speech.tts.TextToSpeech as TTS


class TextToSpeech(private val context: Context) : ITextToSpeech {

    private val lock = Any()
    private var tts: TTS? = null
    private var generation = 0L

    override fun speak(text: String, locale: String?) {
        synchronized(lock) {
            cancelLocked()
            val request = generation
            tts = TTS(context.applicationContext) { status ->
                synchronized(lock) {
                    if (status != TTS.ERROR && request == generation) {
                        tts?.language = locale?.let(Locale::forLanguageTag) ?: Locale.getDefault()
                        tts?.speak(text, TTS.QUEUE_FLUSH, null, null)
                    }
                }
            }
        }
    }

    override fun cancel() {
        synchronized(lock) { cancelLocked() }
    }

    private fun cancelLocked() {
        generation++
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

}
