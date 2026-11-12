package com.kylecorry.trail_sense.tools.paths.ui.commands

import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.shared.text.AppStrings

import com.kylecorry.trail_sense.shared.dem.ElevationModel

import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.dialogs.withCancelableProgress
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ReplacePathElevationsCommand(
    private val scope: CoroutineScope,
    private val pathService: IPathService = getAppService<IPathService>()
) : IPathCommand {

    override fun execute(path: Path) {
        Alerts.dialog(AppHost.current(),
            getAppService<AppStrings>().get(Res.string.replace_elevations),
            getAppService<AppStrings>().get(Res.string.replace_elevations_confirmation)
        ) { cancelled ->
            if (cancelled) {
                return@dialog
            }

            scope.launch {
                val elevations = mutableMapOf<Long, Float>()
                var job: Job? = null
                Alerts.withCancelableProgress(AppHost.current(),
                    getAppService<AppStrings>().get(Res.string.loading),
                    onCancel = { job?.cancel() }
                ) { setProgress ->
                    job = launch {
                        val points = pathService.getWaypoints(path.id)
                        points.forEachIndexed { index, point ->
                            val elevation = getAppService<ElevationModel>().getElevation(point.coordinate)
                            setProgress((index + 1) / points.size.coerceAtLeast(1).toFloat())
                            if (elevation.hasData) {
                                elevations[point.id] = elevation.elevation
                            }
                        }
                    }
                    job.join()
                }

                if (job?.isCancelled == true) {
                    return@launch
                }

                val loading = onMain {
                    Alerts.loading(AppHost.current(), getAppService<AppStrings>().get(Res.string.saving))
                }
                onIO {
                    pathService.updateWaypointElevations(path.id, elevations)
                }
                onMain {
                    loading.dismiss()
                }
            }
        }
    }
}
