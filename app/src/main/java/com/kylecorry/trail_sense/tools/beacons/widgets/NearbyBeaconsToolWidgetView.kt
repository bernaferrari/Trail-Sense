package com.kylecorry.trail_sense.tools.beacons.widgets

import com.kylecorry.trail_sense.shared.icons.trail.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.lifecycle.Lifecycle
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.tools.beacons.subsystem.BeaconsSubsystem
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.widgets.ListItem
import com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetContent
import com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetView
import com.kylecorry.trail_sense.tools.tools.ui.widgets.WidgetClickAction
import com.kylecorry.trail_sense.tools.tools.widgets.WidgetPreferences

class NearbyBeaconsToolWidgetView : ToolWidgetView {
    override fun onInAppEvent(context: Context, event: Lifecycle.Event, triggerUpdate: () -> Unit) {
        // Do nothing
    }

    override suspend fun getContent(
        context: Context,
        prefs: WidgetPreferences?
    ): ToolWidgetContent {
        val subsystem = BeaconsSubsystem.getInstance(context)
        val prefs = UserPreferences(context)
        val beacons = subsystem.getNearbyBeacons().take(10)
        val location = LocationSubsystem.getInstance(context).location
        val formatter = FormatService.getInstance(context)

        val items = beacons.map { beacon ->
            val distance = Distance.meters(location.distanceTo(beacon.coordinate))
                .convertTo(prefs.baseDistanceUnits)
                .toRelativeDistance()
            ListItem(
                title = beacon.name,
                description = formatter.formatDistance(distance, Units.getDecimalPlaces(distance.units)),
                icon = beacon.icon?.icon ?: TrailIcons.Location,
                iconTint = Colors.mostContrastingColor(
                    Color.WHITE,
                    Color.BLACK,
                    beacon.color
                ),
                iconBackground = beacon.color,
                clickAction = WidgetClickAction.StartActivity(
                    Intent(context, MainActivity::class.java).also {
                        it.putExtra("beacon_id", beacon.id)
                    }
                )
            )
        }

        return ToolWidgetContent.List(
            title = context.getString(R.string.nearby_beacons),
            emptyText = context.getString(R.string.no_beacons),
            items = items,
            clickAction = WidgetClickAction.StartActivity(
                NavigationUtils.toolIntent(context, Tools.BEACONS)
            )
        )
    }
}
