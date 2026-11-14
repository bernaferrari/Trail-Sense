package com.kylecorry.andromeda.core.sensors

import com.kylecorry.sol.units.Speed

public interface Thermometer : Sensor { public val temperature: Float }
public interface Altimeter : Sensor { public val altitude: Float }
public interface Speedometer : Sensor { public val speed: Speed }
