package com.kylecorry.andromeda.core.sensors

interface IThermometer: ISensor, Thermometer {
    override val temperature: Float
}