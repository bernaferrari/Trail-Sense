package com.kylecorry.andromeda.core.sensors

import com.kylecorry.sol.units.Speed

interface Thermometer : Sensor { val temperature: Float }
interface Altimeter : Sensor { val altitude: Float }
interface Speedometer : Sensor { val speed: Speed }
