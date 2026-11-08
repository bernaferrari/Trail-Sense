package com.kylecorry.andromeda.core.sensors

interface IAltimeter: ISensor, Altimeter {
    override val altitude: Float
}