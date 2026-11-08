package com.kylecorry.andromeda.sound

import kotlinx.coroutines.flow.Flow

/** Streams normalized mono PCM while collected. Microphone permission is required. */
expect fun microphoneSamples(sampleRate: Int = 16000): Flow<FloatArray>
