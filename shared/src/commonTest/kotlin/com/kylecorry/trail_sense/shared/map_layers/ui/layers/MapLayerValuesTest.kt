package com.kylecorry.trail_sense.shared.map_layers.ui.layers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MapLayerValuesTest {

    @Test
    fun typedGettersFallBackToTheirDefaults() {
        val values = MapLayerValues.of("opacity" to 50, "labels" to true, "time" to 10L, "name" to "a")
        assertEquals(50, values.getInt("opacity", 100))
        assertEquals(100, values.getInt("missing", 100))
        assertTrue(values.getBoolean("labels"))
        assertEquals(10L, values.getLong("time"))
        assertEquals("a", values.getString("name"))
        assertNull(values.getString("opacity"))
        // A value of another type is treated as missing, as Bundle did
        assertEquals(0L, values.getLong("opacity"))
        assertEquals(1f, values.getFloat("opacity", 1f))
    }

    @Test
    fun nullsAreNotStored() {
        val values = MapLayerValues.of("a" to null, "b" to 1)
        assertFalse(values.containsKey("a"))
        assertFalse(values.with("b", null).containsKey("b"))
    }

    @Test
    fun defaultsNeverReplaceSetValues() {
        val values = MapLayerValues.of("opacity" to 50).withDefaults(mapOf("opacity" to 100, "enabled" to true))
        assertEquals(50, values.getInt("opacity"))
        assertTrue(values.getBoolean("enabled"))
    }

    @Test
    fun preferencesNestInsideParameters() {
        val preferences = MapLayerValues.of("smooth" to true)
        val params = MapLayerValues.of(MapLayerParams.PARAM_PREFERENCES to preferences, MapLayerParams.PARAM_TIME to 5L)
        assertEquals(preferences, params.getPreferences())
        assertEquals(MapLayerValues.Empty, MapLayerValues.Empty.getPreferences())
        assertEquals(MapLayerValues.of("smooth" to true), preferences)
    }
}
