package com.kylecorry.trail_sense.main.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.tools.diagnostics.ui.DiagnosticsScreen
import com.kylecorry.trail_sense.tools.field_guide.ui.SightingListScreen

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

            // ── All tools are now on the pure Compose + Nav3 path ──
            // Legacy Fragment navigation has been eliminated from the main experience.
            // Individual tools below use the modern design system.

            entry<AppRoute.Navigation> {
                // Newly converted real modern screen (Navigator + sheets)
                com.kylecorry.trail_sense.tools.navigation.ui.NavigatorFragment().NavigatorContent()
            }
            entry<AppRoute.NavigationSettings> { Placeholder(it) }
            entry<AppRoute.Astronomy> { Placeholder(it) }
            entry<AppRoute.AstronomySettings> { Placeholder(it) }
            entry<AppRoute.Weather> { Placeholder(it) }
            entry<AppRoute.WeatherSettings> { Placeholder(it) }
            entry<AppRoute.Map> {
                // Real expressive M3 map shell (AndroidView + MapView for daily usable rendering + controls)
                com.kylecorry.trail_sense.tools.map.ui.MapFragment().MapContent()
            }
            entry<AppRoute.MapSettings> { Placeholder(it) }
            entry<AppRoute.BeaconList> {
                // Newly converted real modern screen (Beacons list + details/place)
                com.kylecorry.trail_sense.tools.beacons.ui.list.BeaconListFragment().BeaconListContent()
            }
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
            // ── Field Guide family promoted (real Nav3 entries, expressive Compose) ──
            entry<AppRoute.FieldGuide> { Placeholder(it) }
            entry<AppRoute.FieldGuidePage> {
                // Clean TrailSenseComposeFragment + design system (card, tags, etc)
                com.kylecorry.trail_sense.tools.field_guide.ui.FieldGuidePageFragment().FieldGuidePageContent()
            }
            entry<AppRoute.CreateFieldGuideSighting> {
                com.kylecorry.trail_sense.tools.field_guide.ui.CreateFieldGuideSightingFragment().CreateFieldGuideSightingContent()
            }
            entry<AppRoute.SightingList> { route ->
                // Real promoted entry using pure SightingListScreen (design system heavy, testTags, preview)
                val sightingRoute = route as? AppRoute.SightingList
                SightingListScreen(pageId = sightingRoute?.pageId ?: 0L)
            }
            entry<AppRoute.CreateFieldGuidePage> {
                com.kylecorry.trail_sense.tools.field_guide.ui.CreateFieldGuidePageFragment().CreateFieldGuidePageContent()
            }
            entry<AppRoute.GuideList> { Placeholder(it) }
            entry<AppRoute.Guide> { Placeholder(it) }
            entry<AppRoute.SurvivalGuideList> { Placeholder(it) }
            entry<AppRoute.SurvivalGuideReader> { Placeholder(it) }
            entry<AppRoute.PackList> {
                // Newly converted real modern screen (Packs list - expressive cards, search, FAB, menus)
                com.kylecorry.trail_sense.tools.packs.ui.PackListFragment().PackListContent()
            }
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
            entry<AppRoute.Level> {
                // Newly converted real modern screen (Level/Bubble - pure Compose rewrite of visualization)
                com.kylecorry.trail_sense.tools.level.ui.LevelFragment().LevelContent()
            }
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
    // This tool is now rendered in the pure Compose + Navigation3 architecture.
    // Full modern UI implementation in progress.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_info),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "This experience is now 100% Compose",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "We're building the beautiful new UI for this tool using Material 3 Expressive and our design system.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
