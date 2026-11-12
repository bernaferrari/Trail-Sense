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
    fun aRouteAlreadyOnTheStackIsOpenedOnTopAndBackReturnsToTheScreenItWasOpenedFrom() {
        val navigator = TrailSenseNavigator()
        navigator.setStartRoute(Route.Tools)
        navigator.navigate(Route.BeaconList())
        navigator.navigate(Route.BeaconDetails(3))
        navigator.navigate(Route.Navigation())
        navigator.navigate(Route.BeaconDetails(3))

        assertEquals(
            listOf(Route.Tools, Route.BeaconList(), Route.Navigation(), Route.BeaconDetails(3)),
            navigator.backStack.toList()
        )
        assertTrue(navigator.popBackStack())
        assertEquals(Route.Navigation(), navigator.currentRoute)
    }

    @Test
    fun openingTheScreenAlreadyOnTopChangesNothing() {
        val navigator = TrailSenseNavigator()
        navigator.setStartRoute(Route.Tools)
        navigator.navigate(Route.BeaconList())
        navigator.navigate(Route.BeaconList())

        assertEquals(listOf(Route.Tools, Route.BeaconList()), navigator.backStack.toList())
    }

    @Test
    fun openingTheFirstScreenFromAboveItKeepsAWayBack() {
        // Settings is the first tab and the tools tab was opened on top of it
        val navigator = TrailSenseNavigator()
        navigator.setStartRoute(Route.Settings)
        navigator.navigate(Route.Tools)
        navigator.navigate(Route.Settings)

        assertEquals(listOf(Route.Tools, Route.Settings), navigator.backStack.toList())
        assertTrue(navigator.popBackStack())
        assertEquals(Route.Tools, navigator.currentRoute)
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
    fun replacingWithARouteFurtherDownTheStackKeepsItOnce() {
        // The map list is open and a shared map file opens the importing list on top of it
        val navigator = TrailSenseNavigator()
        navigator.setStartRoute(Route.Map())
        navigator.navigate(Route.OfflineMapList())
        navigator.navigate(Route.OfflineMapList(importUri = "content://maps/1"))

        navigator.replace(Route.OfflineMapList())

        assertEquals(listOf(Route.Map(), Route.OfflineMapList()), navigator.backStack.toList())
        assertTrue(navigator.popBackStack())
        assertEquals(Route.Map(), navigator.currentRoute)
    }

    @Test
    fun replacingTheTopWithItselfChangesNothing() {
        val navigator = TrailSenseNavigator()
        navigator.setStartRoute(Route.Tools)
        navigator.navigate(Route.Settings)

        navigator.replace(Route.Settings)

        assertEquals(listOf(Route.Tools, Route.Settings), navigator.backStack.toList())
    }

    @Test
    fun replacingOnAnEmptyStackStartsIt() {
        val navigator = TrailSenseNavigator()
        navigator.replace(Route.Tools)
        assertEquals(listOf<Route>(Route.Tools), navigator.backStack.toList())
    }

    @Test
    fun aRestoredStackHoldsEachRouteOnce() {
        val navigator = TrailSenseNavigator()
        val saved = "[" + listOf(Route.Tools, Route.Settings, Route.Tools, Route.UnitSettings)
            .joinToString(",") { TrailSenseNavigator.encode(it) } + "]"

        assertTrue(navigator.restoreState(saved))

        assertEquals(listOf(Route.Settings, Route.Tools, Route.UnitSettings), navigator.backStack.toList())
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
