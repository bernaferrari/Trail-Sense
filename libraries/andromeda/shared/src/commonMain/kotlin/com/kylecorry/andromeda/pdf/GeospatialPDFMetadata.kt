package com.kylecorry.andromeda.pdf

import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.units.Coordinate

public data class GeospatialPDFMetadata(
    val points: List<Pair<PixelCoordinate, Coordinate>>,
    val projection: ProjectedCoordinateSystem?
)
