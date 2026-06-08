package com.kylecorry.trail_sense.tools.offline_maps.ui.commands.create

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.platform.ComposeView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import com.kylecorry.andromeda.alerts.CoroutineAlerts
import com.kylecorry.andromeda.alerts.loading.ILoadingIndicator
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.CompassDirection
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.DistanceUtils
import com.kylecorry.trail_sense.shared.DistanceUtils.toRelativeDistance
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.andromeda_temp.Result
import com.kylecorry.trail_sense.shared.andromeda_temp.map
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.views.CoordinateInputView
import com.kylecorry.trail_sense.shared.views.DistanceInputView
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.MapCalibrationPoint
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PercentCoordinate
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.MapService

class CreateBlankMapCommand(
    private val context: Context
) : ICreateMapCommand {

    private val files = FileSubsystem.getInstance(context)
    private val location = LocationSubsystem.getInstance(context)
    private val prefs = UserPreferences(context)
    private val formatter = FormatService.getInstance(context)

    override suspend fun execute(): Result<CreateOfflineMapRequest, CreateOfflineMapError> = onIO {
        val calibration = getCalibration() ?: return@onIO Result.Err(CreateOfflineMapError.Cancelled)

        val file = files.createTemp(".webp")
        val bitmap = createBitmap(100, 100)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        files.save(files.getLocalPath(file), bitmap, 10, true)

        CreateMapFromUriCommand(
            context,
            file.toUri()
        ).execute().map {
            it.copy(photoMapCalibration = calibration, visible = false)
        }
    }

    private suspend fun getCalibration(): List<MapCalibrationPoint>? {
        val allUnits = formatter.sortDistanceUnits(DistanceUtils.hikingDistanceUnits)

        val defaultDistance =
            Distance.kilometers(2f).convertTo(prefs.baseDistanceUnits).toRelativeDistance()
        var distance = defaultDistance
        var center = location.location

        val cancelled = onMain {
            val view = createBlankMapCalibrationView()
            val distanceInput = view.findViewById<DistanceInputView>(R.id.distance_input)
            val coordinateInput = view.findViewById<CoordinateInputView>(R.id.coordinate_input)

            coordinateInput.coordinate = center
            coordinateInput.setOnCoordinateChangeListener {
                center = it ?: location.location
            }

            distanceInput.units = allUnits
            distanceInput.value = distance
            distanceInput.setOnValueChangeListener {
                distance = it ?: defaultDistance
            }

            try {
                CoroutineAlerts.dialog(
                    context,
                    context.getString(R.string.create_blank_map),
                    contentView = view
                )
            } finally {
                tryOrNothing { coordinateInput.pause() }
            }
        }

        if (cancelled) {
            return null
        }

        val topLeft = center.plus(distance, Bearing.from(CompassDirection.NorthWest))
        return listOf(
            MapCalibrationPoint(center, PercentCoordinate(0.5f, 0.5f)),
            MapCalibrationPoint(topLeft, PercentCoordinate(0f, 0f)),
        )
    }

    private fun createBlankMapCalibrationView(): View {
        val padding = Resources.dp(context, 16f).toInt()
        val smallSpacing = Resources.dp(context, 4f).toInt()
        val spacing = Resources.dp(context, 8f).toInt()
        val root = ConstraintLayout(context).apply {
            setPadding(padding, padding, padding, padding)
        }

        val centerLabelId = View.generateViewId()
        root.addView(
            createBlankMapLabel(context, context.getString(R.string.center)).apply {
                id = centerLabelId
            },
            ConstraintLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            }
        )

        root.addView(
            CoordinateInputView(context).apply {
                id = R.id.coordinate_input
            },
            ConstraintLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                topToBottom = centerLabelId
                topMargin = smallSpacing
            }
        )

        val sizeLabelId = View.generateViewId()
        root.addView(
            createBlankMapLabel(context, context.getString(R.string.size)).apply {
                id = sizeLabelId
            },
            ConstraintLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                topToBottom = R.id.coordinate_input
                topMargin = spacing
            }
        )

        root.addView(
            DistanceInputView(context).apply {
                id = R.id.distance_input
            },
            ConstraintLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                topToBottom = sizeLabelId
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                topMargin = smallSpacing
            }
        )

        return root
    }

    private fun createBlankMapLabel(context: Context, text: String): ComposeView {
        return ComposeView(context).apply {
            setContent {
                TrailSenseTheme {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

}
