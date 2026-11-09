package com.kylecorry.sol.units

import kotlin.test.Test
import kotlin.test.assertEquals
import com.kylecorry.sol.tests.arg

internal class VolumeTest {
    @Test
    fun convertTo() {
        provideConversions().forEach { args -> convertTo(args[0].arg<Float>(), args[1].arg<VolumeUnits>(), args[2].arg<Float>(), args[3].arg<VolumeUnits>()) }
    }

    private fun convertTo(volume: Float, units: VolumeUnits, expectedVolume: Float, expectedUnits: VolumeUnits) {
        val converted = Volume.from(volume, units).convertTo(expectedUnits)
        assertEquals(expectedUnits, converted.units)
        assertEquals(expectedVolume, converted.value, 0.0001f)
    }

    companion object {
        fun provideConversions(): List<List<Any?>> {
            return listOf(
                listOf(
                    2f, VolumeUnits.Liters,
                    0.528344f, VolumeUnits.USGallons
                ),
                listOf(
                    3f, VolumeUnits.USGallons,
                    11.35623f, VolumeUnits.Liters
                ),
                listOf(
                    3f, VolumeUnits.ImperialGallons,
                    461.16525f, VolumeUnits.USOunces
                ),
                listOf(
                    4f, VolumeUnits.Milliliter,
                    0.014078f, VolumeUnits.ImperialCups
                ),
                listOf(4f, VolumeUnits.USPints, 2f, VolumeUnits.USQuarts),
                listOf(
                    4f, VolumeUnits.USCups,
                    3.3307f, VolumeUnits.ImperialCups
                ),
                listOf(
                    4f, VolumeUnits.ImperialOunces,
                    0.2f, VolumeUnits.ImperialPints
                ),
                listOf(
                    1f, VolumeUnits.ImperialQuarts,
                    1136.52f, VolumeUnits.Milliliter
                ),
                listOf(
                    1f, VolumeUnits.USTablespoons,
                    3f, VolumeUnits.USTeaspoons
                ),
                listOf(
                    1f, VolumeUnits.ImperialTablespoons,
                    3f, VolumeUnits.ImperialTeaspoons
                )
            )
        }

    }
}