package com.kylecorry.trail_sense.shared.navigation

import androidx.navigation.NavAction
import androidx.navigation.NavArgument
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.NavGraphNavigator
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.compose.*

object TrailSenseNavGraph {

    fun create(navController: NavController, startDestination: Int): NavGraph {
        val provider = navController.navigatorProvider
        val graph = NavGraph(provider.getNavigator(NavGraphNavigator::class.java))
        graph.setStartDestination(startDestination)
        val composeNavigator = provider.getNavigator(TrailSenseComposeNavigator::class.java)

        composeNavigator.createDestination().apply {
            id = R.id.action_navigation
            label = "NavigationScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_NAVIGATION)
                .build())
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

        composeNavigator.createDestination().apply {
            id = R.id.action_astronomy
            label = "AstronomyScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_ASTRONOMY)
                .build())
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

        composeNavigator.createDestination().apply {
            id = R.id.action_experimental_tools
            label = "ToolsScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_TOOLS)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.action_weather
            label = "WeatherScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_WEATHER)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.mapFragment
            label = "MapScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_MAP)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.mapSettingsFragment
            label = "MapSettingsScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_MAP_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.offlineMapViewFragment
            label = "ViewVectorMapScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_VECTOR_MAP_VIEW)
                .build())
            addArgument("offline_map_file_id", NavArgument.Builder()
                .setType(NavType.LongType)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.action_settings
            label = "SettingsScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_SETTINGS)
                .build())
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
                R.id.action_settings_to_backup_settings,
                NavAction(R.id.backupSettingsFragment, NavOptions.Builder()
                    .setEnterAnim(R.anim.slide_in_right)
                    .setExitAnim(R.anim.slide_out_left)
                    .setPopEnterAnim(R.anim.slide_in_left)
                    .setPopExitAnim(R.anim.slide_out_right)
                    .build())
            )
            putAction(
                R.id.action_settings_to_about,
                NavAction(R.id.aboutFragment, NavOptions.Builder()
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

        composeNavigator.createDestination().apply {
            id = R.id.experimentationFragment
            label = "ExperimentationScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_EXPERIMENTATION)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.fieldGuideFragment
            label = "FieldGuideScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_FIELD_GUIDE)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.fieldGuidePageFragment
            label = "FieldGuidePageScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_FIELD_GUIDE_PAGE)
                .build())
            addArgument("page_id", NavArgument.Builder()
                .setType(NavType.LongType)
                .setDefaultValue(0L)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.fieldGuideSettingsFragment
            label = "FieldGuideSettingsScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_FIELD_GUIDE_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.createFieldGuideSightingFragment
            label = "CreateFieldGuideSightingScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_CREATE_FIELD_GUIDE_SIGHTING)
                .build())
            addArgument("page_id", NavArgument.Builder()
                .setType(NavType.LongType)
                .build())
            addArgument("sighting_id", NavArgument.Builder()
                .setType(NavType.LongType)
                .setDefaultValue(0L)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.sightingListFragment
            label = "SightingListScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_SIGHTING_LIST)
                .build())
            addArgument("page_id", NavArgument.Builder()
                .setType(NavType.LongType)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.createFieldGuidePageFragment
            label = "CreateFieldGuidePageScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_CREATE_FIELD_GUIDE_PAGE)
                .build())
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

        composeNavigator.createDestination().apply {
            id = R.id.signalFinderFragment
            label = "SignalFinderScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_SIGNAL_FINDER)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.toolDeclinationFragment
            label = "DeclinationScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_DECLINATION)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.beacon_list
            label = "BeaconListScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_BEACON_LIST)
                .build())
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

        composeNavigator.createDestination().apply {
            id = R.id.placeBeaconFragment
            label = "PlaceBeaconScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_PLACE_BEACON)
                .build())
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

        composeNavigator.createDestination().apply {
            id = R.id.clinometerFragment
            label = "ClinometerScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_CLINOMETER)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.calibrateAltimeterFragment
            label = "CalibrateAltimeterScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_CALIBRATE_ALTIMETER)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.calibrateBarometerFragment
            label = "CalibrateBarometerScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_CALIBRATE_BAROMETER)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.calibrateCompassFragment
            label = "CalibrateCompassScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_CALIBRATE_COMPASS)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.calibrateGPSFragment
            label = "CalibrateGPSScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_CALIBRATE_GPS)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.aboutFragment
            label = "AboutScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_ABOUT)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.licenseFragment
            label = "LicenseScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_LICENSES)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.weatherSettingsFragment
            label = "WeatherSettingsScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_WEATHER_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.navigationSettingsFragment
            label = "NavigationSettingsScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_NAVIGATION_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.pathsSettingsFragment
            label = "PathsSettingsScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_PATHS_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.beaconsSettingsFragment
            label = "BeaconSettingsScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_BEACON_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.pluginsSettingsFragment
            label = "PluginsSettingsScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_PLUGINS_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.pluginDetailsFragment
            label = "PluginDetailsScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_PLUGIN_DETAILS)
                .build())
            addArgument("package_id", NavArgument.Builder()
                .setType(NavType.StringType)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.astronomySettingsFragment
            label = "AstronomySettingsScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_ASTRONOMY_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.guideListFragment
            label = "GuideListScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_GUIDE_LIST)
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

        composeNavigator.createDestination().apply {
            id = R.id.guideFragment
            label = "GuideScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_GUIDE)
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

        composeNavigator.createDestination().apply {
            id = R.id.viewQrFragment
            label = "QRShareScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_VIEW_QR)
                .build())
            addArgument("qr_title", NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue("")
                .build())
            addArgument("qr_text", NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue("")
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.levelFragment
            label = "LevelScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_LEVEL)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.beaconDetailsFragment
            label = "BeaconDetailsScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_BEACON_DETAILS)
                .build())
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

        composeNavigator.createDestination().apply {
            id = R.id.packItemListFragment
            label = "PackItemListScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_PACK_ITEM_LIST)
                .build())
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

        composeNavigator.createDestination().apply {
            id = R.id.createItemFragment
            label = "CreateItemScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_CREATE_PACK_ITEM)
                .build())
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

        composeNavigator.createDestination().apply {
            id = R.id.toolConvertFragment
            label = "ConvertScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_CONVERT)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.toolCliffHeightFragment
            label = "ToolCliffHeightScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_CLIFF_HEIGHT)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.toolWhistleFragment
            label = "ToolWhistleScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_WHISTLE)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.fragmentToolSolarPanel
            label = "ScreenToolSolarPanel"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_SOLAR_PANEL)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.waterPurificationFragment
            label = "WaterPurificationScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_WATER_PURIFICATION)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.fragmentToolFlashlight
            label = "ScreenToolFlashlight"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_FLASHLIGHT)
                .build())
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

        composeNavigator.createDestination().apply {
            id = R.id.toolClockFragment
            label = "ClockScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_CLOCK)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.fragmentToolLightning
            label = "ScreenToolLightning"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_LIGHTNING)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.rulerFragment
            label = "RulerScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_RULER)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.fragmentToolBattery
            label = "ScreenToolBattery"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_BATTERY)
                .build())
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

        composeNavigator.createDestination().apply {
            id = R.id.fragmentToolTriangulate
            label = "ScreenToolTriangulate"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_TRIANGULATE)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.fragmentToolBallistics
            label = "ScreenToolBallistics"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_BALLISTICS)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.fragmentToolMetalDetector
            label = "ScreenToolMetalDetector"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_METAL_DETECTOR)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.fragmentToolSurvivalGuideList
            label = "ScreenToolSurvivalGuideList"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_SURVIVAL_GUIDE_LIST)
                .build())
            addArgument("search_query", NavArgument.Builder()
                .setType(NavType.StringType)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.fragmentToolSurvivalGuideReader
            label = "ScreenToolSurvivalGuideReader"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_SURVIVAL_GUIDE_READER)
                .build())
            addArgument("chapter_resource_id", NavArgument.Builder()
                .setType(NavType.IntType)
                .build())
            addArgument("header_index", NavArgument.Builder()
                .setType(NavType.IntType)
                .setDefaultValue(0)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.fragmentToolNotes
            label = "ScreenToolNotes"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_NOTES)
                .build())
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

        composeNavigator.createDestination().apply {
            id = R.id.fragmentToolNotesCreate
            label = "ScreenToolNotesCreate"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_NOTE_EDITOR)
                .build())
            addArgument("edit_note_id", NavArgument.Builder()
                .setType(NavType.LongType)
                .setDefaultValue(0L)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.fragmentToolWhiteNoise
            label = "ScreenToolWhiteNoise"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_WHITE_NOISE)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.fragmentBacktrack
            label = "PathsScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_PATHS)
                .build())
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

        composeNavigator.createDestination().apply {
            id = R.id.cloudFragment
            label = "CloudScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_CLOUDS)
                .build())
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

        composeNavigator.createDestination().apply {
            id = R.id.sensorDetailsFragment
            label = "SensorDetailsScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_SENSOR_DETAILS)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.toolLightFragment
            label = "ToolLightScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_LIGHT_METER)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.tidesFragment
            label = "TidesScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_TIDES)
                .build())
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

        composeNavigator.createDestination().apply {
            id = R.id.fragmentToolScreenFlashlight
            label = "ScreenToolScreenFlashlight"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_SCREEN_FLASHLIGHT)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.fragmentToolPedometer
            label = "ScreenToolSpeedometer"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_PEDOMETER)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.fragmentTurnBackSettings
            label = "TurnBackSettingsScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_TURN_BACK_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.fragmentWaterBoilTimerSettings
            label = "WaterBoilTimerSettingsScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_WATER_BOIL_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.calibrateOdometerFragment
            label = "CalibrateOdometerScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_PEDOMETER_SETTINGS)
                .build())
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

        composeNavigator.createDestination().apply {
            id = R.id.createTideFragment
            label = "CreateTideScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_CREATE_TIDE)
                .build())
            addArgument("edit_tide_id", NavArgument.Builder()
                .setType(NavType.LongType)
                .setDefaultValue(0L)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.tideListFragment
            label = "TideListScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_TIDE_LIST)
                .build())
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

        composeNavigator.createDestination().apply {
            id = R.id.photoMapsFragment
            label = "PhotoMapsScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_PHOTO_MAPS)
                .build())
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

        composeNavigator.createDestination().apply {
            id = R.id.mapListFragment
            label = "MapListScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_OFFLINE_MAP_LIST)
                .build())
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

        composeNavigator.createDestination().apply {
            id = R.id.flashlightSettingsFragment
            label = "FlashlightSettingsScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_FLASHLIGHT_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.thermometerSettingsFragment
            label = "ThermometerSettingsScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_THERMOMETER_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.cellSignalSettingsFragment
            label = "CellSignalSettingsScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_CELL_SIGNAL_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.bubbleLevelSettingsFragment
            label = "BubbleLevelSettingsScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_BUBBLE_LEVEL_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.clockSettingsFragment
            label = "ClockSettingsScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_CLOCK_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.privacySettingsFragment
            label = "PrivacySettingsScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_PRIVACY_SETTINGS)
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

        composeNavigator.createDestination().apply {
            id = R.id.backupSettingsFragment
            label = "BackupSettingsScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_BACKUP_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.powerSettingsFragment
            label = "PowerSettingsScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_POWER_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.unitSettingsFragment
            label = "UnitSettingsScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_UNIT_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.experimentalSettingsFragment
            label = "ExperimentalSettingsScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_EXPERIMENTAL_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.toolRulerSettingsFragment
            label = "RulerSettingsScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_RULER_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.sensorSettingsFragment
            label = "SensorSettingsScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_SENSOR_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.photoMapSettingsFragment
            label = "PhotoMapSettingsScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_PHOTO_MAP_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.temperatureEstimationFragment
            label = "TemperatureEstimationScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_TEMPERATURE_ESTIMATION)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.packListFragment
            label = "PackListScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_PACK_LIST)
                .build())
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

        composeNavigator.createDestination().apply {
            id = R.id.diagnosticsFragment
            label = "DiagnosticsScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_DIAGNOSTICS)
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

        composeNavigator.createDestination().apply {
            id = R.id.pathDetailsFragment
            label = "PathOverviewScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_PATH_OVERVIEW)
                .build())
            addArgument("destination", NavArgument.Builder()
                .setType(NavType.LongType)
                .setDefaultValue(0L)
                .build())
            addArgument("path_id", NavArgument.Builder()
                .setType(NavType.LongType)
                .setDefaultValue(0L)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.tideSettingsFragment
            label = "TideSettingsScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_TIDE_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.clinometerSettingsFragment
            label = "ClinometerSettingsScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_CLINOMETER_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.augmentedRealitySettingsFragment
            label = "AugmentedRealitySettingsScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_AUGMENTED_REALITY_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.cameraSettingsFragment
            label = "CameraSettingsScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_CAMERA_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.errorSettingsFragment
            label = "ErrorSettingsScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_ERROR_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.scanQrFragment
            label = "ScanQRScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_SCAN_QR)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.augmentedRealityFragment
            label = "AugmentedRealityScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_AUGMENTED_REALITY)
                .build())
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

        composeNavigator.createDestination().apply {
            id = R.id.fragmentStrideLengthEstimation
            label = "StrideLengthEstimationScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_STRIDE_LENGTH_ESTIMATION)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.cloudResultsFragment
            label = "CloudResultsScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_CLOUD_RESULTS)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.climateFragment
            label = "ClimateScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_CLIMATE)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.mirrorCameraFragment
            label = "ToolMirrorCameraScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_MIRROR_CAMERA)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.magnifierFragment
            label = "ToolMagnifierScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_MAGNIFIER)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.turnBackFragment
            label = "TurnBackScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_TURN_BACK)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.fragmentLocalMessaging
            label = "ScreenToolLocalMessaging"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_LOCAL_MESSAGING)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.fragmentLocalTalk
            label = "ScreenToolLocalTalk"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_LOCAL_TALK)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.toolsSettingsFragment
            label = "ToolsSettingsScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_TOOLS_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        composeNavigator.createDestination().apply {
            id = R.id.fragmentToolErrorHandler
            label = "ErrorHandlerScreen"
            addArgument(ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(DESTINATION_ERROR_HANDLER)
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
