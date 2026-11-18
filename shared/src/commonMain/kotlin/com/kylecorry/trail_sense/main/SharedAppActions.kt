package com.kylecorry.trail_sense.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.camera.rememberSharedPermissionRequester
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import com.kylecorry.trail_sense.shared.sensors.ISensorSubsystem
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.shared.views.compose.AppPermission
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.IBeaconService
import com.kylecorry.trail_sense.tools.paths.PathsToolServices
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCatalog
import kotlinx.coroutines.flow.Flow

internal enum class SharedAppAction {
    StartBacktrack,
    OpenBacktrack,
    SaveCurrentPlace
}

@Composable
internal fun BindSharedAppActions(navigator: TrailSenseNavigator, actions: Flow<SharedAppAction>) {
    val requestPermission = rememberSharedPermissionRequester()
    LaunchedEffect(navigator, actions) {
        val strings = getAppService<AppStrings>()
        actions.collect { action ->
            when (action) {
                SharedAppAction.OpenBacktrack -> navigator.navigate(Route.Paths)
                SharedAppAction.StartBacktrack -> {
                    navigator.navigate(Route.Paths)
                    val service = getAppService<ToolCatalog>().getService(PathsToolServices.BACKTRACK)
                    if (service?.isSupported == true && !service.isRunning()) {
                        if (requestPermission(AppPermission.Backtrack)) service.enable()
                        else AppHost.current().showSnackbar(strings.get(Res.string.backtrack_no_permission))
                    }
                }
                SharedAppAction.SaveCurrentPlace -> {
                    val sensors = getAppService<ISensorSubsystem>()
                    val coordinate = sensors.getLocation()
                    if (coordinate == Coordinate.zero && sensors.lastKnownLocationAccuracy == null) {
                        AppHost.current().showSnackbar(strings.get(Res.string.shared_map_location_unavailable))
                    } else {
                        val places = getAppService<IBeaconService>()
                        val id = places.add(Beacon(0, strings.get(Res.string.my_location), coordinate))
                        navigator.navigate(Route.BeaconDetails(id))
                    }
                }
            }
        }
    }
}
