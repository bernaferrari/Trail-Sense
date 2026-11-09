package com.kylecorry.sol.science.geography.formatting

import com.kylecorry.sol.units.Coordinate

public interface CoordinateFormat {
    public fun toString(coordinate: Coordinate): String
    public fun parse(text: String): Coordinate?
}
