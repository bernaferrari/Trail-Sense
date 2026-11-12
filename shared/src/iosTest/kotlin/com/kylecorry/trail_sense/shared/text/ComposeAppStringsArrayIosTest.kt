package com.kylecorry.trail_sense.shared.text

import com.kylecorry.trail_sense.settings.ui.SettingsOption
import com.kylecorry.trail_sense.settings.ui.settingsOptions
import com.kylecorry.trail_sense.shared.generated.resources.Res
import com.kylecorry.trail_sense.shared.generated.resources.distance_unit_entries
import com.kylecorry.trail_sense.shared.generated.resources.distance_unit_values
import kotlin.test.Test
import kotlin.test.assertEquals

class ComposeAppStringsArrayIosTest {

    private val strings = ComposeAppStrings()

    @Test
    fun readsAStringArrayCopiedFromTheApp() {
        assertEquals(listOf("meters", "feet_miles"), strings.array(Res.array.distance_unit_values))
    }

    @Test
    fun resolvesArrayItemsThatPointAtStrings() {
        assertEquals(listOf("Meters", "Feet / miles"), strings.array(Res.array.distance_unit_entries))
    }

    @Test
    fun pairsSettingsValuesWithTheirLabels() {
        assertEquals(
            listOf(SettingsOption("meters", "Meters"), SettingsOption("feet_miles", "Feet / miles")),
            settingsOptions(Res.array.distance_unit_entries, Res.array.distance_unit_values, strings)
        )
    }
}
