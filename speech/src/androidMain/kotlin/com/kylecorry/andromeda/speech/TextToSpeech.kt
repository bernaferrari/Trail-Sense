package com.kylecorry.andromeda.speech

import android.content.Context
import com.ksensor.core.context.KSensorContext
import java.util.*
import android.speech.tts.TextToSpeech as TTS


private class AndroidSpeechSynthesizer(private val context: Context) : SpeechSynthesizer {

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

public actual fun textToSpeech(): SpeechSynthesizer = AndroidSpeechSynthesizer(KSensorContext.get())

public fun textToSpeech(context: Context): SpeechSynthesizer = AndroidSpeechSynthesizer(context.applicationContext)
