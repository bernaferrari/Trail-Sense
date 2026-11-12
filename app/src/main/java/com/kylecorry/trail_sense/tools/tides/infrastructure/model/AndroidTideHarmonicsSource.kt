package com.kylecorry.trail_sense.tools.tides.infrastructure.model

import android.content.Context
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.sensors.ILocationSubsystem
import com.kylecorry.trail_sense.tools.tides.domain.TideService
import com.kylecorry.trail_sense.tools.tides.domain.TideTable
import com.kylecorry.trail_sense.tools.tides.domain.waterlevel.TideHarmonicsSource
import com.kylecorry.trail_sense.tools.tides.domain.waterlevel.TideTableWaterLevelCalculator

/** The tide model bundled in the app's assets, and the device's last known location. */
class AndroidTideHarmonicsSource(private val context: Context) : TideHarmonicsSource {
    private val locationSubsystem by lazy { getAppService<ILocationSubsystem>() }

    override suspend fun getHarmonics(location: Coordinate): TideModelResult? {
        return TideModel.getHarmonics(context, location)
    }

    override val currentLocation: Coordinate
        get() = locationSubsystem.location
}

fun TideService(context: Context): TideService = TideService(AndroidTideHarmonicsSource(context))

fun TideTableWaterLevelCalculator(context: Context, table: TideTable): TideTableWaterLevelCalculator {
    return TideTableWaterLevelCalculator(AndroidTideHarmonicsSource(context), table)
}
