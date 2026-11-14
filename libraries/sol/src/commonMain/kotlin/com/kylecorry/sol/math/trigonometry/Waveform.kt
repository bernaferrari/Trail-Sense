package com.kylecorry.sol.math.trigonometry

public interface Waveform {
    public val amplitude: Float
    public val frequency: Float
    public val horizontalShift: Float
    public val verticalShift: Float

    public fun calculate(x: Float): Float
}
