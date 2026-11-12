package com.kylecorry.trail_sense.tools.navigation.infrastructure

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner

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
}
