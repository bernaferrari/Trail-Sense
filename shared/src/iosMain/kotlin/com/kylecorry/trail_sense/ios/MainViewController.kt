package com.kylecorry.trail_sense.ios

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.files.appFiles
import androidx.compose.ui.window.ComposeUIViewController
import com.kylecorry.trail_sense.main.BindSharedAppActions
import com.kylecorry.trail_sense.main.SharedApp
import com.kylecorry.trail_sense.main.persistence.iosAppDatabase
import com.kylecorry.trail_sense.main.registerSharedApp
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.SharedMapFiles
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.diskOfflineMapStorage
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.renderNativeMapPdf
import com.kylecorry.trail_sense.settings.backup.FilePortableBackup
import com.kylecorry.trail_sense.shared.images.IosAppImages
import com.kylecorry.trail_sense.shared.images.ComposeBundledImages
import com.kylecorry.trail_sense.shared.text.ComposeAppStrings
import com.kylecorry.trail_sense.shared.text.IosPlatformFormats
import com.kylecorry.trail_sense.shared.text.appString
import com.kylecorry.trail_sense.shared.navigation.Route
import com.mohamedrejeb.calf.sf.symbols.SFSymbol
import com.mohamedrejeb.calf.ui.ExperimentalCalfUiApi
import com.mohamedrejeb.calf.ui.navigation.AdaptiveNavigationBar
import com.mohamedrejeb.calf.ui.navigation.AdaptiveScaffold
import com.mohamedrejeb.calf.ui.navigation.UIKitTabBarConfiguration
import com.mohamedrejeb.calf.ui.navigation.UIKitUITabBarItem
import com.mohamedrejeb.calf.ui.uikit.UIKitImage
import com.kylecorry.trail_sense.shared.generated.resources.Res
import com.kylecorry.trail_sense.shared.generated.resources.backtrack
import com.kylecorry.trail_sense.shared.generated.resources.sunrise
import com.kylecorry.trail_sense.shared.generated.resources.sunset
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.tools.astronomy.ui.ScheduledSunAlerts
import com.kylecorry.trail_sense.tools.navigation.ui.LocalLinearCompassAvailable
import com.kylecorry.trail_sense.tools.paths.domain.BacktrackRecorder
import com.kylecorry.trail_sense.tools.paths.infrastructure.LocationBacktrackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.*
import platform.CoreMotion.CMMotionManager
import platform.UIKit.UIViewController

interface IosBacktrackActivity {
    fun start()
    fun end()
}

@OptIn(ExperimentalCalfUiApi::class, ExperimentalForeignApi::class)
fun MainViewController(
    databasePath: String,
    turnBackScheduler: IosTurnBackScheduler?,
    backtrackActivity: IosBacktrackActivity
): UIViewController {
    val strings = ComposeAppStrings()
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val database = iosAppDatabase(databasePath)
    val files = appFiles()
    registerSharedApp(
        database = database,
        deviceProfile = IosDeviceProfile(),
        strings = strings,
        bundledImages = ComposeBundledImages(),
        images = IosAppImages(),
        formats = IosPlatformFormats(),
        hasMotion = CMMotionManager().deviceMotionAvailable,
        hasCamera = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) != null,
        mapFiles = SharedMapFiles(diskOfflineMapStorage(files), ::renderNativeMapPdf),
        portableBackup = FilePortableBackup(database, files),
        createSunAlerts = { preferences, sensors ->
            listOf(true, false).map { sunrise ->
                ScheduledSunAlerts(
                    name = strings.get(if (sunrise) Res.string.sunrise else Res.string.sunset),
                    isSunrise = sunrise,
                    preferences = preferences.astronomy,
                    location = {
                        val coordinate = sensors.getLocation()
                        coordinate.takeIf { it != Coordinate.zero || sensors.lastKnownLocationAccuracy != null }
                    },
                    replaceNotifications = ::replaceSunNotifications
                ).also { service -> scope.launch { service.restart() } }
            }
        },
        turnBackAlarm = turnBackScheduler?.let { IosTurnBackAlarm(it, Preferences(), strings) },
        createBacktrack = { preferences, paths, logger ->
            LocationBacktrackService(
                strings.get(Res.string.backtrack), preferences.paths, BacktrackRecorder(paths), paths::endBacktrackPath,
                scope, logger, ::backtrackLocations, ::isBacktrackLocationBlocked,
                recordingChanged = { active -> if (active) backtrackActivity.start() else backtrackActivity.end() }
            ).also { backtrack -> scope.launch { backtrack.restart() } }
        }
    )

    return ComposeUIViewController {
        CompositionLocalProvider(LocalLinearCompassAvailable provides false) {
            SharedApp(strings, bindNavigation = { BindSharedAppActions(it, iosAppActions) }) { tabs, selectedIndex, select, content ->
                AdaptiveScaffold(
                    containerColor = MaterialTheme.colorScheme.surface,
                    // The Navigation toolbar paints the status area as it collapses.
                    contentWindowInsets = if (tabs.getOrNull(selectedIndex)?.route is Route.Navigation) {
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                    } else WindowInsets.safeDrawing,
                    bottomBar = {
                        if (selectedIndex >= 0) {
                            AdaptiveNavigationBar(
                                iosItems = tabs.mapIndexed { index, tab ->
                                    val (icon, selectedIcon) = tabSymbols[index]
                                    UIKitUITabBarItem(
                                        title = appString(tab.label),
                                        image = UIKitImage.SystemName(icon),
                                        selectedImage = UIKitImage.SystemName(selectedIcon)
                                    )
                                },
                                iosSelectedIndex = selectedIndex,
                                iosOnItemSelected = select,
                                iosConfiguration = UIKitTabBarConfiguration(
                                    selectedItemColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    },
                    content = content
                )
            }
        }
    }
}

private val tabSymbols = listOf(
    SFSymbol.location to SFSymbol.locationFill,
    SFSymbol.wrenchAndScrewdriver to SFSymbol.wrenchAndScrewdriverFill,
    SFSymbol.cloudSun to SFSymbol.cloudSunFill,
    SFSymbol.gearshape to SFSymbol.gearshape
)
