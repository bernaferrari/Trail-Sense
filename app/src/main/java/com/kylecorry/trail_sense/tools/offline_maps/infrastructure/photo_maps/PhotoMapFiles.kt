package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.photo_maps

import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap

/** Whether the map's source PDF is stored alongside its image. */
fun PhotoMap.hasPdf(): Boolean {
    return getAppService<FileSubsystem>().get(pdfFileName).exists()
}
