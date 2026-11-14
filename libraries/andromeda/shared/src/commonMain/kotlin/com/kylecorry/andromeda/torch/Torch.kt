package com.kylecorry.andromeda.torch

public interface Torch {

    /**
     * The number of brightness levels supported by the torch
     */
    public val brightnessLevels: Int

    /**
     * Turn the torch on
     */
    public fun on()

    /**
     * Turn on the torch at the set brightness
     * @param brightness the brightness in [0, 1]
     */
    public fun on(brightness: Float)

    /**
     * Turn the torch off
     */
    public fun off()

    /**
     * Determines if the device has a torch
     * @return true if a torch exists, false otherwise
     */
    public fun isAvailable(): Boolean

    /**
     * Determines if the torch is dimmable
     */
    public fun isDimmable(): Boolean
}