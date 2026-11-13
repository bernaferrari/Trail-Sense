package com.kylecorry.trail_sense.tools.navigation.infrastructure

import kotlinx.coroutines.flow.Flow
import android.content.Context
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.luna.concurrency.BackgroundTask
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.settings.PreferenceKeys
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.events.ToolBroadcasts
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.preferences.SessionStateSubsystem
import com.kylecorry.trail_sense.shared.preferences.onEachChange
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.IBeaconService
import com.kylecorry.trail_sense.tools.navigation.NavigationToolRegistration
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationBearing
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationBearingCalculator
import com.kylecorry.trail_sense.tools.navigation.domain.guidance.Guidance
import com.kylecorry.trail_sense.tools.navigation.domain.guidance.GuidanceInput
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathFollow
import com.kylecorry.trail_sense.tools.navigation.infrastructure.persistence.NavigationBearingService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class Navigator private constructor(context: Context) : INavigator {

    private val prefs = SessionStateSubsystem.getInstance(context).preferences
    private val userPrefs = DependencyRegistry.get<UserPreferences>()
    private val beacons = getAppService<IBeaconService>()
    private val bearings = NavigationBearingService.getInstance(context)
    private val locationSubsystem = DependencyRegistry.get<LocationSubsystem>()

    // Flows
    private val beaconDestinationId = MutableStateFlow(prefs.getLong(BEACON_DESTINATION_ID_KEY))
    private val beaconDestinationInvalidationKey = MutableStateFlow(0)

    // Bearings
    private val navigationBearing = bearings.getBearing()

    private val bearingInvalidationKey = MutableStateFlow(0)

    private val bearingDestination = combine(navigationBearing, bearingInvalidationKey) { bearing, _ ->
        bearing?.let {
            // The start location is hidden when the bearing is not locked to it, so it is not seen by consumers
            val startLocation = it.startLocation.takeIf { userPrefs.navigation.lockBearingToLocation }
            Destination.Bearing(
                Bearing.from(it.bearing),
                userPrefs.compass.useTrueNorth,
                getDeclination(startLocation ?: locationSubsystem.location),
                startLocation,
                userPrefs.navigation.bearingDistance
            )
        }
    }

    // TODO: Standardized navigation instructions
    // NavigationInstructions
    // Bearing
    // Distance?
    // ETA?
    // Elevation change?
    // Beacons = always to the beacon, bearings = if off track, then back to the bearing reading otherwise bearing end point (or just the bearing if no location info)

    private val beaconDestination = combine(beaconDestinationId, beaconDestinationInvalidationKey) { id, _ ->
        id?.let { beacons.getBeacon(it) }?.let { Destination.Beacon(it) }
    }

    private val pathNavigator = PathNavigator(context)
    override val isRestoringNavigation: StateFlow<Boolean> = pathNavigator.isRestoring

    override val destination: Flow<Destination?> =
        combine(bearingDestination, beaconDestination, pathNavigator.destination) { bearing, beacon, path ->
            path ?: beacon ?: bearing
        }


    suspend fun navigateAlongPath(path: Path, follow: PathFollow) {
        pathNavigator.navigate(path, follow)
        clearBeaconDestination()
        clearBearing()
        ToolBroadcasts.broadcast(NavigationToolRegistration.BROADCAST_NAVIGATION_STARTED)
    }

    private val listenTask = BackgroundTask {
        destination.collect {
            ToolBroadcasts.broadcast(NavigationToolRegistration.BROADCAST_DESTINATION_CHANGED)
        }
    }

    init {
        listenTask.start()
        val lockBearingKey = PreferenceKeys.lockBearingToLocation
        PreferencesSubsystem.getInstance(context).preferences.onEachChange { key ->
            if (key == lockBearingKey) {
                bearingInvalidationKey.update { it + 1 }
            }
        }
        // Keeps the beacon destination in sync with external changes to the preferences
        prefs.onEachChange { key ->
            if (key == BEACON_DESTINATION_ID_KEY) {
                val id = prefs.getLong(BEACON_DESTINATION_ID_KEY)
                if (beaconDestinationId.value != id) {
                    updateBeaconDestination(id)
                }
            }
        }
    }

    override fun navigateTo(
        location: Coordinate,
        name: String,
        owner: BeaconOwner,
        elevation: Float?,
        useDemElevation: Boolean,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val beacon = Beacon.temporary(
                location,
                elevation = if (useDemElevation) DEM.getElevation(location).elevation else elevation,
                name = name,
                visible = false,
                owner = owner
            )
            val id = beacons.add(beacon)
            navigateTo(id)
        }
    }

    override fun navigateTo(beacon: Beacon) {
        navigateTo(beacon.id)
    }

    override fun navigateTo(beaconId: Long) {
        pathNavigator.cancel()
        CoroutineScope(Dispatchers.IO).launch { clearBearing() }
        setBeaconDestination(beaconId)
        ToolBroadcasts.broadcast(NavigationToolRegistration.BROADCAST_NAVIGATION_STARTED)
    }

    private fun setBeaconDestination(beaconId: Long) {
        updateBeaconDestination(beaconId)
        prefs.putLong(BEACON_DESTINATION_ID_KEY, beaconId)
    }

    override fun cancelBeaconNavigation() {
        val wasNavigating = getBeaconDestinationId() != null
        clearBeaconDestination()
        if (wasNavigating) {
            ToolBroadcasts.broadcast(NavigationToolRegistration.BROADCAST_NAVIGATION_STOPPED)
        }
    }

    private fun clearBeaconDestination() {
        updateBeaconDestination(null)
        prefs.remove(BEACON_DESTINATION_ID_KEY)
    }

    private fun updateBeaconDestination(beaconId: Long?) {
        beaconDestinationId.value = beaconId
        beaconDestinationInvalidationKey.update { it + 1 }
    }

    override fun cancelPathNavigation(pathId: Long) {
        if (pathNavigator.cancel(pathId)) {
            ToolBroadcasts.broadcast(NavigationToolRegistration.BROADCAST_NAVIGATION_STOPPED)
        }
    }

    /**
     * Cancels all navigation.
     * @return a function which restores the navigation that was cancelled, unless the user has
     * started navigating somewhere else since
     */
    override suspend fun cancelAllNavigation(): suspend () -> Unit {
        val beaconId = getBeaconDestinationId()
        val bearing = navigationBearing.firstOrNull()
        val pathSession = pathNavigator.currentSession()

        pathNavigator.cancel()
        clearBeaconDestination()
        clearBearing()

        if (beaconId != null || bearing != null || pathSession != null) {
            ToolBroadcasts.broadcast(NavigationToolRegistration.BROADCAST_NAVIGATION_STOPPED)
        }

        return restore@{
            if (isNavigating()) {
                return@restore
            }
            beaconId?.let { setBeaconDestination(it) }
            bearing?.let { bearings.setBearing(it) }
            pathSession?.let { pathNavigator.restore(it) }
            if (beaconId != null || bearing != null || pathSession != null) {
                ToolBroadcasts.broadcast(NavigationToolRegistration.BROADCAST_NAVIGATION_STARTED)
            }
        }
    }

    private suspend fun isNavigating(): Boolean {
        return getBeaconDestinationId() != null ||
                navigationBearing.firstOrNull() != null ||
                pathNavigator.currentSession() != null
    }

    override fun getBeaconDestinationId(): Long? {
        return beaconDestinationId.value
    }

    fun getDestinationId(): Long? {
        return getBeaconDestinationId()
    }

    override suspend fun getDestination(): Destination? {
        pathNavigator.awaitRestore()
        return destination.firstOrNull()
    }

    override suspend fun navigateToBearing(bearing: Float, startingLocation: Coordinate?) {
        pathNavigator.cancel()
        clearBeaconDestination()
        val navigationBearing = NavigationBearing(
            0,
            bearing,
            startingLocation,
            isActive = true,
            startTime = Clock.System.now()
        )
        bearings.setBearing(navigationBearing)
        ToolBroadcasts.broadcast(NavigationToolRegistration.BROADCAST_NAVIGATION_STARTED)
    }

    suspend fun clearBearing() {
        bearings.setBearing(null)
    }

    override fun getBearing(
        myLocation: Coordinate,
        destination: Destination
    ): Bearing {
        return getBearing(myLocation, destination, getDeclination(myLocation))
    }

    fun getBearing(
        myLocation: Coordinate,
        destination: Destination,
        declination: Float
    ): Bearing {
        return getGuidance(
            destination,
            myLocation,
            locationSubsystem.elevation,
            declination
        ).targetBearing
    }

    override fun getGuidance(
        destination: Destination,
        location: Coordinate,
        elevation: Distance,
        declination: Float,
        useTrueNorthOverride: Boolean?
    ): Guidance {
        val useTrueNorth = destination.usesTrueNorth(
            userPrefs.compass.useTrueNorth,
            useTrueNorthOverride
        )
        return destination.getGuidance(
            GuidanceInput(location, elevation, declination, useTrueNorth)
        )
    }

    private fun getDeclination(location: Coordinate): Float {
        return NavigationBearingCalculator.getDeclination(
            location,
            locationSubsystem.elevation.meters().value,
            System.currentTimeMillis(),
            userPrefs.useAutoDeclination,
            userPrefs.declinationOverride
        )
    }

    companion object {
        internal const val BEACON_DESTINATION_ID_KEY = "last_beacon_id_long"

        private var instance: Navigator? = null

        @Synchronized
        fun getInstance(context: Context): Navigator {
            if (instance == null) {
                instance = Navigator(context.applicationContext)
            }
            return instance!!
        }
    }

}
