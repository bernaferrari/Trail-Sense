package com.kylecorry.trail_sense.tools.paths.infrastructure.commands

import android.content.Context
import com.kylecorry.andromeda.signal.CellNetwork
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.getInstance
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.IBeaconService
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint

class CreateLastSignalBeaconCommand(private val context: Context) {

    private val beaconService = getAppService<IBeaconService>()
    private val formatter = FormatService.getInstance(context)

    suspend fun execute(point: PathPoint) {
        val cellSignal = point.cellSignal ?: return
        onIO {
            beaconService.add(
                Beacon(
                    0L,
                    context.getString(
                        R.string.last_signal_beacon_name,
                        formatter.formatCellNetwork(
                            CellNetwork.entries
                                .first { it.id == cellSignal.network.id }
                        ),
                        formatter.formatQuality(cellSignal.quality)
                    ),
                    point.coordinate,
                    false,
                    elevation = point.elevation,
                    temporary = true,
                    owner = BeaconOwner.CellSignal,
                    color = AppColor.Orange.color
                ))
        }
    }
}
