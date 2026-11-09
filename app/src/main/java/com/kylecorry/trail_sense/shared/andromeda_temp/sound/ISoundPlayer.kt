package com.kylecorry.trail_sense.shared.andromeda_temp.sound

interface ISoundPlayer {
    fun on()
    fun off()
    fun isOn(): Boolean
    fun fadeOn()
    fun fadeOff(releaseWhenOff: Boolean)
    fun setVolume(volume: Float)
    fun release()
}