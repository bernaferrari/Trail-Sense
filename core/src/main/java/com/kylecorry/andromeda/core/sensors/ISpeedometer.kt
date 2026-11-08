package com.kylecorry.andromeda.core.sensors

import com.kylecorry.sol.units.Speed

interface ISpeedometer: ISensor, Speedometer {
    override val speed: Speed
}