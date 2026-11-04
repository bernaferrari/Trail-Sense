package com.kylecorry.trail_sense.tools.tools.ui.widgets

import android.content.Context
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.kylecorry.trail_sense.R

object WidgetIcons {

    @Volatile
    private var launcher: ImageVector? = null

    /** Trail Sense's launcher foreground (which stays a resource for the launcher), as a vector for widget placeholders. */
    fun launcher(context: Context): ImageVector {
        return launcher ?: ImageVector.vectorResource(
            null,
            context.resources,
            R.drawable.ic_launcher_foreground
        ).also { launcher = it }
    }
}
