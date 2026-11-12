package com.kylecorry.trail_sense.tools.paths.ui.commands

import kotlinx.coroutines.CoroutineScope
import com.kylecorry.trail_sense.tools.beacons.infrastructure.IBeaconNavigator
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.beacon.IPathPointBeaconConverter
import kotlinx.coroutines.launch

class NavigateToPointCommand(
    private val scope: CoroutineScope,
    private val converter: IPathPointBeaconConverter,
    private val beaconNavigator: IBeaconNavigator
) : IPathPointCommand {

    override fun execute(path: Path, point: PathPoint) {
        scope.launch {
            val beacon = converter.toBeacon(path, point)
            beaconNavigator.navigateTo(beacon)
        }
    }
}
