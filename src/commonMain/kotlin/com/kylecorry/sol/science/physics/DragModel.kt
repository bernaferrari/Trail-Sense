package com.kylecorry.sol.science.physics

import com.kylecorry.sol.math.Vector2

public interface DragModel {
    public fun getDragAcceleration(velocity: Vector2): Vector2
}
