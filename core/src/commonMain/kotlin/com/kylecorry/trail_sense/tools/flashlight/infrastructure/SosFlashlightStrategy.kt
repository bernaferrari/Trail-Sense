package com.kylecorry.trail_sense.tools.flashlight.infrastructure

import kotlin.time.Duration.Companion.milliseconds

import com.kylecorry.trail_sense.shared.morse.ISignalingDevice
import com.kylecorry.trail_sense.shared.morse.Signal
import com.kylecorry.trail_sense.shared.morse.SignalPlayer
import com.kylecorry.trail_sense.shared.morse.Signals

class SosFlashlightStrategy(private val flashlight: FlashlightSwitch) : IFlashlightStrategy {

    private val player by lazy {
        SignalPlayer(
            ISignalingDevice.from(
                flashlight::turnOn,
                flashlight::turnOff
            )
        )
    }

    override fun start() {
        val sos = Signals.sos(200.milliseconds) + listOf(
            Signal.off((200L * 7).milliseconds)
        )
        player.play(sos, true)
    }

    override fun stop() {
        player.cancel()
        flashlight.turnOff()
    }
}
