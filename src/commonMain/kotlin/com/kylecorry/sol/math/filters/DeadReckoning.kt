package com.kylecorry.sol.math.filters

public class DeadReckoning(initialPosition: Float = 0f) {

    public var position: Float = initialPosition
        private set

    public fun calculate(velocity: Float, dt: Float): Float {
        position += velocity * dt
        return position
    }
}
