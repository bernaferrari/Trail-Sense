package com.kylecorry.andromeda.signal

interface CellSignalDistanceCalculator {
    fun getTimingAdvanceDistance(timingAdvance: Int): Float
    fun getTimingAdvanceDistanceError(timingAdvance: Int): Float
}