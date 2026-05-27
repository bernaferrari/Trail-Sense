package com.kylecorry.trail_sense.main.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.diagnostics.ui.DiagnosticsScreen

@Composable
fun TrailSenseNav3Host(
    backStack: MutableList<NavKey>,
    onNavigateToTool: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        modifier = modifier,
        entryProvider = entryProvider {
            // ── Real screens (fully migrated) ──────────────────────

            entry<AppRoute.Tools> {
                ToolsScreen(
                    onToolClick = { tool ->
                        onNavigateToTool(tool.navAction)
                    }
                )
            }

            // ── Placeholder screens (not yet migrated) ─────────────
            // All routes below render a "Not yet migrated" placeholder.
            // As each screen is migrated to Compose, replace its placeholder
            // entry with a real composable call.

            entry<AppRoute.Navigation> { Placeholder(it) }
            entry<AppRoute.NavigationSettings> { Placeholder(it) }
            entry<AppRoute.Astronomy> { Placeholder(it) }
            entry<AppRoute.AstronomySettings> { Placeholder(it) }
            entry<AppRoute.Weather> { Placeholder(it) }
            entry<AppRoute.WeatherSettings> { Placeholder(it) }
            entry<AppRoute.Map> { Placeholder(it) }
            entry<AppRoute.MapSettings> { Placeholder(it) }
            entry<AppRoute.BeaconList> { Placeholder(it) }
            entry<AppRoute.PlaceBeacon> { Placeholder(it) }
            entry<AppRoute.BeaconDetails> { Placeholder(it) }
            entry<AppRoute.BeaconSettings> { Placeholder(it) }
            entry<AppRoute.Paths> { Placeholder(it) }
            entry<AppRoute.PathDetails> { Placeholder(it) }
            entry<AppRoute.PathsSettings> { Placeholder(it) }
            entry<AppRoute.OfflineMapView> { Placeholder(it) }
            entry<AppRoute.MapList> { Placeholder(it) }
            entry<AppRoute.PhotoMaps> { Placeholder(it) }
            entry<AppRoute.PhotoMapSettings> { Placeholder(it) }
            entry<AppRoute.FieldGuide> { Placeholder(it) }
            entry<AppRoute.FieldGuidePage> { Placeholder(it) }
            entry<AppRoute.CreateFieldGuideSighting> { Placeholder(it) }
            entry<AppRoute.SightingList> { Placeholder(it) }
            entry<AppRoute.CreateFieldGuidePage> { Placeholder(it) }
            entry<AppRoute.GuideList> { Placeholder(it) }
            entry<AppRoute.Guide> { Placeholder(it) }
            entry<AppRoute.SurvivalGuideList> { Placeholder(it) }
            entry<AppRoute.SurvivalGuideReader> { Placeholder(it) }
            entry<AppRoute.PackList> { Placeholder(it) }
            entry<AppRoute.PackItemList> { Placeholder(it) }
            entry<AppRoute.CreateItem> { Placeholder(it) }
            entry<AppRoute.Tides> { Placeholder(it) }
            entry<AppRoute.TideList> { Placeholder(it) }
            entry<AppRoute.CreateTide> { Placeholder(it) }
            entry<AppRoute.TideSettings> { Placeholder(it) }
            entry<AppRoute.Cloud> { Placeholder(it) }
            entry<AppRoute.CloudResults> { Placeholder(it) }
            entry<AppRoute.AugmentedReality> { Placeholder(it) }
            entry<AppRoute.AugmentedRealitySettings> { Placeholder(it) }
            entry<AppRoute.Clinometer> { Placeholder(it) }
            entry<AppRoute.ClinometerSettings> { Placeholder(it) }
            entry<AppRoute.Level> { Placeholder(it) }
            entry<AppRoute.BubbleLevelSettings> { Placeholder(it) }
            entry<AppRoute.Flashlight> { Placeholder(it) }
            entry<AppRoute.ScreenFlashlight> { Placeholder(it) }
            entry<AppRoute.FlashlightSettings> { Placeholder(it) }
            entry<AppRoute.SignalFinder> { Placeholder(it) }
            entry<AppRoute.Declination> { Placeholder(it) }
            entry<AppRoute.Convert> { Placeholder(it) }
            entry<AppRoute.CliffHeight> { Placeholder(it) }
            entry<AppRoute.Whistle> {
                com.kylecorry.trail_sense.tools.whistle.ui.WhistleScreen()
            }
            entry<AppRoute.SolarPanel> { Placeholder(it) }
            entry<AppRoute.WaterPurification> { Placeholder(it) }
            entry<AppRoute.WaterBoilTimerSettings> { Placeholder(it) }
            entry<AppRoute.Clock> { Placeholder(it) }
            entry<AppRoute.ClockSettings> { Placeholder(it) }
            entry<AppRoute.Lightning> { Placeholder(it) }
            entry<AppRoute.Ruler> { Placeholder(it) }
            entry<AppRoute.RulerSettings> { Placeholder(it) }
            entry<AppRoute.Battery> { Placeholder(it) }
            entry<AppRoute.Triangulate> { Placeholder(it) }
            entry<AppRoute.Ballistics> { Placeholder(it) }
            entry<AppRoute.Permits> { Placeholder(it) }
            entry<AppRoute.MetalDetector> { Placeholder(it) }
            entry<AppRoute.Notes> { Placeholder(it) }
            entry<AppRoute.CreateNote> { Placeholder(it) }
            entry<AppRoute.WhiteNoise> { Placeholder(it) }
            entry<AppRoute.Pedometer> { Placeholder(it) }
            entry<AppRoute.PedometerSettings> { Placeholder(it) }
            entry<AppRoute.StrideLengthEstimation> { Placeholder(it) }
            entry<AppRoute.SensorDetails> { Placeholder(it) }
            entry<AppRoute.Diagnostics> { DiagnosticsScreen() }
            entry<AppRoute.Light> { Placeholder(it) }
            entry<AppRoute.TurnBack> { Placeholder(it) }
            entry<AppRoute.TurnBackSettings> { Placeholder(it) }
            entry<AppRoute.TemperatureEstimation> { Placeholder(it) }
            entry<AppRoute.Climate> { Placeholder(it) }
            entry<AppRoute.MirrorCamera> { Placeholder(it) }
            entry<AppRoute.Magnifier> { Placeholder(it) }
            entry<AppRoute.ScanQr> { Placeholder(it) }
            entry<AppRoute.LocalMessaging> { Placeholder(it) }
            entry<AppRoute.LocalTalk> { Placeholder(it) }
            entry<AppRoute.Experimentation> { Placeholder(it) }
            entry<AppRoute.Settings> { Placeholder(it) }
            entry<AppRoute.SensorSettings> { Placeholder(it) }
            entry<AppRoute.ErrorSettings> { Placeholder(it) }
            entry<AppRoute.ExperimentalSettings> { Placeholder(it) }
            entry<AppRoute.PluginsSettings> { Placeholder(it) }
            entry<AppRoute.PluginDetails> { Placeholder(it) }
            entry<AppRoute.UnitSettings> { Placeholder(it) }
            entry<AppRoute.PowerSettings> { Placeholder(it) }
            entry<AppRoute.PrivacySettings> { Placeholder(it) }
            entry<AppRoute.ThemeSettings> { Placeholder(it) }
            entry<AppRoute.BackupSettings> { Placeholder(it) }
            entry<AppRoute.License> { Placeholder(it) }
            entry<AppRoute.CalibrateAltimeter> { Placeholder(it) }
            entry<AppRoute.CalibrateBarometer> { Placeholder(it) }
            entry<AppRoute.CalibrateCompass> { Placeholder(it) }
            entry<AppRoute.CalibrateGPS> { Placeholder(it) }
            entry<AppRoute.ThermometerSettings> { Placeholder(it) }
            entry<AppRoute.CellSignalSettings> { Placeholder(it) }
            entry<AppRoute.CameraSettings> { Placeholder(it) }
            entry<AppRoute.ToolsSettings> { Placeholder(it) }
            entry<AppRoute.ErrorHandler> { Placeholder(it) }
        }
    )
}

@Composable
private fun Placeholder(route: AppRoute) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.nav3_not_migrated),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
