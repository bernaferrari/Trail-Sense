package com.kylecorry.trail_sense.shared.io

import com.kylecorry.andromeda.gpx.GPXData
import com.kylecorry.andromeda.gpx.GPXParser
import com.kylecorry.trail_sense.shared.sharing.pickFile
import com.kylecorry.trail_sense.shared.sharing.saveExportFile

class SharedGpxIOService : IOService<GPXData> {
    override suspend fun export(data: GPXData, filename: String): Boolean {
        val gpxString = GPXParser.toGPX(data, "Trail Sense")
        val suggestedName = filename.removeSuffix(".gpx").ifBlank { "trail-sense" }
        return saveExportFile(
            suggestedName = suggestedName,
            extension = "gpx",
            bytes = gpxString.encodeToByteArray()
        )
    }

    override suspend fun import(): GPXData? {
        val file = pickFile(listOf("gpx", "xml")) ?: return null
        return runCatching { GPXParser.parse(file.decodeToString()) }.getOrNull()
    }
}
