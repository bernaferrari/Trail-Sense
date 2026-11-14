package com.kylecorry.sol.science.geophysics

import com.kylecorry.sol.units.Coordinate

public fun interface VerticalDeflectionModel {
    public fun getVerticalDeflection(location: Coordinate): VerticalDeflection?
}
