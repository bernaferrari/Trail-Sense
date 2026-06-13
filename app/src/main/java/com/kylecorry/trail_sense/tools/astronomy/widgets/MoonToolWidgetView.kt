package com.kylecorry.trail_sense.tools.astronomy.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomySubsystem
import com.kylecorry.trail_sense.tools.astronomy.ui.MoonPhaseImageMapper
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.widgets.SimpleToolWidgetView
import com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetContent
import com.kylecorry.trail_sense.tools.tools.ui.widgets.WidgetClickAction
import com.kylecorry.trail_sense.tools.tools.widgets.WidgetPreferences

class MoonToolWidgetView : SimpleToolWidgetView() {

    override suspend fun getContent(
        context: Context,
        prefs: WidgetPreferences?
    ): ToolWidgetContent {
        val astronomy = AstronomySubsystem.getInstance(context)
        val formatter = FormatService.getInstance(context)
        val moon = astronomy.moon
        val image = MoonPhaseImageMapper().getPhaseImage(moon.phase)
        val bitmap = Resources.drawable(context, image)?.toBitmap(
            Resources.dp(context, 32f).toInt(),
            Resources.dp(context, 32f).toInt(),
        )
        val rotated = bitmap?.let { rotate(it, moon.tilt) }
        bitmap?.recycle()

        return ToolWidgetContent.Simple(
            title = context.getString(R.string.moon),
            subtitle = formatter.formatMoonPhase(moon.phase),
            iconBitmap = rotated,
            clickAction = WidgetClickAction.StartActivity(
                NavigationUtils.toolIntent(context, Tools.ASTRONOMY)
            )
        )
    }

}
