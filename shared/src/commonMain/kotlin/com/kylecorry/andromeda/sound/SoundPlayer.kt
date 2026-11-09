package com.kylecorry.andromeda.sound

public interface SoundPlayer {
    public fun on()
    public fun off()
    public fun isOn(): Boolean
    public fun fadeOn()
    public fun fadeOff(releaseWhenOff: Boolean)
    public fun setVolume(volume: Float)
    public fun release()
}