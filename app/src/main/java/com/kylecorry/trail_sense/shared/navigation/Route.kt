package com.kylecorry.trail_sense.shared.navigation

import com.kylecorry.trail_sense.shared.navigation.Route

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Every screen in the app as a Navigation 3 key. A screen's arguments are its fields, so opening one is
 * `navigate(Route.PlaceBeacon(initialLocation = here))` and a missing or mistyped argument is a compile error.
 * Routes are serializable: the back stack survives process death, and notifications and widgets carry a route.
 *
 * Routes hold only plain Kotlin values (no Android types), so this file could move to common code unchanged: a place
 * is a [RouteLocation], a content URI is its string.
 */
@Serializable
sealed interface Route : NavKey {

    // Tabs and tools
    @Serializable data class Navigation(val destinationBeaconId: Long = 0L, val navigationStarted: Boolean = false) : Route
    @Serializable data object Astronomy : Route
    @Serializable data object Tools : Route
    @Serializable data object Weather : Route
    @Serializable data object Map : Route
    @Serializable data object Settings : Route
    @Serializable data object Experimentation : Route
    @Serializable data object SignalFinder : Route
    @Serializable data object Declination : Route
    @Serializable data object Clinometer : Route
    @Serializable data object Level : Route
    @Serializable data object Convert : Route
    @Serializable data object CliffHeight : Route
    @Serializable data object Whistle : Route
    @Serializable data object SolarPanel : Route
    @Serializable data object WaterPurification : Route
    @Serializable data object Flashlight : Route
    @Serializable data object ScreenFlashlight : Route
    @Serializable data object Clock : Route
    @Serializable data object Lightning : Route
    @Serializable data object Ruler : Route
    @Serializable data object Battery : Route
    @Serializable data object Triangulate : Route
    @Serializable data object Ballistics : Route
    @Serializable data object MetalDetector : Route
    @Serializable data object WhiteNoise : Route
    @Serializable data object SensorDetails : Route
    @Serializable data object LightMeter : Route
    @Serializable data object Pedometer : Route
    @Serializable data object StrideLengthEstimation : Route
    @Serializable data object TemperatureEstimation : Route
    @Serializable data object Diagnostics : Route
    @Serializable data object ScanQr : Route
    @Serializable data object Climate : Route
    @Serializable data object MirrorCamera : Route
    @Serializable data object Magnifier : Route
    @Serializable data object TurnBack : Route
    @Serializable data object LocalMessaging : Route
    @Serializable data object LocalTalk : Route
    @Serializable data class ErrorHandler(val toolId: Long, val error: String = "") : Route
    /** [time] is an ISO zoned date-time to show the sky at, instead of now. */
    @Serializable data class AugmentedReality(
        val modeId: Long = 1L,
        val cameraEnabled: Boolean = true,
        val time: String? = null
    ) : Route
    @Serializable data class ViewQr(val title: String = "", val text: String = "") : Route

    // Places
    @Serializable data class BeaconList(
        val initialLocation: RouteLocation? = null
    ) : Route
    @Serializable data class PlaceBeacon(
        val editBeaconId: Long = 0L,
        val initialGroupId: Long = 0L,
        val initialLocation: RouteLocation? = null
    ) : Route
    @Serializable data class BeaconDetails(val beaconId: Long) : Route

    // Paths
    @Serializable data object Paths : Route
    @Serializable data class PathOverview(val pathId: Long) : Route

    // Maps
    @Serializable data object MapSettings : Route
    @Serializable data class VectorMapView(val offlineMapFileId: Long) : Route
    /** [importUri] is a map file shared to the app, to import as soon as the list opens. */
    @Serializable data class OfflineMapList(val importUri: String? = null) : Route
    @Serializable data class PhotoMaps(val mapId: Long, val autoLockLocation: Boolean = false) : Route
    @Serializable data object PhotoMapSettings : Route

    // Field guide
    @Serializable data object FieldGuide : Route
    @Serializable data class FieldGuidePage(val pageId: Long = 0L) : Route
    @Serializable data object FieldGuideSettings : Route
    @Serializable data class CreateFieldGuideSighting(val pageId: Long = 0L, val sightingId: Long = 0L) : Route
    @Serializable data class SightingList(val pageId: Long) : Route
    @Serializable data class CreateFieldGuidePage(val pageId: Long = 0L, val classificationId: Long = 0L) : Route

    // Guides
    @Serializable data object GuideList : Route
    /** [contents] and [chapterResourceId] are the guide's text resource. */
    @Serializable data class Guide(val name: String, val contents: Int) : Route
    @Serializable data class SurvivalGuideList(val searchQuery: String = "") : Route
    @Serializable data class SurvivalGuideReader(val chapterResourceId: Int, val headerIndex: Int = 0) : Route

    // Packing lists, notes, clouds, tides
    @Serializable data object PackList : Route
    @Serializable data class PackItemList(val packId: Long) : Route
    @Serializable data class CreatePackItem(val packId: Long, val editItemId: Long = 0L) : Route
    @Serializable data object Notes : Route
    @Serializable data class NoteEditor(val editNoteId: Long = 0L) : Route
    @Serializable data class Clouds(val openScanner: Boolean = false) : Route
    @Serializable data class CloudResults(val imageUri: String? = null) : Route
    @Serializable data object Tides : Route
    @Serializable data class CreateTide(val editTideId: Long = 0L) : Route
    @Serializable data object TideList : Route

    // Calibration and settings
    @Serializable data object CalibrateAltimeter : Route
    @Serializable data object CalibrateBarometer : Route
    @Serializable data object CalibrateCompass : Route
    @Serializable data object CalibrateGps : Route
    @Serializable data object About : Route
    @Serializable data object Licenses : Route
    @Serializable data object WeatherSettings : Route
    @Serializable data object NavigationSettings : Route
    @Serializable data object PathsSettings : Route
    @Serializable data object BeaconSettings : Route
    @Serializable data object PluginsSettings : Route
    @Serializable data class PluginDetails(val packageId: String) : Route
    @Serializable data object AstronomySettings : Route
    @Serializable data object TurnBackSettings : Route
    @Serializable data object WaterBoilSettings : Route
    @Serializable data object PedometerSettings : Route
    @Serializable data object FlashlightSettings : Route
    @Serializable data object ThermometerSettings : Route
    @Serializable data object CellSignalSettings : Route
    @Serializable data object BubbleLevelSettings : Route
    @Serializable data object ClockSettings : Route
    @Serializable data object PrivacySettings : Route
    @Serializable data object BackupSettings : Route
    @Serializable data object PowerSettings : Route
    @Serializable data object UnitSettings : Route
    @Serializable data object ExperimentalSettings : Route
    @Serializable data object RulerSettings : Route
    @Serializable data object SensorSettings : Route
    @Serializable data object TideSettings : Route
    @Serializable data object ClinometerSettings : Route
    @Serializable data object AugmentedRealitySettings : Route
    @Serializable data object CameraSettings : Route
    @Serializable data object ErrorSettings : Route
    @Serializable data object ToolsSettings : Route
}

/** A place handed to a screen (a spot to save, a tower to mark): where it is and what the sharer called it. */
@Serializable
data class RouteLocation(
    val latitude: Double,
    val longitude: Double,
    val altitude: Float? = null,
    val parameters: kotlin.collections.Map<String, String> = emptyMap()
)
