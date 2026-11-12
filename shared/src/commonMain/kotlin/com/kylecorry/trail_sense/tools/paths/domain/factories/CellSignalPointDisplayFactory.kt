package com.kylecorry.trail_sense.tools.paths.domain.factories

import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.trail_sense.shared.colors.QualityColors
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.scales.DiscreteColorScale
import com.kylecorry.trail_sense.shared.scales.IColorScale
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.waypointcolors.CellSignalPointColoringStrategy
import com.kylecorry.trail_sense.tools.paths.domain.waypointcolors.IPointColoringStrategy

class CellSignalPointDisplayFactory : IPointDisplayFactory {
    override fun createColoringStrategy(path: List<PathPoint>): IPointColoringStrategy {
        return CellSignalPointColoringStrategy(createColorScale(path))
    }

    override fun createColorScale(path: List<PathPoint>): IColorScale {
        return DiscreteColorScale(
            listOf(
                Quality.Poor,
                Quality.Moderate,
                Quality.Good
            ).map { QualityColors.of(it) }
        )
    }

    override fun createLabelMap(path: List<PathPoint>): Map<Float, String> {
        val formatService = FormatService.getInstance()
        return mapOf(
            0.167f to formatService.formatQuality(Quality.Poor),
            0.5f to formatService.formatQuality(Quality.Moderate),
            0.833f to formatService.formatQuality(Quality.Good),
        )
    }
}
