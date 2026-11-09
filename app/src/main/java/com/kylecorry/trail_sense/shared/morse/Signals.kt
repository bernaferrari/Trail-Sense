package com.kylecorry.trail_sense.shared.morse

import kotlin.time.Duration.Companion.seconds

import kotlin.time.Duration

object Signals {

    fun sos(dotDuration: Duration): List<Signal> {
        return listOf(
            Signal.on(dotDuration),
            Signal.off(dotDuration),
            Signal.on(dotDuration),
            Signal.off(dotDuration),
            Signal.on(dotDuration),
            Signal.off(dotDuration),
            Signal.on(dotDuration.times(3)),
            Signal.off(dotDuration),
            Signal.on(dotDuration.times(3)),
            Signal.off(dotDuration),
            Signal.on(dotDuration.times(3)),
            Signal.off(dotDuration),
            Signal.on(dotDuration),
            Signal.off(dotDuration),
            Signal.on(dotDuration),
            Signal.off(dotDuration),
            Signal.on(dotDuration)
        )
    }

    fun help(): List<Signal> {
        return listOf(
            Signal.on(2.seconds),
            Signal.off(1.seconds),
            Signal.on(2.seconds),
            Signal.off(1.seconds),
            Signal.on(2.seconds),
            Signal.off(3.seconds)
        )
    }

    fun acknowledged(): List<Signal> {
        return listOf(Signal.on(2.seconds))
    }

    fun comeHere(): List<Signal> {
        return listOf(
            Signal.on(2.seconds),
            Signal.off(1.seconds),
            Signal.on(2.seconds),
        )
    }

}
