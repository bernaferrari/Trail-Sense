package com.kylecorry.andromeda.speech

public interface SpeechSynthesizer : AutoCloseable {
    /** Speaks using the given BCP 47 language tag, or the platform default. */
    public fun speak(text: String, locale: String? = null)
    public fun cancel()
    override fun close(): Unit = cancel()
}

/** Creates a speech engine. On Android, initialize the KSensor application context first. */
public expect fun textToSpeech(): SpeechSynthesizer
