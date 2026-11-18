package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.vector_maps.attribution

import com.kylecorry.trail_sense.tools.offline_maps.domain.vector_maps.VectorMapFileType

class OfflineMapAttributionExtractorFactory {
    fun getAttributionExtractor(type: VectorMapFileType): OfflineMapAttributionExtractor {
        return when (type) {
            VectorMapFileType.GeoJson -> error("GeoJSON import uses the shared map reader")
            VectorMapFileType.Mapsforge -> MapsforgeOfflineMapAttributionExtractor()
        }
    }
}
