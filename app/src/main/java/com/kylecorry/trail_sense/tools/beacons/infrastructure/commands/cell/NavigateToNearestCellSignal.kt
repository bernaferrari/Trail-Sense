package com.kylecorry.trail_sense.tools.beacons.infrastructure.commands.cell

import android.content.Context
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.commands.CoroutineValueCommand
import com.kylecorry.trail_sense.shared.sensors.ILocationSubsystem
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.IBeaconService
import com.kylecorry.trail_sense.tools.navigation.infrastructure.Navigator

class NavigateToNearestCellSignal(context: Context) : CoroutineValueCommand<Boolean> {

    private val lookup = CellSignalLookup(context)
    private val navigator = Navigator.getInstance(context)
    private val location = getAppService<ILocationSubsystem>()
    private val beacons = getAppService<IBeaconService>()

    override suspend fun execute(): Boolean {
        val nearest = lookup.getNearestCellSignalBeacon(location.location)
        return if (nearest != null) {
            val id = beacons.add(nearest)
            navigator.navigateTo(id)
            true
        } else {
            false
        }
    }
}
