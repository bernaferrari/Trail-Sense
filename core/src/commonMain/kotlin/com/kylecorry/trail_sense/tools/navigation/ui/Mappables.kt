package com.kylecorry.trail_sense.tools.navigation.ui

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.data.Identifiable
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconIcon
import com.kylecorry.trail_sense.tools.paths.domain.LineStyle

interface IMappableBearing {
    val bearing: Float
    val color: Int
}

data class MappableBearing(override val bearing: Float, override val color: Int) :
    IMappableBearing

interface IMappableLocation : Identifiable {
    val coordinate: Coordinate
    val color: Int
    val icon: BeaconIcon?
    val elevation: Float?
}

data class MappableLocation(
    override val id: Long,
    override val coordinate: Coordinate,
    override val color: Int,
    override val icon: BeaconIcon?,
    override val elevation: Float? = null
) : IMappableLocation

interface IMappablePath : Identifiable {
    val points: List<IMappableLocation>
    val color: Int
    val style: LineStyle
    val name: String?
    val thicknessScale: Float
}

data class MappablePath(
    override val id: Long,
    override val points: List<IMappableLocation>,
    override val color: Int,
    override val style: LineStyle,
    override val name: String? = null,
    override val thicknessScale: Float = 1f
) : IMappablePath
