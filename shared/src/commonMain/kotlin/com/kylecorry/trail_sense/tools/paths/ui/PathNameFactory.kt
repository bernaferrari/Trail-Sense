package com.kylecorry.trail_sense.tools.paths.ui

import com.kylecorry.trail_sense.shared.text.SystemString
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.text.AppStrings

import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.tools.paths.domain.Path

class PathNameFactory() {

    private val formatService = FormatService.getInstance()

    fun getName(path: Path): String {
        val name = path.name
        val start = path.metadata.duration?.start
        val end = path.metadata.duration?.end
        return if (name != null) {
            name
        } else if (start != null && end != null) {
            formatService.formatTimeSpan(start, end, true)
        } else {
            getAppService<AppStrings>().system(SystemString.Untitled)
        }
    }

}
