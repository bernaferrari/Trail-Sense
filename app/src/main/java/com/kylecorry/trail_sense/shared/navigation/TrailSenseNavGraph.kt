package com.kylecorry.trail_sense.shared.navigation

import androidx.navigation.NavAction
import androidx.navigation.NavArgument
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.NavGraphNavigator
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.fragment.FragmentNavigator
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment

object TrailSenseNavGraph {

    fun create(navController: NavController, startDestination: Int): NavGraph {
        val provider = navController.navigatorProvider
        val graph = NavGraph(provider.getNavigator(NavGraphNavigator::class.java))
        graph.setStartDestination(startDestination)
        val fragmentNavigator = provider.getNavigator(FragmentNavigator::class.java)

        fragmentNavigator.createDestination().apply {
            id = R.id.action_navigation
            setClassName("com.kylecorry.trail_sense.tools.navigation.ui.NavigatorFragment")
            label = "activity_navigator"
            addArgument("destination", NavArgument.Builder()
                .setType(NavType.LongType)
                .setDefaultValue(0L)
                .build())
            putAction(
                R.id.action_navigatorFragment_to_calibrateGPSFragment,
                NavAction(R.id.calibrateGPSFragment, NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build())
            )
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.action_astronomy
            setClassName("com.kylecorry.trail_sense.tools.astronomy.ui.AstronomyFragment")
            label = "AstronomyFragment"
            putAction(
                R.id.action_astronomyFragment_to_calibrateGPSFragment,
                NavAction(R.id.calibrateGPSFragment, NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build())
            )
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.action_experimental_tools
            setClassName("com.kylecorry.trail_sense.tools.tools.ui.ToolsFragment")
            label = "ToolsFragment"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.action_weather
            setClassName("com.kylecorry.trail_sense.tools.weather.ui.WeatherFragment")
            label = "activity_weather"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.mapFragment
            setClassName("com.kylecorry.trail_sense.tools.map.ui.MapFragment")
            label = "MapFragment"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.mapSettingsFragment
            setClassName("com.kylecorry.trail_sense.tools.map.ui.MapSettingsFragment")
            label = "MapSettingsFragment"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.offlineMapViewFragment
            setClassName("com.kylecorry.trail_sense.tools.offline_maps.ui.vector_maps.ViewVectorMapFragment")
            label = "OfflineMapViewFragment"
            addArgument("offline_map_file_id", NavArgument.Builder()
                .setType(NavType.LongType)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.action_settings
            setClassName("com.kylecorry.trail_sense.settings.ui.SettingsFragment")
            label = "SettingsFragment"
            putAction(
                R.id.action_settings_to_sensor_settings,
                NavAction(R.id.sensorSettingsFragment, NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build())
            )
            putAction(
                R.id.action_settings_to_diagnostics,
                NavAction(R.id.diagnosticsFragment, NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build())
            )
            putAction(
                R.id.action_settings_to_error_settings,
                NavAction(R.id.errorSettingsFragment, NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build())
            )
            putAction(
                R.id.action_settings_to_experimental_settings,
                NavAction(R.id.experimentalSettingsFragment, NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build())
            )
            putAction(
                R.id.action_settings_to_plugins_settings,
                NavAction(R.id.pluginsSettingsFragment, NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build())
            )
            putAction(
                R.id.action_settings_to_unit_settings,
                NavAction(R.id.unitSettingsFragment, NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build())
            )
            putAction(
                R.id.action_settings_to_power_settings,
                NavAction(R.id.powerSettingsFragment, NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build())
            )
            putAction(
                R.id.action_settings_to_privacy_settings,
                NavAction(R.id.privacySettingsFragment, NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build())
            )
            putAction(
                R.id.action_settings_to_theme_settings,
                NavAction(R.id.themeSettingsFragment, NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build())
            )
            putAction(
                R.id.action_settings_to_backup_settings,
                NavAction(R.id.backupSettingsFragment, NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build())
            )
            putAction(
                R.id.action_action_settings_to_licenseFragment,
                NavAction(R.id.licenseFragment, NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build())
            )
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.experimentationFragment
            setClassName("com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment")
            label = "ExperimentationFragment"
            addArgument(TrailSenseComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(TrailSenseComposeDestinationFragment.DESTINATION_EXPERIMENTATION)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fieldGuideFragment
            setClassName("com.kylecorry.trail_sense.tools.field_guide.ui.FieldGuideFragment")
            label = "FieldGuideFragment"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fieldGuidePageFragment
            setClassName("com.kylecorry.trail_sense.tools.field_guide.ui.FieldGuidePageFragment")
            label = "FieldGuidePageFragment"
            addArgument("page_id", NavArgument.Builder()
                .setType(NavType.LongType)
                .setDefaultValue(0L)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.createFieldGuideSightingFragment
            setClassName("com.kylecorry.trail_sense.tools.field_guide.ui.CreateFieldGuideSightingFragment")
            label = "CreateFieldGuideSightingFragment"
            addArgument("page_id", NavArgument.Builder()
                .setType(NavType.LongType)
                .build())
            addArgument("sighting_id", NavArgument.Builder()
                .setType(NavType.LongType)
                .setDefaultValue(0L)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.sightingListFragment
            setClassName("com.kylecorry.trail_sense.tools.field_guide.ui.SightingListFragment")
            label = "SightingListFragment"
            addArgument("page_id", NavArgument.Builder()
                .setType(NavType.LongType)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.createFieldGuidePageFragment
            setClassName("com.kylecorry.trail_sense.tools.field_guide.ui.CreateFieldGuidePageFragment")
            label = "CreateFieldGuidePageFragment"
            addArgument("page_id", NavArgument.Builder()
                .setType(NavType.LongType)
                .setDefaultValue(0L)
                .build())
            addArgument("classification_id", NavArgument.Builder()
                .setType(NavType.LongType)
                .setDefaultValue(0L)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.signalFinderFragment
            setClassName("com.kylecorry.trail_sense.tools.signal_finder.ui.ToolSignalFinderFragment")
            label = "SignalFinderFragment"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.toolDeclinationFragment
            setClassName("com.kylecorry.trail_sense.tools.declination.ui.ToolDeclinationFragment")
            label = "ToolDeclinationFragment"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.beacon_list
            setClassName("com.kylecorry.trail_sense.tools.beacons.ui.list.BeaconListFragment")
            label = "BeaconListFragment"
            addArgument("initial_location", NavArgument.Builder()
                .setType(NavType.ParcelableType(GeoUri::class.java))
                .setIsNullable(true)
                .setDefaultValue(null)
                .build())
            putAction(
                R.id.action_beaconListFragment_to_placeBeaconFragment,
                NavAction(R.id.placeBeaconFragment, NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build())
            )
            putAction(
                R.id.action_beacon_list_to_beaconDetailsFragment,
                NavAction(R.id.beaconDetailsFragment, NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build())
            )
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.placeBeaconFragment
            setClassName("com.kylecorry.trail_sense.tools.beacons.ui.PlaceBeaconFragment")
            label = "PlaceBeaconFragment"
            addArgument("edit_beacon", NavArgument.Builder()
                .setType(NavType.LongType)
                .setDefaultValue(0L)
                .build())
            addArgument("initial_group", NavArgument.Builder()
                .setType(NavType.LongType)
                .setDefaultValue(0L)
                .build())
            addArgument("initial_location", NavArgument.Builder()
                .setType(NavType.ParcelableType(GeoUri::class.java))
                .setIsNullable(true)
                .setDefaultValue(null)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.clinometerFragment
            setClassName("com.kylecorry.trail_sense.tools.clinometer.ui.ClinometerFragment")
            label = "ClinometerFragment"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.calibrateAltimeterFragment
            setClassName("com.kylecorry.trail_sense.settings.ui.CalibrateAltimeterFragment")
            label = "CalibrateAltimeterFragment"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.calibrateBarometerFragment
            setClassName("com.kylecorry.trail_sense.settings.ui.CalibrateBarometerFragment")
            label = "CalibrateBarometerFragment"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.calibrateCompassFragment
            setClassName("com.kylecorry.trail_sense.settings.ui.CalibrateCompassFragment")
            label = "CalibrateCompassFragment"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.calibrateGPSFragment
            setClassName("com.kylecorry.trail_sense.settings.ui.CalibrateGPSFragment")
            label = "CalibrateGPSFragment"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.licenseFragment
            setClassName("com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment")
            label = "LicenseFragment"
            addArgument(TrailSenseComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(TrailSenseComposeDestinationFragment.DESTINATION_LICENSES)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.weatherSettingsFragment
            setClassName("com.kylecorry.trail_sense.tools.weather.ui.WeatherSettingsFragment")
            label = "WeatherSettingsFragment"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.navigationSettingsFragment
            setClassName("com.kylecorry.trail_sense.tools.navigation.ui.NavigationSettingsFragment")
            label = "NavigationSettingsFragment"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.pathsSettingsFragment
            setClassName("com.kylecorry.trail_sense.settings.ui.PathsSettingsFragment")
            label = "PathsSettingsFragment"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.beaconsSettingsFragment
            setClassName("com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment")
            label = "BeaconSettingsFragment"
            addArgument(TrailSenseComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(TrailSenseComposeDestinationFragment.DESTINATION_BEACON_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.pluginsSettingsFragment
            setClassName("com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment")
            label = "PluginsSettingsFragment"
            addArgument(TrailSenseComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(TrailSenseComposeDestinationFragment.DESTINATION_PLUGINS_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.pluginDetailsFragment
            setClassName("com.kylecorry.trail_sense.settings.ui.PluginDetailsFragment")
            label = "PluginDetailsFragment"
            addArgument("package_id", NavArgument.Builder()
                .setType(NavType.StringType)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.astronomySettingsFragment
            setClassName("com.kylecorry.trail_sense.tools.astronomy.ui.AstronomySettingsFragment")
            label = "AstronomySettingsFragment"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.guideListFragment
            setClassName("com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment")
            label = "GuideListFragment"
            addArgument(TrailSenseComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(TrailSenseComposeDestinationFragment.DESTINATION_GUIDE_LIST)
                .build())
            putAction(
                R.id.action_guideListFragment_to_guideFragment,
                NavAction(R.id.guideFragment, NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build())
            )
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.guideFragment
            setClassName("com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment")
            label = "GuideFragment"
            addArgument(TrailSenseComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(TrailSenseComposeDestinationFragment.DESTINATION_GUIDE)
                .build())
            addArgument("guide_name", NavArgument.Builder()
                .setType(NavType.StringType)
                .setIsNullable(true)
                .setDefaultValue(null)
                .build())
            addArgument("guide_contents", NavArgument.Builder()
                .setType(NavType.IntType)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.levelFragment
            setClassName("com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment")
            label = "LevelFragment"
            addArgument(TrailSenseComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(TrailSenseComposeDestinationFragment.DESTINATION_LEVEL)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.beaconDetailsFragment
            setClassName("com.kylecorry.trail_sense.tools.beacons.ui.BeaconDetailsFragment")
            label = "BeaconDetailsFragment"
            addArgument("beacon_id", NavArgument.Builder()
                .setType(NavType.LongType)
                .build())
            putAction(
                R.id.action_beacon_details_to_beacon_edit,
                NavAction(R.id.placeBeaconFragment, NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build())
            )
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.packItemListFragment
            setClassName("com.kylecorry.trail_sense.tools.packs.ui.PackItemListFragment")
            label = "ItemListFragment"
            addArgument("pack_id", NavArgument.Builder()
                .setType(NavType.LongType)
                .setDefaultValue(0L)
                .build())
            putAction(
                R.id.action_action_inventory_to_createItemFragment,
                NavAction(R.id.createItemFragment, NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build())
            )
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.createItemFragment
            setClassName("com.kylecorry.trail_sense.tools.packs.ui.CreateItemFragment")
            label = "CreateItemFragment"
            addArgument("edit_item_id", NavArgument.Builder()
                .setType(NavType.LongType)
                .setDefaultValue(0L)
                .build())
            addArgument("pack_id", NavArgument.Builder()
                .setType(NavType.LongType)
                .setDefaultValue(0L)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.toolConvertFragment
            setClassName("com.kylecorry.trail_sense.tools.convert.ui.FragmentToolConvert")
            label = "ConvertFragment"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.toolCliffHeightFragment
            setClassName("com.kylecorry.trail_sense.tools.cliffheight.ui.ToolCliffHeightFragment")
            label = "ToolCliffHeightFragment"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.toolWhistleFragment
            setClassName("com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment")
            label = "ToolWhistleFragment"
            addArgument(TrailSenseComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(TrailSenseComposeDestinationFragment.DESTINATION_WHISTLE)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentToolSolarPanel
            setClassName("com.kylecorry.trail_sense.tools.solarpanel.ui.FragmentToolSolarPanel")
            label = "FragmentToolSolarPanel"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.waterPurificationFragment
            setClassName("com.kylecorry.trail_sense.tools.waterpurification.ui.WaterPurificationFragment")
            label = "WaterPurificationFragment"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentToolFlashlight
            setClassName("com.kylecorry.trail_sense.tools.flashlight.ui.FragmentToolFlashlight")
            label = "FragmentToolFlashlight"
            putAction(
                R.id.action_flashlight_to_screen_flashlight,
                NavAction(R.id.fragmentToolScreenFlashlight, NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build())
            )
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.toolClockFragment
            setClassName("com.kylecorry.trail_sense.tools.clock.ui.ToolClockFragment")
            label = "ToolClockFragment"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentToolLightning
            setClassName("com.kylecorry.trail_sense.tools.lightning.ui.FragmentToolLightning")
            label = "FragmentToolLightning"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.rulerFragment
            setClassName("com.kylecorry.trail_sense.tools.ruler.ui.RulerFragment")
            label = "RulerFragment"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentToolBattery
            setClassName("com.kylecorry.trail_sense.tools.battery.ui.FragmentToolBattery")
            label = "FragmentToolBattery"
            putAction(
                R.id.action_settings_to_power_settings,
                NavAction(R.id.powerSettingsFragment, NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build())
            )
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentToolTriangulate
            setClassName("com.kylecorry.trail_sense.tools.triangulate.ui.FragmentToolTriangulate")
            label = "FragmentToolTriangulate"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentToolBallistics
            setClassName("com.kylecorry.trail_sense.tools.ballistics.ui.FragmentToolBallistics")
            label = "FragmentToolBallistics"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentToolPermits
            setClassName("com.kylecorry.trail_sense.tools.permits.ui.FragmentToolPermits")
            label = "FragmentToolPermits"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentToolMetalDetector
            setClassName("com.kylecorry.trail_sense.tools.metaldetector.ui.FragmentToolMetalDetector")
            label = "FragmentToolMetalDetector"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentToolSurvivalGuideList
            setClassName("com.kylecorry.trail_sense.tools.survival_guide.ui.FragmentToolSurvivalGuideList")
            label = "FragmentToolSurvivalGuideList"
            addArgument("search_query", NavArgument.Builder()
                .setType(NavType.StringType)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentToolSurvivalGuideReader
            setClassName("com.kylecorry.trail_sense.tools.survival_guide.ui.FragmentToolSurvivalGuideReader")
            label = "FragmentToolSurvivalGuideReader"
            addArgument("chapter_resource_id", NavArgument.Builder()
                .setType(NavType.IntType)
                .build())
            addArgument("header_index", NavArgument.Builder()
                .setType(NavType.IntType)
                .setDefaultValue(0)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentToolNotes
            setClassName("com.kylecorry.trail_sense.tools.notes.ui.FragmentToolNotes")
            label = "FragmentToolNotes"
            putAction(
                R.id.action_fragmentToolNotes_to_fragmentToolNotesCreate,
                NavAction(R.id.fragmentToolNotesCreate, NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build())
            )
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentToolNotesCreate
            setClassName("com.kylecorry.trail_sense.tools.notes.ui.FragmentToolNotesCreate")
            label = "FragmentToolNotesCreate"
            addArgument("edit_note_id", NavArgument.Builder()
                .setType(NavType.LongType)
                .setDefaultValue(0L)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentToolWhiteNoise
            setClassName("com.kylecorry.trail_sense.tools.whitenoise.ui.FragmentToolWhiteNoise")
            label = "FragmentToolWhiteNoise"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentBacktrack
            setClassName("com.kylecorry.trail_sense.tools.paths.ui.PathsFragment")
            label = "FragmentBacktrack"
            putAction(
                R.id.action_fragmentBacktrack_to_action_navigation,
                NavAction(R.id.action_navigation, NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_left)
                    .setExitAnim(R.anim.slide_out_right)
                    .setPopEnterAnim(R.anim.slide_in_right)
                    .setPopExitAnim(R.anim.slide_out_left)
                    .setPopUpTo(R.id.action_navigation, true)
                    .build())
            )
            putAction(
                R.id.action_backtrack_to_path,
                NavAction(R.id.pathDetailsFragment, NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build())
            )
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.cloudFragment
            setClassName("com.kylecorry.trail_sense.tools.clouds.ui.CloudFragment")
            label = "CloudFragment"
            addArgument("open_scanner", NavArgument.Builder()
                .setType(NavType.BoolType)
                .setDefaultValue(false)
                .build())
            putAction(
                R.id.action_cloud_to_cloud_picker,
                NavAction(R.id.cloudResultsFragment, NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build())
            )
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.sensorDetailsFragment
            setClassName("com.kylecorry.trail_sense.tools.sensors.ui.SensorDetailsFragment")
            label = "SensorDetailsFragment"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.toolLightFragment
            setClassName("com.kylecorry.trail_sense.tools.light.ui.ToolLightFragment")
            label = "ToolLightFragment"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.tidesFragment
            setClassName("com.kylecorry.trail_sense.tools.tides.ui.TidesFragment")
            label = "TidesFragment"
            putAction(
                R.id.action_tides_to_tideList,
                NavAction(R.id.tideListFragment, NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build())
            )
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentToolScreenFlashlight
            setClassName("com.kylecorry.trail_sense.tools.flashlight.ui.FragmentToolScreenFlashlight")
            label = "FragmentToolScreenFlashlight"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentToolPedometer
            setClassName("com.kylecorry.trail_sense.tools.pedometer.ui.FragmentToolPedometer")
            label = "FragmentToolSpeedometer"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentTurnBackSettings
            setClassName("com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment")
            label = "TurnBackSettingsFragment"
            addArgument(TrailSenseComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(TrailSenseComposeDestinationFragment.DESTINATION_TURN_BACK_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentWaterBoilTimerSettings
            setClassName("com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment")
            label = "WaterBoilTimerSettingsFragment"
            addArgument(TrailSenseComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(TrailSenseComposeDestinationFragment.DESTINATION_WATER_BOIL_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.calibrateOdometerFragment
            setClassName("com.kylecorry.trail_sense.tools.pedometer.ui.PedometerSettingsFragment")
            label = "CalibrateOdometerFragment"
            putAction(
                R.id.action_calibrate_pedometer_to_estimate_stride_length,
                NavAction(R.id.fragmentStrideLengthEstimation, NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build())
            )
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.createTideFragment
            setClassName("com.kylecorry.trail_sense.tools.tides.ui.CreateTideFragment")
            label = "CreateTideFragment"
            addArgument("edit_tide_id", NavArgument.Builder()
                .setType(NavType.LongType)
                .setDefaultValue(0L)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.tideListFragment
            setClassName("com.kylecorry.trail_sense.tools.tides.ui.TideListFragment")
            label = "TideListFragment"
            putAction(
                R.id.action_tideList_to_createTide,
                NavAction(R.id.createTideFragment, NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build())
            )
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.photoMapsFragment
            setClassName("com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps.PhotoMapsFragment")
            label = "PhotoMapsFragment"
            addArgument("mapId", NavArgument.Builder()
                .setType(NavType.LongType)
                .setDefaultValue(0L)
                .build())
            addArgument("autoLockLocation", NavArgument.Builder()
                .setType(NavType.BoolType)
                .setDefaultValue(false)
                .build())
            putAction(
                R.id.action_maps_to_path,
                NavAction(R.id.pathDetailsFragment, NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build())
            )
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.mapListFragment
            setClassName("com.kylecorry.trail_sense.tools.offline_maps.ui.OfflineMapListFragment")
            label = "MapListFragment"
            putAction(
                R.id.action_mapList_to_maps,
                NavAction(R.id.photoMapsFragment, NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build())
            )
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.flashlightSettingsFragment
            setClassName("com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment")
            label = "FlashlightSettingsFragment"
            addArgument(TrailSenseComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(TrailSenseComposeDestinationFragment.DESTINATION_FLASHLIGHT_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.thermometerSettingsFragment
            setClassName("com.kylecorry.trail_sense.settings.ui.ThermometerSettingsFragment")
            label = "ThermometerSettingsFragment"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.cellSignalSettingsFragment
            setClassName("com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment")
            label = "CellSignalSettingsFragment"
            addArgument(TrailSenseComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(TrailSenseComposeDestinationFragment.DESTINATION_CELL_SIGNAL_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.bubbleLevelSettingsFragment
            setClassName("com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment")
            label = "BubbleLevelSettingsFragment"
            addArgument(TrailSenseComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(TrailSenseComposeDestinationFragment.DESTINATION_BUBBLE_LEVEL_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.clockSettingsFragment
            setClassName("com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment")
            label = "ClockSettingsFragment"
            addArgument(TrailSenseComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(TrailSenseComposeDestinationFragment.DESTINATION_CLOCK_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.privacySettingsFragment
            setClassName("com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment")
            label = "PrivacySettingsFragment"
            addArgument(TrailSenseComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(TrailSenseComposeDestinationFragment.DESTINATION_PRIVACY_SETTINGS)
                .build())
            putAction(
                R.id.action_privacy_settings_to_gps_calibration,
                NavAction(R.id.calibrateGPSFragment, NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build())
            )
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.themeSettingsFragment
            setClassName("com.kylecorry.trail_sense.settings.ui.ThemeSettingsFragment")
            label = "ThemeSettingsFragment"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.backupSettingsFragment
            setClassName("com.kylecorry.trail_sense.settings.ui.BackupSettingsFragment")
            label = "BackupSettingsFragment"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.powerSettingsFragment
            setClassName("com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment")
            label = "PowerSettingsFragment"
            addArgument(TrailSenseComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(TrailSenseComposeDestinationFragment.DESTINATION_POWER_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.unitSettingsFragment
            setClassName("com.kylecorry.trail_sense.settings.ui.UnitSettingsFragment")
            label = "UnitSettingsFragment"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.experimentalSettingsFragment
            setClassName("com.kylecorry.trail_sense.settings.ui.ExperimentalSettingsFragment")
            label = "ExperimentalSettingsFragment"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.toolRulerSettingsFragment
            setClassName("com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment")
            label = "RulerSettingsFragment"
            addArgument(TrailSenseComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(TrailSenseComposeDestinationFragment.DESTINATION_RULER_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.sensorSettingsFragment
            setClassName("com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment")
            label = "SensorSettingsFragment"
            addArgument(TrailSenseComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(TrailSenseComposeDestinationFragment.DESTINATION_SENSOR_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.photoMapSettingsFragment
            setClassName("com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps.PhotoMapSettingsFragment")
            label = "PhotoMapSettingsFragment"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.temperatureEstimationFragment
            setClassName("com.kylecorry.trail_sense.tools.temperature_estimation.ui.TemperatureEstimationFragment")
            label = "TemperatureEstimationFragment"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.packListFragment
            setClassName("com.kylecorry.trail_sense.tools.packs.ui.PackListFragment")
            label = "PackListFragment"
            putAction(
                R.id.action_pack_to_pack_items,
                NavAction(R.id.packItemListFragment, NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build())
            )
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.diagnosticsFragment
            setClassName("com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment")
            label = "DiagnosticsFragment"
            addArgument(TrailSenseComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(TrailSenseComposeDestinationFragment.DESTINATION_DIAGNOSTICS)
                .build())
            putAction(
                R.id.action_diagnostics_to_sensor_details,
                NavAction(R.id.sensorDetailsFragment, NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build())
            )
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.pathDetailsFragment
            setClassName("com.kylecorry.trail_sense.tools.paths.ui.PathOverviewFragment")
            label = "PathDetailsFragment"
            addArgument("destination", NavArgument.Builder()
                .setType(NavType.LongType)
                .setDefaultValue(0L)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.tideSettingsFragment
            setClassName("com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment")
            label = "TideSettingsFragment"
            addArgument(TrailSenseComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(TrailSenseComposeDestinationFragment.DESTINATION_TIDE_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.clinometerSettingsFragment
            setClassName("com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment")
            label = "ClinometerSettingsFragment"
            addArgument(TrailSenseComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(TrailSenseComposeDestinationFragment.DESTINATION_CLINOMETER_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.augmentedRealitySettingsFragment
            setClassName("com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealitySettingsFragment")
            label = "AugmentedRealitySettingsFragment"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.cameraSettingsFragment
            setClassName("com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment")
            label = "CameraSettingsFragment"
            addArgument(TrailSenseComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(TrailSenseComposeDestinationFragment.DESTINATION_CAMERA_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.errorSettingsFragment
            setClassName("com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment")
            label = "ErrorSettingsFragment"
            addArgument(TrailSenseComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(TrailSenseComposeDestinationFragment.DESTINATION_ERROR_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.scanQrFragment
            setClassName("com.kylecorry.trail_sense.tools.qr.ui.ScanQRFragment")
            label = "ScanQRFragment"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.augmentedRealityFragment
            setClassName("com.kylecorry.trail_sense.tools.augmented_reality.ui.AugmentedRealityFragment")
            label = "AugmentedRealityFragment"
            addArgument("mode", NavArgument.Builder()
                .setType(NavType.LongType)
                .setDefaultValue(1L)
                .build())
            addArgument("camera_enabled", NavArgument.Builder()
                .setType(NavType.BoolType)
                .setDefaultValue(true)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentStrideLengthEstimation
            setClassName("com.kylecorry.trail_sense.tools.pedometer.ui.FragmentStrideLengthEstimation")
            label = "FragmentStrideLengthEstimation"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.cloudResultsFragment
            setClassName("com.kylecorry.trail_sense.tools.clouds.ui.CloudResultsFragment")
            label = "CloudResultsFragment"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.climateFragment
            setClassName("com.kylecorry.trail_sense.tools.climate.ui.ClimateFragment")
            label = "ClimateFragment"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.mirrorCameraFragment
            setClassName("com.kylecorry.trail_sense.tools.mirror.ui.ToolMirrorCameraFragment")
            label = "ToolMirrorCameraFragment"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.magnifierFragment
            setClassName("com.kylecorry.trail_sense.tools.magnifier.ui.ToolMagnifierFragment")
            label = "ToolMagnifierFragment"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.turnBackFragment
            setClassName("com.kylecorry.trail_sense.tools.turn_back.ui.TurnBackFragment")
            label = "TurnBackFragment"
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentLocalMessaging
            setClassName("com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment")
            label = "FragmentToolLocalMessaging"
            addArgument(TrailSenseComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(TrailSenseComposeDestinationFragment.DESTINATION_LOCAL_MESSAGING)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentLocalTalk
            setClassName("com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment")
            label = "FragmentToolLocalTalk"
            addArgument(TrailSenseComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(TrailSenseComposeDestinationFragment.DESTINATION_LOCAL_TALK)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.toolsSettingsFragment
            setClassName("com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment")
            label = "ToolsSettingsFragment"
            addArgument(TrailSenseComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(TrailSenseComposeDestinationFragment.DESTINATION_TOOLS_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentToolErrorHandler
            setClassName("com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment")
            label = "ErrorHandlerFragment"
            addArgument(TrailSenseComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(TrailSenseComposeDestinationFragment.DESTINATION_ERROR_HANDLER)
                .build())
            addArgument("tool_id", NavArgument.Builder()
                .setType(NavType.LongType)
                .build())
            addArgument("error", NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue("")
                .build())
            graph.addDestination(this)
        }

        return graph
    }

}
