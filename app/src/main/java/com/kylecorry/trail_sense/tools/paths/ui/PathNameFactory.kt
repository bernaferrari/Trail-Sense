package com.kylecorry.trail_sense.tools.paths.ui

import android.content.Context
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.tools.paths.domain.Path

class PathNameFactory(private val context: Context) {

    private val formatService = FormatService.getInstance(context)

    fun getName(path: Path): String {
        val name = path.name
        val start = path.metadata.duration?.start
        val end = path.metadata.duration?.end
        return if (name != null) {
            name
        } else if (start != null && end != null) {
            formatService.formatTimeSpan(start, end, true)
        } else {
            context.getString(android.R.string.untitled)
        }
    }

}
