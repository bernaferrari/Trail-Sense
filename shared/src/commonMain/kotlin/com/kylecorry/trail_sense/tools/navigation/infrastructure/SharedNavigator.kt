package com.kylecorry.trail_sense.tools.navigation.infrastructure

import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.preferences.Preferences
import kotlinx.serialization.json.Json
import com.kylecorry.luna.concurrency.BackgroundTask
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.settings.PreferenceKeys
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dem.ElevationModel
import com.kylecorry.trail_sense.shared.events.ToolBroadcasts
import com.kylecorry.trail_sense.shared.preferences.onEachChange
import com.kylecorry.trail_sense.shared.sensors.ILocationSubsystem
import com.kylecorry.trail_sense.shared.sensors.SharedLocationSubsystem
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.tools.beacons.infrastructure.persistence.IBeaconService
import com.kylecorry.trail_sense.tools.navigation.domain.Destination
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationBearing
import com.kylecorry.trail_sense.tools.navigation.domain.NavigationBearingCalculator
import com.kylecorry.trail_sense.tools.navigation.domain.guidance.Guidance
import com.kylecorry.trail_sense.tools.navigation.domain.guidance.GuidanceInput
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathFollow
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathGuidance
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathNavigationSession
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathNavigationSessionProgress
import com.kylecorry.trail_sense.tools.navigation.domain.paths.PathRouteBuilder
import com.kylecorry.trail_sense.tools.navigation.infrastructure.persistence.NavigationBearingService
import com.kylecorry.trail_sense.tools.paths.domain.IPathService
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.hiking.HikingService
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import com.kylecorry.trail_sense.shared.coroutines.ioDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * Multiplatform [INavigator] handling beacon, bearing, and path navigation destinations.
 */
class SharedNavigator(
    private val sessionPrefs: Preferences,
    private val appPrefs: Preferences,
    private val userPrefs: UserPreferences,
    private val beacons: IBeaconService,
    private val bearings: NavigationBearingService,
    private val locationSubsystem: ILocationSubsystem,
    pathService: IPathService,
    private val elevationModel: ElevationModel
) : INavigator {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val beaconDestinationId = MutableStateFlow(sessionPrefs.getLong(BEACON_DESTINATION_ID_KEY))
    private val beaconDestinationInvalidationKey = MutableStateFlow(0)

    private val navigationBearing = bearings.getBearing()
    private val bearingInvalidationKey = MutableStateFlow(0)

    private val bearingDestination = combine(navigationBearing, bearingInvalidationKey) { bearing, _ ->
        bearing?.let {
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

    private val beaconDestination = combine(beaconDestinationId, beaconDestinationInvalidationKey) { id, _ ->
        id?.let { beacons.getBeacon(it) }?.let { Destination.Beacon(it) }
    }

    private val pathNavigator = SharedPathNavigator(
        store = SharedPathNavigationStore(sessionPrefs),
        userPrefs = userPrefs,
        locationSubsystem = locationSubsystem,
        paths = pathService
    )
    override val isRestoringNavigation: StateFlow<Boolean> = pathNavigator.isRestoring

    override val destination: Flow<Destination?> =
        combine(bearingDestination, beaconDestination, pathNavigator.destination) { bearing, beacon, path ->
            path ?: beacon ?: bearing
        }

    private val listenTask = BackgroundTask {
        destination.collect {
            ToolBroadcasts.broadcast(BROADCAST_DESTINATION_CHANGED)
        }
    }

    init {
        listenTask.start()
        val lockBearingKey = PreferenceKeys.lockBearingToLocation
        appPrefs.onEachChange { key ->
            if (key == lockBearingKey) {
                bearingInvalidationKey.update { it + 1 }
            }
        }
        sessionPrefs.onEachChange { key ->
            if (key == BEACON_DESTINATION_ID_KEY) {
                val id = sessionPrefs.getLong(BEACON_DESTINATION_ID_KEY)
                if (beaconDestinationId.value != id) {
                    updateBeaconDestination(id)
                }
            }
        }
    }

    override suspend fun navigateAlongPath(path: Path, follow: PathFollow) {
        pathNavigator.navigate(path, follow)
        clearBeaconDestination()
        clearBearing()
        ToolBroadcasts.broadcast(BROADCAST_NAVIGATION_STARTED)
    }

    override fun navigateTo(
        location: Coordinate,
        name: String,
        owner: BeaconOwner,
        elevation: Float?,
        useDemElevation: Boolean
    ) {
        CoroutineScope(ioDispatcher).launch {
            val resolvedElevation = if (useDemElevation) {
                elevationModel.getElevation(location).elevation
            } else {
                elevation
            }
            val beacon = Beacon.temporary(
                location,
                elevation = resolvedElevation,
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
        scope.launch { clearBearing() }
        setBeaconDestination(beaconId)
        ToolBroadcasts.broadcast(BROADCAST_NAVIGATION_STARTED)
    }

    private fun setBeaconDestination(beaconId: Long) {
        updateBeaconDestination(beaconId)
        sessionPrefs.putLong(BEACON_DESTINATION_ID_KEY, beaconId)
    }

    override fun cancelBeaconNavigation() {
        val wasNavigating = getBeaconDestinationId() != null
        clearBeaconDestination()
        if (wasNavigating) {
            ToolBroadcasts.broadcast(BROADCAST_NAVIGATION_STOPPED)
        }
    }

    private fun clearBeaconDestination() {
        updateBeaconDestination(null)
        sessionPrefs.remove(BEACON_DESTINATION_ID_KEY)
    }

    private fun updateBeaconDestination(beaconId: Long?) {
        beaconDestinationId.value = beaconId
        beaconDestinationInvalidationKey.update { it + 1 }
    }

    override fun cancelPathNavigation(pathId: Long) {
        if (pathNavigator.cancel(pathId)) {
            ToolBroadcasts.broadcast(BROADCAST_NAVIGATION_STOPPED)
        }
    }

    override suspend fun cancelAllNavigation(): suspend () -> Unit {
        val beaconId = getBeaconDestinationId()
        val bearing = navigationBearing.firstOrNull()
        val pathSession = pathNavigator.currentSession()

        pathNavigator.cancel()
        clearBeaconDestination()
        clearBearing()

        if (beaconId != null || bearing != null || pathSession != null) {
            ToolBroadcasts.broadcast(BROADCAST_NAVIGATION_STOPPED)
        }

        return restore@{
            if (isNavigating()) {
                return@restore
            }
            beaconId?.let { setBeaconDestination(it) }
            bearing?.let { bearings.setBearing(it) }
            pathSession?.let { pathNavigator.restore(it) }
            if (beaconId != null || bearing != null || pathSession != null) {
                ToolBroadcasts.broadcast(BROADCAST_NAVIGATION_STARTED)
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
        ToolBroadcasts.broadcast(BROADCAST_NAVIGATION_STARTED)
    }

    private suspend fun clearBearing() {
        bearings.setBearing(null)
    }

    override fun getBearing(
        myLocation: Coordinate,
        destination: Destination
    ): Bearing {
        return getBearing(myLocation, destination, getDeclination(myLocation))
    }

    private fun getBearing(
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
            Clock.System.now().toEpochMilliseconds(),
            userPrefs.useAutoDeclination,
            userPrefs.declinationOverride
        )
    }

    companion object {
        const val BEACON_DESTINATION_ID_KEY = "last_beacon_id_long"
        const val BROADCAST_DESTINATION_CHANGED = "navigation-broadcast-destination-changed"
        const val BROADCAST_NAVIGATION_STARTED = "navigation-broadcast-navigation-started"
        const val BROADCAST_NAVIGATION_STOPPED = "navigation-broadcast-navigation-stopped"
    }
}

private class SharedPathNavigationStore(private val prefs: Preferences) {
    private val json = Json { ignoreUnknownKeys = true }

    fun hasSavedRoute(): Boolean = prefs.getString(SESSION_KEY) != null

    fun save(session: PathNavigationSession) {
        prefs.putString(SESSION_KEY, json.encodeToString(PathNavigationSession.serializer(), session))
    }

    fun load(): PathNavigationSession? {
        val saved = prefs.getString(SESSION_KEY) ?: return null
        return tryOrDefault(null) { json.decodeFromString(PathNavigationSession.serializer(), saved) }
    }

    fun clear() {
        prefs.remove(SESSION_KEY)
    }

    private companion object {
        const val SESSION_KEY = "path_navigation_route"
    }
}

private class SharedPathNavigator(
    private val store: SharedPathNavigationStore,
    userPrefs: UserPreferences,
    private val locationSubsystem: ILocationSubsystem,
    private val paths: IPathService
) {
    private val prefs = userPrefs.paths
    private val destinationState = MutableStateFlow<Destination.Path?>(null)
    private val restoringState = MutableStateFlow(false)
    val isRestoring = restoringState.asStateFlow()

    private val lock = SynchronizedObject()
    private var session: PathNavigationSession? = null
    private var buildJob: Job? = null
    private var lastSavedProgress: Pair<Destination.Path, Coordinate>? = null

    val destination: Flow<Destination.Path?> = destinationState

    init {
        if (store.hasSavedRoute()) {
            synchronized(lock) {
                buildJob = startBuild()
            }
        }
        ToolBroadcasts.subscribe(SharedLocationSubsystem.BROADCAST_LOCATION_CHANGED) {
            destinationState.value?.let { destination ->
                tryOrLog {
                    val location = locationSubsystem.location
                    val guidance = destination.route.navigate(location, locationSubsystem.locationAccuracy)
                    saveProgress(destination, guidance, location)
                }
            }
        }
    }

    fun navigate(path: Path, follow: PathFollow) {
        val newSession = PathNavigationSession.of(path.id, follow, locationSubsystem.location)
        synchronized(lock) {
            cancel()
            store.save(newSession)
            buildJob = startBuild()
        }
    }

    private fun startBuild(): Job {
        restoringState.value = true
        return CoroutineScope(Dispatchers.Default).launch {
            try {
                val built = tryBuild()
                synchronized(lock) {
                    if (!isActive) return@synchronized
                    when (built) {
                        is BuildResult.Built -> {
                            session = built.session
                            destinationState.value = built.destination
                        }
                        BuildResult.Unfollowable -> store.clear()
                        is BuildResult.Failed -> Unit
                    }
                }
            } finally {
                synchronized(lock) {
                    if (buildJob === coroutineContext.job) {
                        restoringState.value = false
                    }
                }
            }
        }
    }

    private sealed interface BuildResult {
        data class Built(val session: PathNavigationSession, val destination: Destination.Path) : BuildResult
        data object Unfollowable : BuildResult
        data class Failed(val error: Exception) : BuildResult
    }

    private suspend fun tryBuild(): BuildResult {
        return try {
            build()?.let { BuildResult.Built(it.first, it.second) } ?: BuildResult.Unfollowable
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            BuildResult.Failed(e)
        }
    }

    private suspend fun build(): Pair<PathNavigationSession, Destination.Path>? {
        val loaded = store.load() ?: return null
        val path = paths.getPath(loaded.pathId) ?: return null
        val points = HikingService.correctElevations(
            paths.getWaypoints(loaded.pathId).sortedBy { it.id },
            prefs.elevationSmoothing
        )
        currentCoroutineContext().ensureActive()
        val route = PathRouteBuilder.buildRoute(points, loaded.start, loaded.follow) ?: return null
        val destination = Destination.Path(path, route)
        loaded.progress?.let { destination.route.restorePosition(it.distance, it.location) }
        destination.route.navigate(locationSubsystem.location, locationSubsystem.locationAccuracy)
        return loaded to destination
    }

    private fun saveProgress(destination: Destination.Path, guidance: PathGuidance, location: Coordinate) {
        val lastLocation = lastSavedProgress?.takeIf { it.first === destination }?.second
        if (lastLocation != null && lastLocation.distanceTo(location) < SAVE_PROGRESS_MIN_DISTANCE_METERS) {
            return
        }
        lastSavedProgress = destination to location
        synchronized(lock) {
            val current = session
            if (current != null && destinationState.value === destination) {
                val updated = current.copy(
                    progress = PathNavigationSessionProgress.of(guidance.distanceAlong, location)
                )
                session = updated
                store.save(updated)
            }
        }
    }

    fun restore(restored: PathNavigationSession) {
        synchronized(lock) {
            cancel()
            store.save(restored)
            buildJob = startBuild()
        }
    }

    fun currentSession(): PathNavigationSession? {
        return synchronized(lock) {
            session ?: tryOrDefault<PathNavigationSession?>(null) { store.load() }
        }
    }

    fun cancel() {
        synchronized(lock) {
            buildJob?.cancel()
            restoringState.value = false
            session = null
            destinationState.value = null
            store.clear()
        }
    }

    fun cancel(pathId: Long): Boolean {
        synchronized(lock) {
            if (currentSession()?.pathId != pathId) {
                return false
            }
            cancel()
            return true
        }
    }

    suspend fun awaitRestore() {
        val job = synchronized(lock) { buildJob }
        job?.join()
    }

    private companion object {
        const val SAVE_PROGRESS_MIN_DISTANCE_METERS = 25f
    }
}
