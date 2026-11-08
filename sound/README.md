# Sound

`playTone(frequencyHz)` plays a sine tone until its coroutine is cancelled; pass a finite `duration` for timed playback. `playGeneratedSound` streams a custom normalized waveform with the same cancellation and resource cleanup behavior. These functions use the shared `PcmSpeaker` backend and do not buffer an entire sound in memory.

`microphoneSamples()` streams normalized mono PCM while collected, subject to platform microphone permission. `AudioPlayer` exposes Gadulka for encoded local and remote audio; desktop applications must provide JavaFX media and its platform runtime libraries.
