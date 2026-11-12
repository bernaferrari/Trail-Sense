package com.kylecorry.trail_sense.tools.flashlight.infrastructure

class TorchFlashlightStrategy(private val flashlight: FlashlightSwitch) : IFlashlightStrategy {
    override fun start() {
        flashlight.turnOn()
    }

    override fun stop() {
        flashlight.turnOff()
    }
}
