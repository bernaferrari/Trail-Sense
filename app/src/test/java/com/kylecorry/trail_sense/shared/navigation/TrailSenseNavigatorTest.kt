package com.kylecorry.trail_sense.shared.navigation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class TrailSenseNavigatorTest {

    @Test
    fun navigatesAndPops() {
        val navigator = TrailSenseNavigator()
        navigator.setStartRoute(Route.Navigation())
        navigator.navigate(Route.BeaconList())
        navigator.navigate(Route.BeaconDetails(3))

        assertEquals(Route.BeaconDetails(3), navigator.currentRoute)
        assertTrue(navigator.popBackStack())
        assertEquals(Route.BeaconList(), navigator.currentRoute)
        assertTrue(navigator.popBackStack())
        assertFalse(navigator.popBackStack())
    }

    @Test
    fun returnsToARouteAlreadyOnTheStack() {
        val navigator = TrailSenseNavigator()
        navigator.setStartRoute(Route.Navigation())
        navigator.navigate(Route.BeaconList())
        navigator.navigate(Route.BeaconDetails(3))
        navigator.navigate(Route.BeaconList())

        assertEquals(listOf(Route.Navigation(), Route.BeaconList()), navigator.backStack.toList())
    }

    @Test
    fun switchingTabsKeepsOnlyTheStartRoute() {
        val navigator = TrailSenseNavigator()
        navigator.setStartRoute(Route.Navigation())
        navigator.navigate(Route.Settings)
        navigator.navigate(Route.UnitSettings)

        navigator.switchTab(Route.Weather)
        assertEquals(listOf(Route.Navigation(), Route.Weather), navigator.backStack.toList())

        navigator.switchTab(Route.Navigation())
        assertEquals(listOf<Route>(Route.Navigation()), navigator.backStack.toList())
    }

    @Test
    fun replaceSwapsTheTopScreen() {
        val navigator = TrailSenseNavigator()
        navigator.setStartRoute(Route.Tools)
        navigator.navigate(Route.OfflineMapList(importUri = "content://maps/1"))

        navigator.replace(Route.OfflineMapList())

        assertEquals(listOf(Route.Tools, Route.OfflineMapList()), navigator.backStack.toList())
    }

    @Test
    fun savesAndRestoresTheStack() {
        val navigator = TrailSenseNavigator()
        navigator.setStartRoute(Route.Tools)
        navigator.navigate(
            Route.PlaceBeacon(initialLocation = RouteLocation(1.0, 2.0, 3f, mapOf("label" to "Camp")))
        )
        navigator.navigate(Route.AugmentedReality(modeId = 2, time = "2026-11-05T10:00-08:00"))

        val restored = TrailSenseNavigator()
        assertTrue(restored.restoreState(navigator.saveState()))
        assertEquals(navigator.backStack.toList(), restored.backStack.toList())
        assertFalse(TrailSenseNavigator().restoreState("not a stack"))
    }

    @Test
    fun encodesRoutesForIntents() {
        val route = Route.PathOverview(42)
        assertEquals(route, TrailSenseNavigator.decode(TrailSenseNavigator.encode(route)))
        assertNull(TrailSenseNavigator.decode("not a route"))
    }
}
