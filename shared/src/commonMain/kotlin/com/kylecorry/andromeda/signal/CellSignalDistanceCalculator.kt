package com.kylecorry.andromeda.signal

public interface CellSignalDistanceCalculator {
    public fun getTimingAdvanceDistance(timingAdvance: Int): Float
    public fun getTimingAdvanceDistanceError(timingAdvance: Int): Float
}