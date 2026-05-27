package com.kylecorry.trail_sense.main.navigation

import androidx.annotation.IdRes
import androidx.navigation3.runtime.NavKey
import com.kylecorry.trail_sense.R
import kotlinx.serialization.Serializable

/**
 * Exhaustive, type-safe route map for the Navigation 3 migration.
 *
 * Every [AppRoute] maps 1:1 to a destination currently defined in
 * `app/src/main/res/navigation/nav_graph.xml`.
 *
 * ## Conventions
 * - **`data object`** — leaf screen that never needs arguments.
 * - **`data class`** — screen that requires one or more arguments
 *   (entity IDs, search queries, etc.).  Every parameter has a
 *   default value so callers can use named arguments.
 * - Every route is `@Serializable` so it doubles as a Navigation 3
 *   [NavKey] and can round-trip through `kotlinx.serialization`.
 *
 * ## Migration
 * Use [fromNavId] to convert an old `R.id.xxxFragment` int into
 * the corresponding [AppRoute].  Routes that require arguments
 * will be constructed with their defaults; callers should replace
 * bare `fromNavId` calls with direct constructor invocations once
 * the surrounding code is migrated.
 */
sealed interface AppRoute : NavKey {

    // ──────────────────────────────────────────────────────────────
    //  Top-level / Hub screens
    // ──────────────────────────────────────────────────────────────

    /** The main tools grid (start destination). */
    @Serializable
    data object Tools : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Navigation
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data class Navigation(
        val destinationId: Long = 0L
    ) : AppRoute

    @Serializable
    data object NavigationSettings : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Astronomy
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object Astronomy : AppRoute

    @Serializable
    data object AstronomySettings : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Weather
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object Weather : AppRoute

    @Serializable
    data object WeatherSettings : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Map
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object Map : AppRoute

    @Serializable
    data object MapSettings : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Beacons
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data class BeaconList(
        val initialLocationUri: String? = null
    ) : AppRoute

    @Serializable
    data class PlaceBeacon(
        val editBeaconId: Long = 0L,
        val initialGroupId: Long = 0L,
        val initialLocationUri: String? = null
    ) : AppRoute

    @Serializable
    data class BeaconDetails(
        val beaconId: Long
    ) : AppRoute

    @Serializable
    data object BeaconSettings : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Paths / Backtrack
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object Paths : AppRoute

    @Serializable
    data class PathDetails(
        val destinationId: Long = 0L
    ) : AppRoute

    @Serializable
    data object PathsSettings : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Offline Maps
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data class OfflineMapView(
        val offlineMapFileId: Long
    ) : AppRoute

    @Serializable
    data object MapList : AppRoute

    @Serializable
    data class PhotoMaps(
        val mapId: Long = 0L,
        val autoLockLocation: Boolean = false
    ) : AppRoute

    @Serializable
    data object PhotoMapSettings : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Field Guide
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object FieldGuide : AppRoute

    @Serializable
    data class FieldGuidePage(
        val pageId: Long = 0L
    ) : AppRoute

    @Serializable
    data class CreateFieldGuideSighting(
        val pageId: Long,
        val sightingId: Long = 0L
    ) : AppRoute

    @Serializable
    data class SightingList(
        val pageId: Long
    ) : AppRoute

    @Serializable
    data class CreateFieldGuidePage(
        val pageId: Long = 0L,
        val classificationId: Long = 0L
    ) : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Guides (User Guide)
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object GuideList : AppRoute

    @Serializable
    data class Guide(
        val guideName: String? = null,
        val guideContents: Int
    ) : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Survival Guide
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data class SurvivalGuideList(
        val searchQuery: String
    ) : AppRoute

    @Serializable
    data class SurvivalGuideReader(
        val chapterResourceId: Int,
        val headerIndex: Int = 0
    ) : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Packs / Inventory
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object PackList : AppRoute

    @Serializable
    data class PackItemList(
        val packId: Long = 0L
    ) : AppRoute

    @Serializable
    data class CreateItem(
        val editItemId: Long = 0L,
        val packId: Long = 0L
    ) : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Tides
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object Tides : AppRoute

    @Serializable
    data object TideList : AppRoute

    @Serializable
    data class CreateTide(
        val editTideId: Long = 0L
    ) : AppRoute

    @Serializable
    data object TideSettings : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Clouds
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data class Cloud(
        val openScanner: Boolean = false
    ) : AppRoute

    @Serializable
    data object CloudResults : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Augmented Reality
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data class AugmentedReality(
        val mode: Long = 1L,
        val cameraEnabled: Boolean = true
    ) : AppRoute

    @Serializable
    data object AugmentedRealitySettings : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Clinometer
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object Clinometer : AppRoute

    @Serializable
    data object ClinometerSettings : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Level
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object Level : AppRoute

    @Serializable
    data object BubbleLevelSettings : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Flashlight
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object Flashlight : AppRoute

    @Serializable
    data object ScreenFlashlight : AppRoute

    @Serializable
    data object FlashlightSettings : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Signal Finder
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object SignalFinder : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Declination
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object Declination : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Convert
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object Convert : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Cliff Height
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object CliffHeight : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Whistle
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object Whistle : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Solar Panel
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object SolarPanel : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Water Purification
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object WaterPurification : AppRoute

    @Serializable
    data object WaterBoilTimerSettings : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Clock
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object Clock : AppRoute

    @Serializable
    data object ClockSettings : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Lightning
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object Lightning : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Ruler
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object Ruler : AppRoute

    @Serializable
    data object RulerSettings : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Battery
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object Battery : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Triangulate
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object Triangulate : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Ballistics
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object Ballistics : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Permits
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object Permits : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Metal Detector
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object MetalDetector : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Notes
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object Notes : AppRoute

    @Serializable
    data class CreateNote(
        val editNoteId: Long = 0L
    ) : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  White Noise
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object WhiteNoise : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Pedometer / Odometer
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object Pedometer : AppRoute

    @Serializable
    data object PedometerSettings : AppRoute

    @Serializable
    data object StrideLengthEstimation : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Sensors / Diagnostics
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object SensorDetails : AppRoute

    @Serializable
    data object Diagnostics : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Light Meter
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object Light : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Turn Back
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object TurnBack : AppRoute

    @Serializable
    data object TurnBackSettings : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Temperature Estimation
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object TemperatureEstimation : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Climate
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object Climate : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Mirror
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object MirrorCamera : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Magnifier
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object Magnifier : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  QR Scanner
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object ScanQr : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Comms
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object LocalMessaging : AppRoute

    @Serializable
    data object LocalTalk : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Experimentation
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object Experimentation : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Settings (hierarchy)
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object Settings : AppRoute

    @Serializable
    data object SensorSettings : AppRoute

    @Serializable
    data object ErrorSettings : AppRoute

    @Serializable
    data object ExperimentalSettings : AppRoute

    @Serializable
    data object PluginsSettings : AppRoute

    @Serializable
    data class PluginDetails(
        val packageId: String
    ) : AppRoute

    @Serializable
    data object UnitSettings : AppRoute

    @Serializable
    data object PowerSettings : AppRoute

    @Serializable
    data object PrivacySettings : AppRoute

    @Serializable
    data object ThemeSettings : AppRoute

    @Serializable
    data object BackupSettings : AppRoute

    @Serializable
    data object License : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Calibration sub-screens
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object CalibrateAltimeter : AppRoute

    @Serializable
    data object CalibrateBarometer : AppRoute

    @Serializable
    data object CalibrateCompass : AppRoute

    @Serializable
    data object CalibrateGPS : AppRoute

    @Serializable
    data object ThermometerSettings : AppRoute

    @Serializable
    data object CellSignalSettings : AppRoute

    @Serializable
    data object CameraSettings : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Tools Settings
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data object ToolsSettings : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Error Handler
    // ──────────────────────────────────────────────────────────────

    @Serializable
    data class ErrorHandler(
        val toolId: Long,
        val error: String = ""
    ) : AppRoute

    // ──────────────────────────────────────────────────────────────
    //  Migration helpers
    // ──────────────────────────────────────────────────────────────

    companion object {

        /**
         * Map an old `R.id.xxxFragment` / `R.id.action_xxx` integer to
         * the corresponding [AppRoute].
         *
         * For `data class` routes that require arguments the default
         * constructor is used.  Migrated call-sites should call the
         * route constructor directly once they are ready to pass
         * proper arguments.
         *
         * @return the matching [AppRoute], or `null` if [navId] is
         *         not a recognised destination.
         */
        fun fromNavId(@IdRes navId: Int): AppRoute? = when (navId) {
            // ── Top-level ──────────────────────────────────────────
            R.id.action_experimental_tools -> Tools

            // ── Navigation ─────────────────────────────────────────
            R.id.action_navigation -> Navigation()
            R.id.navigationSettingsFragment -> NavigationSettings

            // ── Astronomy ──────────────────────────────────────────
            R.id.action_astronomy -> Astronomy
            R.id.astronomySettingsFragment -> AstronomySettings

            // ── Weather ────────────────────────────────────────────
            R.id.action_weather -> Weather
            R.id.weatherSettingsFragment -> WeatherSettings

            // ── Map ────────────────────────────────────────────────
            R.id.mapFragment -> Map
            R.id.mapSettingsFragment -> MapSettings

            // ── Beacons ────────────────────────────────────────────
            R.id.beacon_list -> BeaconList()
            R.id.placeBeaconFragment -> PlaceBeacon()
            R.id.beaconDetailsFragment -> BeaconDetails(beaconId = 0L)
            R.id.beaconsSettingsFragment -> BeaconSettings

            // ── Paths / Backtrack ──────────────────────────────────
            R.id.fragmentBacktrack -> Paths
            R.id.pathDetailsFragment -> PathDetails()
            R.id.pathsSettingsFragment -> PathsSettings

            // ── Offline Maps ───────────────────────────────────────
            R.id.offlineMapViewFragment -> OfflineMapView(offlineMapFileId = 0L)
            R.id.mapListFragment -> MapList
            R.id.photoMapsFragment -> PhotoMaps()
            R.id.photoMapSettingsFragment -> PhotoMapSettings

            // ── Field Guide ────────────────────────────────────────
            R.id.fieldGuideFragment -> FieldGuide
            R.id.fieldGuidePageFragment -> FieldGuidePage()
            R.id.createFieldGuideSightingFragment -> CreateFieldGuideSighting(pageId = 0L)
            R.id.sightingListFragment -> SightingList(pageId = 0L)
            R.id.createFieldGuidePageFragment -> CreateFieldGuidePage()

            // ── Guides (User Guide) ────────────────────────────────
            R.id.guideListFragment -> GuideList
            R.id.guideFragment -> Guide(guideContents = 0)

            // ── Survival Guide ─────────────────────────────────────
            R.id.fragmentToolSurvivalGuideList -> SurvivalGuideList(searchQuery = "")
            R.id.fragmentToolSurvivalGuideReader -> SurvivalGuideReader(chapterResourceId = 0)

            // ── Packs / Inventory ──────────────────────────────────
            R.id.packListFragment -> PackList
            R.id.packItemListFragment -> PackItemList()
            R.id.createItemFragment -> CreateItem()

            // ── Tides ──────────────────────────────────────────────
            R.id.tidesFragment -> Tides
            R.id.tideListFragment -> TideList
            R.id.createTideFragment -> CreateTide()
            R.id.tideSettingsFragment -> TideSettings

            // ── Clouds ─────────────────────────────────────────────
            R.id.cloudFragment -> Cloud()
            R.id.cloudResultsFragment -> CloudResults

            // ── Augmented Reality ──────────────────────────────────
            R.id.augmentedRealityFragment -> AugmentedReality()
            R.id.augmentedRealitySettingsFragment -> AugmentedRealitySettings

            // ── Clinometer ─────────────────────────────────────────
            R.id.clinometerFragment -> Clinometer
            R.id.clinometerSettingsFragment -> ClinometerSettings

            // ── Level ──────────────────────────────────────────────
            R.id.levelFragment -> Level
            R.id.bubbleLevelSettingsFragment -> BubbleLevelSettings

            // ── Flashlight ─────────────────────────────────────────
            R.id.fragmentToolFlashlight -> Flashlight
            R.id.fragmentToolScreenFlashlight -> ScreenFlashlight
            R.id.flashlightSettingsFragment -> FlashlightSettings

            // ── Signal Finder ──────────────────────────────────────
            R.id.signalFinderFragment -> SignalFinder

            // ── Declination ────────────────────────────────────────
            R.id.toolDeclinationFragment -> Declination

            // ── Convert ────────────────────────────────────────────
            R.id.toolConvertFragment -> Convert

            // ── Cliff Height ───────────────────────────────────────
            R.id.toolCliffHeightFragment -> CliffHeight

            // ── Whistle ────────────────────────────────────────────
            R.id.toolWhistleFragment -> Whistle

            // ── Solar Panel ────────────────────────────────────────
            R.id.fragmentToolSolarPanel -> SolarPanel

            // ── Water Purification ─────────────────────────────────
            R.id.waterPurificationFragment -> WaterPurification
            R.id.fragmentWaterBoilTimerSettings -> WaterBoilTimerSettings

            // ── Clock ──────────────────────────────────────────────
            R.id.toolClockFragment -> Clock
            R.id.clockSettingsFragment -> ClockSettings

            // ── Lightning ──────────────────────────────────────────
            R.id.fragmentToolLightning -> Lightning

            // ── Ruler ──────────────────────────────────────────────
            R.id.rulerFragment -> Ruler
            R.id.toolRulerSettingsFragment -> RulerSettings

            // ── Battery ────────────────────────────────────────────
            R.id.fragmentToolBattery -> Battery

            // ── Triangulate ────────────────────────────────────────
            R.id.fragmentToolTriangulate -> Triangulate

            // ── Ballistics ─────────────────────────────────────────
            R.id.fragmentToolBallistics -> Ballistics

            // ── Permits ────────────────────────────────────────────
            R.id.fragmentToolPermits -> Permits

            // ── Metal Detector ─────────────────────────────────────
            R.id.fragmentToolMetalDetector -> MetalDetector

            // ── Notes ──────────────────────────────────────────────
            R.id.fragmentToolNotes -> Notes
            R.id.fragmentToolNotesCreate -> CreateNote()

            // ── White Noise ────────────────────────────────────────
            R.id.fragmentToolWhiteNoise -> WhiteNoise

            // ── Pedometer / Odometer ───────────────────────────────
            R.id.fragmentToolPedometer -> Pedometer
            R.id.calibrateOdometerFragment -> PedometerSettings
            R.id.fragmentStrideLengthEstimation -> StrideLengthEstimation

            // ── Sensors / Diagnostics ──────────────────────────────
            R.id.sensorDetailsFragment -> SensorDetails
            R.id.diagnosticsFragment -> Diagnostics

            // ── Light Meter ────────────────────────────────────────
            R.id.toolLightFragment -> Light

            // ── Turn Back ──────────────────────────────────────────
            R.id.turnBackFragment -> TurnBack
            R.id.fragmentTurnBackSettings -> TurnBackSettings

            // ── Temperature Estimation ─────────────────────────────
            R.id.temperatureEstimationFragment -> TemperatureEstimation

            // ── Climate ────────────────────────────────────────────
            R.id.climateFragment -> Climate

            // ── Mirror ─────────────────────────────────────────────
            R.id.mirrorCameraFragment -> MirrorCamera

            // ── Magnifier ──────────────────────────────────────────
            R.id.magnifierFragment -> Magnifier

            // ── QR Scanner ─────────────────────────────────────────
            R.id.scanQrFragment -> ScanQr

            // ── Comms ──────────────────────────────────────────────
            R.id.fragmentLocalMessaging -> LocalMessaging
            R.id.fragmentLocalTalk -> LocalTalk

            // ── Experimentation ────────────────────────────────────
            R.id.experimentationFragment -> Experimentation

            // ── Settings (hierarchy) ───────────────────────────────
            R.id.action_settings -> Settings
            R.id.sensorSettingsFragment -> SensorSettings
            R.id.errorSettingsFragment -> ErrorSettings
            R.id.experimentalSettingsFragment -> ExperimentalSettings
            R.id.pluginsSettingsFragment -> PluginsSettings
            R.id.pluginDetailsFragment -> PluginDetails(packageId = "")
            R.id.unitSettingsFragment -> UnitSettings
            R.id.powerSettingsFragment -> PowerSettings
            R.id.privacySettingsFragment -> PrivacySettings
            R.id.themeSettingsFragment -> ThemeSettings
            R.id.backupSettingsFragment -> BackupSettings
            R.id.licenseFragment -> License

            // ── Calibration ────────────────────────────────────────
            R.id.calibrateAltimeterFragment -> CalibrateAltimeter
            R.id.calibrateBarometerFragment -> CalibrateBarometer
            R.id.calibrateCompassFragment -> CalibrateCompass
            R.id.calibrateGPSFragment -> CalibrateGPS
            R.id.thermometerSettingsFragment -> ThermometerSettings
            R.id.cellSignalSettingsFragment -> CellSignalSettings
            R.id.cameraSettingsFragment -> CameraSettings

            // ── Tools Settings ─────────────────────────────────────
            R.id.toolsSettingsFragment -> ToolsSettings

            // ── Error Handler ──────────────────────────────────────
            R.id.fragmentToolErrorHandler -> ErrorHandler(toolId = 0L)

            else -> null
        }
    }
}

/*
 * ============================================================
 * R.id  →  AppRoute  mapping reference
 * ============================================================
 *
 * nav_graph.xml fragment id              → AppRoute
 * ───────────────────────────────────────────────────────────────
 * action_experimental_tools              → Tools
 * action_navigation                      → Navigation(destinationId)
 * action_astronomy                       → Astronomy
 * action_weather                         → Weather
 * mapFragment                            → Map
 * mapSettingsFragment                    → MapSettings
 * offlineMapViewFragment                 → OfflineMapView(offlineMapFileId)
 * action_settings                        → Settings
 * experimentationFragment               → Experimentation
 * fieldGuideFragment                     → FieldGuide
 * fieldGuidePageFragment                 → FieldGuidePage(pageId)
 * createFieldGuideSightingFragment       → CreateFieldGuideSighting(pageId, sightingId)
 * sightingListFragment                   → SightingList(pageId)
 * createFieldGuidePageFragment           → CreateFieldGuidePage(pageId, classificationId)
 * signalFinderFragment                   → SignalFinder
 * toolDeclinationFragment                → Declination
 * beacon_list                            → BeaconList(initialLocationUri)
 * placeBeaconFragment                    → PlaceBeacon(editBeaconId, initialGroupId, initialLocationUri)
 * beaconDetailsFragment                  → BeaconDetails(beaconId)
 * clinometerFragment                     → Clinometer
 * calibrateAltimeterFragment             → CalibrateAltimeter
 * calibrateBarometerFragment             → CalibrateBarometer
 * calibrateCompassFragment               → CalibrateCompass
 * calibrateGPSFragment                   → CalibrateGPS
 * licenseFragment                        → License
 * weatherSettingsFragment                → WeatherSettings
 * navigationSettingsFragment             → NavigationSettings
 * pathsSettingsFragment                  → PathsSettings
 * beaconsSettingsFragment                → BeaconSettings
 * pluginsSettingsFragment                → PluginsSettings
 * pluginDetailsFragment                  → PluginDetails(packageId)
 * astronomySettingsFragment              → AstronomySettings
 * guideListFragment                      → GuideList
 * guideFragment                          → Guide(guideName, guideContents)
 * levelFragment                          → Level
 * packItemListFragment                   → PackItemList(packId)
 * createItemFragment                     → CreateItem(editItemId, packId)
 * toolConvertFragment                    → Convert
 * toolCliffHeightFragment                → CliffHeight
 * toolWhistleFragment                    → Whistle
 * fragmentToolSolarPanel                 → SolarPanel
 * waterPurificationFragment              → WaterPurification
 * fragmentToolFlashlight                 → Flashlight
 * toolClockFragment                      → Clock
 * fragmentToolLightning                  → Lightning
 * rulerFragment                          → Ruler
 * fragmentToolBattery                    → Battery
 * fragmentToolTriangulate                → Triangulate
 * fragmentToolBallistics                 → Ballistics
 * fragmentToolPermits                    → Permits
 * fragmentToolMetalDetector              → MetalDetector
 * fragmentToolSurvivalGuideList          → SurvivalGuideList(searchQuery)
 * fragmentToolSurvivalGuideReader        → SurvivalGuideReader(chapterResourceId, headerIndex)
 * fragmentToolNotes                      → Notes
 * fragmentToolNotesCreate                → CreateNote(editNoteId)
 * fragmentToolWhiteNoise                 → WhiteNoise
 * fragmentBacktrack                      → Paths
 * cloudFragment                          → Cloud(openScanner)
 * cloudResultsFragment                   → CloudResults
 * sensorDetailsFragment                  → SensorDetails
 * toolLightFragment                      → Light
 * tidesFragment                          → Tides
 * fragmentToolScreenFlashlight           → ScreenFlashlight
 * fragmentToolPedometer                  → Pedometer
 * fragmentTurnBackSettings               → TurnBackSettings
 * fragmentWaterBoilTimerSettings         → WaterBoilTimerSettings
 * calibrateOdometerFragment              → PedometerSettings
 * createTideFragment                     → CreateTide(editTideId)
 * tideListFragment                       → TideList
 * photoMapsFragment                      → PhotoMaps(mapId, autoLockLocation)
 * mapListFragment                        → MapList
 * flashlightSettingsFragment             → FlashlightSettings
 * thermometerSettingsFragment            → ThermometerSettings
 * cellSignalSettingsFragment             → CellSignalSettings
 * bubbleLevelSettingsFragment            → BubbleLevelSettings
 * clockSettingsFragment                  → ClockSettings
 * privacySettingsFragment                → PrivacySettings
 * themeSettingsFragment                  → ThemeSettings
 * backupSettingsFragment                 → BackupSettings
 * powerSettingsFragment                  → PowerSettings
 * unitSettingsFragment                   → UnitSettings
 * experimentalSettingsFragment           → ExperimentalSettings
 * toolRulerSettingsFragment              → RulerSettings
 * sensorSettingsFragment                 → SensorSettings
 * photoMapSettingsFragment               → PhotoMapSettings
 * temperatureEstimationFragment          → TemperatureEstimation
 * packListFragment                       → PackList
 * diagnosticsFragment                    → Diagnostics
 * pathDetailsFragment                    → PathDetails(destinationId)
 * tideSettingsFragment                   → TideSettings
 * clinometerSettingsFragment             → ClinometerSettings
 * augmentedRealityFragment               → AugmentedReality(mode, cameraEnabled)
 * augmentedRealitySettingsFragment       → AugmentedRealitySettings
 * cameraSettingsFragment                 → CameraSettings
 * errorSettingsFragment                  → ErrorSettings
 * scanQrFragment                         → ScanQr
 * fragmentStrideLengthEstimation         → StrideLengthEstimation
 * climateFragment                        → Climate
 * mirrorCameraFragment                   → MirrorCamera
 * magnifierFragment                      → Magnifier
 * turnBackFragment                       → TurnBack
 * fragmentLocalMessaging                 → LocalMessaging
 * fragmentLocalTalk                      → LocalTalk
 * toolsSettingsFragment                  → ToolsSettings
 * fragmentToolErrorHandler               → ErrorHandler(toolId, error)
 *
 * ============================================================
 */
