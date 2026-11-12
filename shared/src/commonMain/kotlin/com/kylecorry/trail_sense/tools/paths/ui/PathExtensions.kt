package com.kylecorry.trail_sense.tools.paths.ui

import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.text.AppStrings

import com.kylecorry.trail_sense.shared.colors.ColorInts
import com.kylecorry.trail_sense.tools.beacons.domain.Beacon
import com.kylecorry.trail_sense.tools.navigation.ui.IMappablePath
import com.kylecorry.trail_sense.tools.navigation.ui.MappableLocation
import com.kylecorry.trail_sense.tools.navigation.ui.MappablePath
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle
import com.kylecorry.trail_sense.tools.paths.domain.Path
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint
import com.kylecorry.trail_sense.tools.paths.domain.PathPointColoringStyle
import com.kylecorry.trail_sense.tools.paths.domain.factories.AltitudePointDisplayFactory
import com.kylecorry.trail_sense.tools.paths.domain.factories.CellSignalPointDisplayFactory
import com.kylecorry.trail_sense.tools.paths.domain.factories.IPointDisplayFactory
import com.kylecorry.trail_sense.tools.paths.domain.factories.NonePointDisplayFactory
import com.kylecorry.trail_sense.tools.paths.domain.factories.SlopePointDisplayFactory
import com.kylecorry.trail_sense.tools.paths.domain.factories.TimePointDisplayFactory
import com.kylecorry.trail_sense.tools.paths.domain.waypointcolors.NoDrawPointColoringStrategy
import com.kylecorry.trail_sense.tools.paths.domain.waypointcolors.SelectedPointDecorator

fun List<PathPoint>.asMappable(path: Path): IMappablePath {
    return MappablePath(
        path.id,
        toMappableLocations(),
        path.style.color,
        path.style.line,
        path.name
    )
}

fun List<PathPoint>.toMappableLocations(
    coloringStyle: PathPointColoringStyle = PathPointColoringStyle.None
): List<MappableLocation> {
    val colorFactory = getPointFactory(coloringStyle)
    val strategy = colorFactory.createColoringStrategy(this)
    return this.map { point ->
        MappableLocation(
            point.id,
            point.coordinate,
            strategy.getColor(point) ?: ColorInts.Transparent,
            null,
            point.elevation
        )
    }
}

fun List<PathPoint>.asBeacons(
    coloringStyle: PathPointColoringStyle = PathPointColoringStyle.None,
    selected: Long? = null
): List<Beacon> {
    val colorFactory = getPointFactory(coloringStyle)
    val baseStrategy = colorFactory.createColoringStrategy(this)
    val strategy = if (selected != null) {
        SelectedPointDecorator(
            selected,
            baseStrategy,
            NoDrawPointColoringStrategy()
        )
    } else {
        baseStrategy
    }
    return this.map { point ->
        val color = strategy.getColor(point) ?: ColorInts.Transparent
        Beacon(
            point.id,
            "",
            point.coordinate,
            elevation = point.elevation,
            color = if (point.id == selected && color == ColorInts.Transparent) {
                ColorInts.White
            } else {
                color
            },
        )
    }
}

fun getLineStyleNames(): Map<LineStyle, String> {
    return mapOf(
        LineStyle.Solid to getAppService<AppStrings>().get(Res.string.solid),
        LineStyle.Dotted to getAppService<AppStrings>().get(Res.string.dotted),
        LineStyle.Arrow to getAppService<AppStrings>().get(Res.string.arrow),
        LineStyle.Dashed to getAppService<AppStrings>().get(Res.string.dashed),
        LineStyle.Square to getAppService<AppStrings>().get(Res.string.square),
        LineStyle.Diamond to getAppService<AppStrings>().get(Res.string.diamond),
        LineStyle.Cross to getAppService<AppStrings>().get(Res.string.cross)
    )
}

fun getPointStyleNames(): Map<PathPointColoringStyle, String> {
    return mapOf(
        PathPointColoringStyle.None to getAppService<AppStrings>().get(Res.string.none),
        PathPointColoringStyle.CellSignal to getAppService<AppStrings>().get(Res.string.cell_signal),
        PathPointColoringStyle.Altitude to getAppService<AppStrings>().get(Res.string.elevation),
        PathPointColoringStyle.Time to getAppService<AppStrings>().get(Res.string.time),
        PathPointColoringStyle.Slope to getAppService<AppStrings>().get(Res.string.path_slope)
    )
}

// TODO: Extract this and add solid style
fun getPointFactory(
    pointColoringStyle: PathPointColoringStyle
): IPointDisplayFactory {
    return when (pointColoringStyle) {
        PathPointColoringStyle.None -> NonePointDisplayFactory()
        PathPointColoringStyle.CellSignal -> CellSignalPointDisplayFactory()
        PathPointColoringStyle.Altitude -> AltitudePointDisplayFactory()
        PathPointColoringStyle.Time -> TimePointDisplayFactory()
        PathPointColoringStyle.Slope -> SlopePointDisplayFactory()
    }
}

