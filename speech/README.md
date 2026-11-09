# Text to speech

Call `textToSpeech()` from common code to create a `SpeechSynthesizer`. Initialize KSensor's application context first on Android, or call the Android `textToSpeech(context)` overload. Call `close()` when finished.

For Compose apps, the optional `speech-compose` module provides `rememberTextToSpeech()` in common code and cancels speech when the composable leaves composition.

The JVM implementation uses macOS `say`, Windows PowerShell with `System.Speech`, or Linux `espeak-ng` (falling back to `espeak`). The relevant operating-system speech engine must be installed. Calling `speak` replaces any current utterance; `cancel` stops it.
