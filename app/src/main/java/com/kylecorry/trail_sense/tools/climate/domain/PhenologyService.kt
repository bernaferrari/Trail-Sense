package com.kylecorry.trail_sense.tools.climate.domain

import kotlinx.datetime.daysUntil

import kotlinx.datetime.toJavaLocalDate

import kotlin.time.Duration.Companion.hours

import com.kylecorry.trail_sense.shared.time.interop.*

import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.sol.math.Range
import com.kylecorry.sol.math.mergeIntersecting
import com.kylecorry.sol.science.ecology.Ecology
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Temperature
import com.kylecorry.trail_sense.tools.weather.infrastructure.subsystem.IWeatherSubsystem
import java.time.LocalDate

class PhenologyService(private val weather: IWeatherSubsystem) {

    suspend fun getYearlyActiveDays(
        year: Int,
        location: Coordinate? = null,
        elevation: Distance? = null,
        calibrated: Boolean = true
    ): Map<BiologicalActivityType, List<Range<LocalDate>>> = onDefault {
        val climate = weather.getClimateClassification(location, elevation, calibrated)
        val temperatures = weather.getTemperatureRanges(year, location, elevation, calibrated)
        val activeDays = mutableMapOf<SpeciesPhenology, List<Range<LocalDate>>>()
        val temperatureMap = temperatures.associateBy { it.first }

        for (species in SpeciesPhenology.ENTRIES) {
            if (climate.code in species.excludedClimates) {
                activeDays[species] = listOf()
                continue
            }

            val events = Ecology.getLifecycleEventDates(
                species.events,
                Range(kotlinx.datetime.LocalDate(year, 1, 1), kotlinx.datetime.LocalDate(year, 12, 31)),
                // TODO: This isn't needed right now, but it should be an interpolated version of astronomy get daylight length (for performance)
                { 12.hours }
            ) { date ->
                val newDate = date.toJavaLocalDate().withYear(year)
                temperatureMap[newDate]?.second ?: Range(Temperature.zero, Temperature.zero)
            }.toMutableList()

            if (events.none { it.second.name == EVENT_ACTIVE_START }){
                // Species is never active
                continue
            }

            activeDays[species] =
                Ecology.getActivePeriodsForYear(year, events, EVENT_ACTIVE_START, EVENT_ACTIVE_END)
                    .filter { it.start.daysUntil(it.end) > 1 }
                    .map { Range(it.start.toJavaLocalDate(), it.end.toJavaLocalDate()) }
        }

        // Merge the active periods for similar biological activity
        val biologicalActivity = mutableMapOf<BiologicalActivityType, List<Range<LocalDate>>>()
        for (type in BiologicalActivityType.entries) {
            val matches = activeDays.entries.filter { it.key.type == type }
            val allDateRanges = matches.flatMap { it.value }.sortedBy { it.start }
            biologicalActivity[type] = allDateRanges.mergeIntersecting()
        }

        biologicalActivity
    }

    companion object {
        const val EVENT_ACTIVE_START = "ACTIVE_START"
        const val EVENT_ACTIVE_END = "ACTIVE_END"
    }

}
