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
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment as ComposeDestinationFragment

object TrailSenseNavGraph {

    private const val COMPOSE_DESTINATION_FRAGMENT_CLASS =
        "com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment"

    fun create(navController: NavController, startDestination: Int): NavGraph {
        val provider = navController.navigatorProvider
        val graph = NavGraph(provider.getNavigator(NavGraphNavigator::class.java))
        graph.setStartDestination(startDestination)
        val fragmentNavigator = provider.getNavigator(FragmentNavigator::class.java)

        fragmentNavigator.createDestination().apply {
            id = R.id.action_navigation
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "NavigationScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_NAVIGATION)
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

        fragmentNavigator.createDestination().apply {
            id = R.id.action_astronomy
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "AstronomyScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_ASTRONOMY)
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

        fragmentNavigator.createDestination().apply {
            id = R.id.action_experimental_tools
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "ToolsFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_TOOLS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.action_weather
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "WeatherScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_WEATHER)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.mapFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "MapScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_MAP)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.mapSettingsFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "MapSettingsFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_MAP_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.offlineMapViewFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "ViewVectorMapScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_VECTOR_MAP_VIEW)
                .build())
            addArgument("offline_map_file_id", NavArgument.Builder()
                .setType(NavType.LongType)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.action_settings
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "SettingsScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_SETTINGS)
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
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "ExperimentationFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_EXPERIMENTATION)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fieldGuideFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "FieldGuideScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_FIELD_GUIDE)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fieldGuidePageFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "FieldGuidePageScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_FIELD_GUIDE_PAGE)
                .build())
            addArgument("page_id", NavArgument.Builder()
                .setType(NavType.LongType)
                .setDefaultValue(0L)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.createFieldGuideSightingFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "CreateFieldGuideSightingScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_CREATE_FIELD_GUIDE_SIGHTING)
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

        fragmentNavigator.createDestination().apply {
            id = R.id.sightingListFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "SightingListScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_SIGHTING_LIST)
                .build())
            addArgument("page_id", NavArgument.Builder()
                .setType(NavType.LongType)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.createFieldGuidePageFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "CreateFieldGuidePageScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_CREATE_FIELD_GUIDE_PAGE)
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

        fragmentNavigator.createDestination().apply {
            id = R.id.signalFinderFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "SignalFinderScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_SIGNAL_FINDER)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.toolDeclinationFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "DeclinationScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_DECLINATION)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.beacon_list
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "BeaconListScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_BEACON_LIST)
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

        fragmentNavigator.createDestination().apply {
            id = R.id.placeBeaconFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "PlaceBeaconScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_PLACE_BEACON)
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

        fragmentNavigator.createDestination().apply {
            id = R.id.clinometerFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "ClinometerScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_CLINOMETER)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.calibrateAltimeterFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "CalibrateAltimeterScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_CALIBRATE_ALTIMETER)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.calibrateBarometerFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "CalibrateBarometerScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_CALIBRATE_BAROMETER)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.calibrateCompassFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "CalibrateCompassScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_CALIBRATE_COMPASS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.calibrateGPSFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "CalibrateGPSScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_CALIBRATE_GPS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.licenseFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "LicenseFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_LICENSES)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.weatherSettingsFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "WeatherSettingsScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_WEATHER_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.navigationSettingsFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "NavigationSettingsScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_NAVIGATION_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.pathsSettingsFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "PathsSettingsScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_PATHS_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.beaconsSettingsFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "BeaconSettingsFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_BEACON_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.pluginsSettingsFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "PluginsSettingsFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_PLUGINS_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.pluginDetailsFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "PluginDetailsFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_PLUGIN_DETAILS)
                .build())
            addArgument("package_id", NavArgument.Builder()
                .setType(NavType.StringType)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.astronomySettingsFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "AstronomySettingsScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_ASTRONOMY_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.guideListFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "GuideListFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_GUIDE_LIST)
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
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "GuideFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_GUIDE)
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
            id = R.id.viewQrFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "QRShareScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_VIEW_QR)
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

        fragmentNavigator.createDestination().apply {
            id = R.id.levelFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "LevelFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_LEVEL)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.beaconDetailsFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "BeaconDetailsScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_BEACON_DETAILS)
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

        fragmentNavigator.createDestination().apply {
            id = R.id.packItemListFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "PackItemListScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_PACK_ITEM_LIST)
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

        fragmentNavigator.createDestination().apply {
            id = R.id.createItemFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "CreateItemScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_CREATE_PACK_ITEM)
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

        fragmentNavigator.createDestination().apply {
            id = R.id.toolConvertFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "ConvertFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_CONVERT)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.toolCliffHeightFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "ToolCliffHeightFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_CLIFF_HEIGHT)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.toolWhistleFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "ToolWhistleFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_WHISTLE)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentToolSolarPanel
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "FragmentToolSolarPanel"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_SOLAR_PANEL)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.waterPurificationFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "WaterPurificationFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_WATER_PURIFICATION)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentToolFlashlight
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "FragmentToolFlashlight"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_FLASHLIGHT)
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

        fragmentNavigator.createDestination().apply {
            id = R.id.toolClockFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "ClockScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_CLOCK)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentToolLightning
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "FragmentToolLightning"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_LIGHTNING)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.rulerFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "RulerScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_RULER)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentToolBattery
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "FragmentToolBattery"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_BATTERY)
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

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentToolTriangulate
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "FragmentToolTriangulate"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_TRIANGULATE)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentToolBallistics
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "FragmentToolBallistics"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_BALLISTICS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentToolPermits
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "FragmentToolPermits"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_PERMITS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentToolMetalDetector
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "FragmentToolMetalDetector"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_METAL_DETECTOR)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentToolSurvivalGuideList
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "FragmentToolSurvivalGuideList"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_SURVIVAL_GUIDE_LIST)
                .build())
            addArgument("search_query", NavArgument.Builder()
                .setType(NavType.StringType)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentToolSurvivalGuideReader
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "FragmentToolSurvivalGuideReader"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_SURVIVAL_GUIDE_READER)
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

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentToolNotes
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "FragmentToolNotes"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_NOTES)
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

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentToolNotesCreate
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "FragmentToolNotesCreate"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_NOTE_EDITOR)
                .build())
            addArgument("edit_note_id", NavArgument.Builder()
                .setType(NavType.LongType)
                .setDefaultValue(0L)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentToolWhiteNoise
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "FragmentToolWhiteNoise"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_WHITE_NOISE)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentBacktrack
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "PathsScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_PATHS)
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

        fragmentNavigator.createDestination().apply {
            id = R.id.cloudFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "CloudScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_CLOUDS)
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

        fragmentNavigator.createDestination().apply {
            id = R.id.sensorDetailsFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "SensorDetailsScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_SENSOR_DETAILS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.toolLightFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "ToolLightFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_LIGHT_METER)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.tidesFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "TidesScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_TIDES)
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

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentToolScreenFlashlight
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "FragmentToolScreenFlashlight"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_SCREEN_FLASHLIGHT)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentToolPedometer
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "FragmentToolSpeedometer"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_PEDOMETER)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentTurnBackSettings
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "TurnBackSettingsFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_TURN_BACK_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentWaterBoilTimerSettings
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "WaterBoilTimerSettingsFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_WATER_BOIL_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.calibrateOdometerFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "CalibrateOdometerFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_PEDOMETER_SETTINGS)
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

        fragmentNavigator.createDestination().apply {
            id = R.id.createTideFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "CreateTideScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_CREATE_TIDE)
                .build())
            addArgument("edit_tide_id", NavArgument.Builder()
                .setType(NavType.LongType)
                .setDefaultValue(0L)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.tideListFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "TideListScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_TIDE_LIST)
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

        fragmentNavigator.createDestination().apply {
            id = R.id.photoMapsFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "PhotoMapsScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_PHOTO_MAPS)
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

        fragmentNavigator.createDestination().apply {
            id = R.id.mapListFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "MapListScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_OFFLINE_MAP_LIST)
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

        fragmentNavigator.createDestination().apply {
            id = R.id.flashlightSettingsFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "FlashlightSettingsFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_FLASHLIGHT_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.thermometerSettingsFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "ThermometerSettingsScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_THERMOMETER_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.cellSignalSettingsFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "CellSignalSettingsFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_CELL_SIGNAL_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.bubbleLevelSettingsFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "BubbleLevelSettingsFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_BUBBLE_LEVEL_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.clockSettingsFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "ClockSettingsFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_CLOCK_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.privacySettingsFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "PrivacySettingsFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_PRIVACY_SETTINGS)
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
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "ThemeSettingsFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_THEME_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.backupSettingsFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "BackupSettingsFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_BACKUP_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.powerSettingsFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "PowerSettingsFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_POWER_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.unitSettingsFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "UnitSettingsFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_UNIT_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.experimentalSettingsFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "ExperimentalSettingsFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_EXPERIMENTAL_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.toolRulerSettingsFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "RulerSettingsFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_RULER_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.sensorSettingsFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "SensorSettingsFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_SENSOR_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.photoMapSettingsFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "PhotoMapSettingsFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_PHOTO_MAP_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.temperatureEstimationFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "TemperatureEstimationScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_TEMPERATURE_ESTIMATION)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.packListFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "PackListScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_PACK_LIST)
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

        fragmentNavigator.createDestination().apply {
            id = R.id.diagnosticsFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "DiagnosticsFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_DIAGNOSTICS)
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
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "PathOverviewScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_PATH_OVERVIEW)
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

        fragmentNavigator.createDestination().apply {
            id = R.id.tideSettingsFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "TideSettingsFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_TIDE_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.clinometerSettingsFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "ClinometerSettingsFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_CLINOMETER_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.augmentedRealitySettingsFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "AugmentedRealitySettingsFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_AUGMENTED_REALITY_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.cameraSettingsFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "CameraSettingsFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_CAMERA_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.errorSettingsFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "ErrorSettingsFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_ERROR_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.scanQrFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "ScanQRScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_SCAN_QR)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.augmentedRealityFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "AugmentedRealityScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_AUGMENTED_REALITY)
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

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentStrideLengthEstimation
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "StrideLengthEstimationScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_STRIDE_LENGTH_ESTIMATION)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.cloudResultsFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "CloudResultsScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_CLOUD_RESULTS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.climateFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "ClimateScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_CLIMATE)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.mirrorCameraFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "ToolMirrorCameraFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_MIRROR_CAMERA)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.magnifierFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "ToolMagnifierFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_MAGNIFIER)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.turnBackFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "TurnBackScreen"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_TURN_BACK)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentLocalMessaging
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "FragmentToolLocalMessaging"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_LOCAL_MESSAGING)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentLocalTalk
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "FragmentToolLocalTalk"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_LOCAL_TALK)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.toolsSettingsFragment
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "ToolsSettingsFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_TOOLS_SETTINGS)
                .build())
            graph.addDestination(this)
        }

        fragmentNavigator.createDestination().apply {
            id = R.id.fragmentToolErrorHandler
            setClassName(COMPOSE_DESTINATION_FRAGMENT_CLASS)
            label = "ErrorHandlerFragment"
            addArgument(ComposeDestinationFragment.ARG_DESTINATION, NavArgument.Builder()
                .setType(NavType.StringType)
                .setDefaultValue(ComposeDestinationFragment.DESTINATION_ERROR_HANDLER)
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
