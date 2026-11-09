package com.kylecorry.andromeda.speech

import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance

private class IosSpeechSynthesizer : SpeechSynthesizer {
    private val synthesizer = AVSpeechSynthesizer()

    override fun speak(text: String, locale: String?) {
        cancel()
        val utterance = AVSpeechUtterance.speechUtteranceWithString(text)
        if (locale != null) utterance.voice = AVSpeechSynthesisVoice.voiceWithLanguage(locale)
        synthesizer.speakUtterance(utterance)
    }

    override fun cancel() {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
    }
}

actual fun textToSpeech(): SpeechSynthesizer = IosSpeechSynthesizer()
