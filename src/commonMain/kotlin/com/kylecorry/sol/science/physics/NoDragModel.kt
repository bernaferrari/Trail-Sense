package com.kylecorry.sol.science.physics

import com.kylecorry.sol.math.Vector2

public class NoDragModel : DragModel {
    override fun getDragAcceleration(velocity: Vector2): Vector2 {
        return Vector2.zero
    }
}
