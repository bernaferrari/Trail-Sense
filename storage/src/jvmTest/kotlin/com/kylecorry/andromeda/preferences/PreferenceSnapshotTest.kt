package com.kylecorry.andromeda.preferences

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class PreferenceSnapshotTest {

    @Test
    fun getAllReportsTypedValues() {
        val prefs = Preferences(MapSettings())
        prefs.putString("s", "text")
        prefs.putBoolean("b", true)
        prefs.putInt("i", 3)
        prefs.putLong("l", 4L)
        prefs.putFloat("f", 1.5f)

        val all = prefs.getAll().associateBy { it.key }

        assertEquals(Preference("s", PreferenceType.String, "text"), all["s"])
        assertEquals(Preference("b", PreferenceType.Boolean, true), all["b"])
        assertEquals(Preference("i", PreferenceType.Int, 3), all["i"])
        assertEquals(Preference("l", PreferenceType.Long, 4L), all["l"])
        assertEquals(Preference("f", PreferenceType.Float, 1.5f), all["f"])
    }

    @Test
    fun putAllRestoresSnapshotAndClearsOthers() {
        val prefs = Preferences(MapSettings())
        prefs.putInt("keep", 1)
        val snapshot = prefs.getAll()
        prefs.putInt("keep", 2)
        prefs.putString("extra", "x")

        prefs.putAll(snapshot, clearOthers = true)

        assertEquals(1, prefs.getInt("keep"))
        assertEquals(setOf("keep"), prefs.keys)
    }
}
