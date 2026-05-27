package com.kylecorry.trail_sense.main.navigation

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AppRouteSerializationTest {

    private val json = Json { prettyPrint = false }

    private inline fun <reified T : AppRoute> assertRoundTrip(route: T) {
        val encoded = json.encodeToString<AppRoute>(route)
        val decoded = json.decodeFromString<AppRoute>(encoded)
        assertEquals(route, decoded, "Round-trip failed for ${T::class.simpleName}: $route")
    }

    @Test
    fun `all data object routes round-trip`() {
        assertRoundTrip(AppRoute.Tools)
        assertRoundTrip(AppRoute.Astronomy)
        assertRoundTrip(AppRoute.Weather)
        assertRoundTrip(AppRoute.Map)
        assertRoundTrip(AppRoute.MapSettings)
        assertRoundTrip(AppRoute.NavigationSettings)
        assertRoundTrip(AppRoute.AstronomySettings)
        assertRoundTrip(AppRoute.WeatherSettings)
        assertRoundTrip(AppRoute.BeaconSettings)
        assertRoundTrip(AppRoute.Paths)
        assertRoundTrip(AppRoute.PathsSettings)
        assertRoundTrip(AppRoute.MapList)
        assertRoundTrip(AppRoute.PhotoMapSettings)
        assertRoundTrip(AppRoute.FieldGuide)
        assertRoundTrip(AppRoute.GuideList)
        assertRoundTrip(AppRoute.PackList)
        assertRoundTrip(AppRoute.Tides)
        assertRoundTrip(AppRoute.TideList)
        assertRoundTrip(AppRoute.TideSettings)
        assertRoundTrip(AppRoute.CloudResults)
        assertRoundTrip(AppRoute.AugmentedRealitySettings)
        assertRoundTrip(AppRoute.Clinometer)
        assertRoundTrip(AppRoute.ClinometerSettings)
        assertRoundTrip(AppRoute.Level)
        assertRoundTrip(AppRoute.BubbleLevelSettings)
        assertRoundTrip(AppRoute.Flashlight)
        assertRoundTrip(AppRoute.ScreenFlashlight)
        assertRoundTrip(AppRoute.FlashlightSettings)
        assertRoundTrip(AppRoute.SignalFinder)
        assertRoundTrip(AppRoute.Declination)
        assertRoundTrip(AppRoute.Convert)
        assertRoundTrip(AppRoute.CliffHeight)
        assertRoundTrip(AppRoute.Whistle)
        assertRoundTrip(AppRoute.SolarPanel)
        assertRoundTrip(AppRoute.WaterPurification)
        assertRoundTrip(AppRoute.WaterBoilTimerSettings)
        assertRoundTrip(AppRoute.Clock)
        assertRoundTrip(AppRoute.ClockSettings)
        assertRoundTrip(AppRoute.Lightning)
        assertRoundTrip(AppRoute.Ruler)
        assertRoundTrip(AppRoute.RulerSettings)
        assertRoundTrip(AppRoute.Battery)
        assertRoundTrip(AppRoute.Triangulate)
        assertRoundTrip(AppRoute.Ballistics)
        assertRoundTrip(AppRoute.Permits)
        assertRoundTrip(AppRoute.MetalDetector)
        assertRoundTrip(AppRoute.Notes)
        assertRoundTrip(AppRoute.WhiteNoise)
        assertRoundTrip(AppRoute.Pedometer)
        assertRoundTrip(AppRoute.PedometerSettings)
        assertRoundTrip(AppRoute.StrideLengthEstimation)
        assertRoundTrip(AppRoute.SensorDetails)
        assertRoundTrip(AppRoute.Diagnostics)
        assertRoundTrip(AppRoute.Light)
        assertRoundTrip(AppRoute.TurnBack)
        assertRoundTrip(AppRoute.TurnBackSettings)
        assertRoundTrip(AppRoute.TemperatureEstimation)
        assertRoundTrip(AppRoute.Climate)
        assertRoundTrip(AppRoute.MirrorCamera)
        assertRoundTrip(AppRoute.Magnifier)
        assertRoundTrip(AppRoute.ScanQr)
        assertRoundTrip(AppRoute.LocalMessaging)
        assertRoundTrip(AppRoute.LocalTalk)
        assertRoundTrip(AppRoute.Experimentation)
        assertRoundTrip(AppRoute.Settings)
        assertRoundTrip(AppRoute.SensorSettings)
        assertRoundTrip(AppRoute.ErrorSettings)
        assertRoundTrip(AppRoute.ExperimentalSettings)
        assertRoundTrip(AppRoute.PluginsSettings)
        assertRoundTrip(AppRoute.UnitSettings)
        assertRoundTrip(AppRoute.PowerSettings)
        assertRoundTrip(AppRoute.PrivacySettings)
        assertRoundTrip(AppRoute.ThemeSettings)
        assertRoundTrip(AppRoute.BackupSettings)
        assertRoundTrip(AppRoute.License)
        assertRoundTrip(AppRoute.CalibrateAltimeter)
        assertRoundTrip(AppRoute.CalibrateBarometer)
        assertRoundTrip(AppRoute.CalibrateCompass)
        assertRoundTrip(AppRoute.CalibrateGPS)
        assertRoundTrip(AppRoute.ThermometerSettings)
        assertRoundTrip(AppRoute.CellSignalSettings)
        assertRoundTrip(AppRoute.CameraSettings)
        assertRoundTrip(AppRoute.ToolsSettings)
    }

    @Test
    fun `all data class routes round-trip with defaults`() {
        assertRoundTrip(AppRoute.Navigation())
        assertRoundTrip(AppRoute.BeaconList())
        assertRoundTrip(AppRoute.PlaceBeacon())
        assertRoundTrip(AppRoute.BeaconDetails(beaconId = 42))
        assertRoundTrip(AppRoute.PathDetails())
        assertRoundTrip(AppRoute.OfflineMapView(offlineMapFileId = 7))
        assertRoundTrip(AppRoute.PhotoMaps())
        assertRoundTrip(AppRoute.FieldGuidePage())
        assertRoundTrip(AppRoute.CreateFieldGuideSighting(pageId = 1))
        assertRoundTrip(AppRoute.SightingList(pageId = 2))
        assertRoundTrip(AppRoute.CreateFieldGuidePage())
        assertRoundTrip(AppRoute.Guide(guideContents = 123))
        assertRoundTrip(AppRoute.SurvivalGuideList(searchQuery = "fire"))
        assertRoundTrip(AppRoute.SurvivalGuideReader(chapterResourceId = 5))
        assertRoundTrip(AppRoute.PackItemList())
        assertRoundTrip(AppRoute.CreateItem())
        assertRoundTrip(AppRoute.CreateTide())
        assertRoundTrip(AppRoute.Cloud())
        assertRoundTrip(AppRoute.AugmentedReality())
        assertRoundTrip(AppRoute.CreateNote())
        assertRoundTrip(AppRoute.ErrorHandler(toolId = 99))
        assertRoundTrip(AppRoute.PluginDetails(packageId = "com.example"))
    }

    @Test
    fun `data class routes round-trip with explicit args`() {
        assertRoundTrip(AppRoute.Navigation(destinationId = 100L))
        assertRoundTrip(AppRoute.BeaconList(initialLocationUri = "geo:0,0"))
        assertRoundTrip(AppRoute.PlaceBeacon(editBeaconId = 5, initialGroupId = 10, initialLocationUri = "geo:1,2"))
        assertRoundTrip(AppRoute.BeaconDetails(beaconId = 42))
        assertRoundTrip(AppRoute.PathDetails(destinationId = 33))
        assertRoundTrip(AppRoute.OfflineMapView(offlineMapFileId = 7))
        assertRoundTrip(AppRoute.PhotoMaps(mapId = 3, autoLockLocation = true))
        assertRoundTrip(AppRoute.FieldGuidePage(pageId = 11))
        assertRoundTrip(AppRoute.CreateFieldGuideSighting(pageId = 2, sightingId = 3))
        assertRoundTrip(AppRoute.SightingList(pageId = 9))
        assertRoundTrip(AppRoute.CreateFieldGuidePage(pageId = 4, classificationId = 5))
        assertRoundTrip(AppRoute.Guide(guideName = "Test Guide", guideContents = 456))
        assertRoundTrip(AppRoute.SurvivalGuideList(searchQuery = "shelter"))
        assertRoundTrip(AppRoute.SurvivalGuideReader(chapterResourceId = 8, headerIndex = 2))
        assertRoundTrip(AppRoute.PackItemList(packId = 15))
        assertRoundTrip(AppRoute.CreateItem(editItemId = 20, packId = 15))
        assertRoundTrip(AppRoute.CreateTide(editTideId = 6))
        assertRoundTrip(AppRoute.Cloud(openScanner = true))
        assertRoundTrip(AppRoute.AugmentedReality(mode = 2L, cameraEnabled = false))
        assertRoundTrip(AppRoute.CreateNote(editNoteId = 77))
        assertRoundTrip(AppRoute.ErrorHandler(toolId = 99, error = "Oops"))
        assertRoundTrip(AppRoute.PluginDetails(packageId = "com.example.plugin"))
    }

    @Test
    fun `encoded data object contains type discriminator`() {
        val encoded = json.encodeToString<AppRoute>(AppRoute.Tools)
        assertTrue(encoded.contains("Tools"), "Expected type discriminator 'Tools' in: $encoded")
    }

    @Test
    fun `encoded data class contains type discriminator and fields`() {
        val encoded = json.encodeToString<AppRoute>(AppRoute.BeaconDetails(beaconId = 42))
        assertTrue(encoded.contains("BeaconDetails"), "Expected type discriminator in: $encoded")
        assertTrue(encoded.contains("42"), "Expected beaconId value in: $encoded")
    }

    @Test
    fun `sealed deserialization picks correct subtype`() {
        val encoded = json.encodeToString<AppRoute>(AppRoute.Cloud(openScanner = true))
        val decoded = json.decodeFromString<AppRoute>(encoded)
        assertTrue(decoded is AppRoute.Cloud)
        assertEquals(true, (decoded as AppRoute.Cloud).openScanner)
    }
}
