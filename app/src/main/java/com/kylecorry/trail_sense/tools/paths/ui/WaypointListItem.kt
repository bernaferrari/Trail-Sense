package com.kylecorry.trail_sense.tools.paths.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.andromeda.signal.CellNetwork
import com.kylecorry.andromeda.views.badge.Badge
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.CellSignalUtils
import com.kylecorry.trail_sense.tools.paths.domain.PathPoint

class WaypointListItem(
    private val context: Context,
    private val formatService: FormatService,
    private val createBeacon: (waypoint: PathPoint) -> Unit,
    private val delete: (waypoint: PathPoint) -> Unit,
    private val navigate: (waypoint: PathPoint) -> Unit,
    private val view: (waypoint: PathPoint) -> Unit
) {

    fun createView(): WaypointListItemView {
        return WaypointListItemView(context)
    }

    fun display(itemView: WaypointListItemView, item: PathPoint) {
        if (item.elevation != null) {
            itemView.waypointCoordinates.isVisible = true
            val elevation = Distance.meters(item.elevation)
                .convertTo(UserPreferences(context).baseDistanceUnits)

            val slope =
                context.getString(R.string.slope_amount, formatService.formatPercentage(item.slope))

            @SuppressLint("SetTextI18n")
            itemView.waypointCoordinates.text =
                formatService.formatDistance(
                    elevation,
                    Units.getDecimalPlaces(elevation.units),
                    false
                ) + "    $slope"
        } else {
            itemView.waypointCoordinates.isVisible = false
        }
        if (item.time != null) {
            val date = item.time.toZonedDateTime()
            val time = date.toLocalTime()
            itemView.waypointTime.text = context.getString(
                R.string.waypoint_time_format,
                formatService.formatRelativeDate(date.toLocalDate()),
                formatService.formatTime(time, false)
            )
        } else {
            itemView.waypointTime.text = context.getString(R.string.waypoint)
        }

        itemView.signalStrength.setStatusText(
            formatService.formatCellNetwork(
                CellNetwork.values().firstOrNull { it.id == item.cellSignal?.network?.id }
            )
        )
        itemView.signalStrength.setImageResource(
            CellSignalUtils.getCellQualityImage(
                item.cellSignal?.quality ?: Quality.Unknown
            )
        )
        itemView.signalStrength.setForegroundTint(Color.BLACK)
        itemView.signalStrength.setBackgroundTint(
            CustomUiUtils.getQualityColor(
                item.cellSignal?.quality ?: Quality.Unknown
            )
        )
        itemView.signalStrength.isVisible = item.cellSignal != null

        itemView.waypointMenuBtn.setOnClickListener {
            Pickers.menu(it, R.menu.waypoint_item_menu) {
                when (it) {
                    R.id.action_waypoint_create_beacon -> {
                        createBeacon(item)
                    }

                    R.id.action_waypoint_delete -> {
                        delete(item)
                    }

                    R.id.action_waypoint_navigate -> {
                        navigate(item)
                    }
                }
                true
            }
        }

        itemView.setOnClickListener {
            view(item)
        }
    }
}

class WaypointListItemView(context: Context) : ConstraintLayout(context) {

    val waypointTime: TextView
    val signalStrength: Badge
    val waypointCoordinates: TextView
    val waypointMenuBtn: ImageButton

    init {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setPadding(
            0,
            Resources.dp(context, 8f).toInt(),
            0,
            Resources.dp(context, 8f).toInt()
        )

        waypointTime = TextView(context).apply {
            id = R.id.waypoint_time
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
        }
        addView(
            waypointTime,
            LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = Resources.dp(context, 16f).toInt()
                marginEnd = Resources.dp(context, 16f).toInt()
                endToStart = R.id.waypoint_menu_btn
                startToStart = LayoutParams.PARENT_ID
                topToTop = LayoutParams.PARENT_ID
            }
        )

        signalStrength = Badge(context, null).apply {
            id = R.id.signal_strength
        }
        addView(
            signalStrength,
            LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = Resources.dp(context, 4f).toInt()
                bottomToBottom = LayoutParams.PARENT_ID
                startToStart = R.id.waypoint_time
                topToBottom = R.id.waypoint_time
            }
        )

        waypointCoordinates = TextView(context).apply {
            id = R.id.waypoint_coordinates
            compoundDrawablePadding = Resources.dp(context, 8f).toInt()
            gravity = Gravity.CENTER_VERTICAL
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall)
            setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_altitude, 0, 0, 0)
            TextViewCompat.setCompoundDrawableTintList(
                this,
                ColorStateList.valueOf(Resources.androidTextColorSecondary(context))
            )
        }
        addView(
            waypointCoordinates,
            LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = Resources.dp(context, 8f).toInt()
                topMargin = Resources.dp(context, 4f).toInt()
                bottomToBottom = LayoutParams.PARENT_ID
                startToEnd = R.id.signal_strength
                topToBottom = R.id.waypoint_time
                goneStartMargin = 0
            }
        )

        waypointMenuBtn = ImageButton(context).apply {
            id = R.id.waypoint_menu_btn
            alpha = 0.86f
            setBackgroundColor(Color.TRANSPARENT)
            setImageResource(R.drawable.ic_menu_dots)
            imageTintList = ColorStateList.valueOf(Resources.androidTextColorSecondary(context))
        }
        addView(
            waypointMenuBtn,
            LayoutParams(
                Resources.dp(context, 32f).toInt(),
                Resources.dp(context, 32f).toInt()
            ).apply {
                marginEnd = Resources.dp(context, 16f).toInt()
                bottomToBottom = LayoutParams.PARENT_ID
                endToEnd = LayoutParams.PARENT_ID
                topToTop = LayoutParams.PARENT_ID
            }
        )
    }
}
