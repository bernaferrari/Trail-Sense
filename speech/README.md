# Text to speech

`ITextToSpeech` is shared across platforms. Construct the platform `TextToSpeech` implementation and pass it to shared code. Android requires a `Context`; iOS and JVM do not.

The JVM implementation uses macOS `say`, Windows PowerShell with `System.Speech`, or Linux `espeak-ng` (falling back to `espeak`). The relevant operating-system speech engine must be installed. Calling `speak` replaces any current utterance; `cancel` stops it.
