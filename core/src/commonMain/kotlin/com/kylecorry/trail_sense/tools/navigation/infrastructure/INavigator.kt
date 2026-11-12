package com.kylecorry.trail_sense.tools.navigation.infrastructure

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.navigation.domain.guidance.Guidance
import kotlinx.coroutines.flow.StateFlow

/** Starts and stops navigating to a beacon (or to a spot, through a hidden temporary beacon). */
interface INavigator {

    /**
     * Navigates to [location] through a hidden temporary beacon named [name]. Without an [elevation], the beacon
     * takes the DEM's elevation when [useDemElevation].
     */
    fun navigateTo(
        location: Coordinate,
        name: String = "",
        owner: BeaconOwner = BeaconOwner.User,
        elevation: Float? = null,
        useDemElevation: Boolean = elevation == null,
    )

    fun navigateTo(beacon: Beacon)

    fun navigateTo(beaconId: Long)

    fun cancelBeaconNavigation()

    fun getBeaconDestinationId(): Long?

    /** Where the person is navigating: a path, a beacon or a bearing, in that order, or null. */
    suspend fun getDestination(): Destination?

    /** True while a path navigation is being restored in the background. */
    val isRestoringNavigation: StateFlow<Boolean>

    /** Stops every kind of navigation and returns an action that puts it back (for undo). */
    suspend fun cancelAllNavigation(): suspend () -> Unit

    /** How to get from [location] to [destination]; [useTrueNorthOverride] replaces the north preference. */
    fun getGuidance(
        destination: Destination,
        location: Coordinate,
        elevation: Distance,
        declination: Float,
        useTrueNorthOverride: Boolean? = null
    ): Guidance
}
