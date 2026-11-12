package com.kylecorry.trail_sense.shared.sound

import com.kylecorry.andromeda.sound.SoundPlayer

/** Makes a player for a continuous tone at [frequencyHz]; supplied by each platform's service registry. */
fun interface TonePlayerFactory {
    fun create(frequencyHz: Int): SoundPlayer
}
