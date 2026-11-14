package com.kylecorry.sol.science.geology

import com.kylecorry.sol.units.Coordinate

public interface IGeoArea {
    public fun contains(location: Coordinate): Boolean
}
