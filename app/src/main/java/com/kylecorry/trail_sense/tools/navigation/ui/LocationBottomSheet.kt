package com.kylecorry.trail_sense.tools.navigation.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.andromeda.core.ui.setCompoundDrawables
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.andromeda.sense.location.ISatelliteGPS
import com.kylecorry.andromeda.views.toolbar.Toolbar
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.domain.BuiltInCoordinateFormat
import com.kylecorry.trail_sense.shared.sharing.Share
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import java.time.Duration
import java.time.Instant

class LocationBottomSheet : BottomSheetDialogFragment() {

    var gps: ISatelliteGPS? = null
    private var locationViews: LocationViews? = null
    private val binding: LocationViews?
        get() = locationViews

    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val prefs by lazy { UserPreferences(requireContext()) }
    private var format = BuiltInCoordinateFormat.DecimalDegrees

    private val intervalometer = CoroutineTimer {
        updateUI()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val views = createLocationViews(requireContext())
        locationViews = views
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    LocationHost(
                        views = views,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        locationViews = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val views = binding ?: return

        format = prefs.navigation.coordinateFormat

        views.locationTitle.subtitle.setCompoundDrawables(
            Resources.dp(requireContext(), 24f).toInt(),
            right = R.drawable.ic_drop_down
        )
        CustomUiUtils.setImageColor(
            views.locationTitle.subtitle,
            Resources.androidTextColorSecondary(requireContext())
        )
        views.locationTitle.subtitle.text = formatService.formatCoordinateType(format)

        views.locationTitle.subtitle.setOnClickListener {
            val formats = BuiltInCoordinateFormat.entries
            val formatStrings = formats.map { formatService.formatCoordinateType(it) }
            Pickers.item(
                requireContext(),
                getString(R.string.pref_coordinate_format_title),
                formatStrings,
                defaultSelectedIndex = formats.indexOf(format)
            ) {
                if (it != null) {
                    format = formats[it]
                    views.locationTitle.subtitle.text = formatStrings[it]
                    updateUI()
                }
            }
        }

        views.locationTitle.rightButton.setOnClickListener {
            Share.shareLocation(
                this,
                gps?.location ?: return@setOnClickListener,
                format
            )
        }
    }

    override fun onResume() {
        super.onResume()
        intervalometer.interval(100)
    }

    override fun onPause() {
        super.onPause()
        intervalometer.stop()
    }

    private fun updateUI() {
        val views = binding ?: return

        val gps = this.gps ?: return

        views.locationTitle.title.text = formatService.formatLocation(gps.location, format)
        views.satellites.text = getString(R.string.num_satellites, gps.satellites ?: 0)

        val accuracy = gps.horizontalAccuracy
        views.accuracy.isVisible = accuracy != null
        if (accuracy != null) {
            val accuracyStr = formatService.formatDistance(
                Distance.meters(accuracy).convertTo(prefs.baseDistanceUnits)
            )
            views.accuracy.text = getString(R.string.accuracy_distance_format, accuracyStr)
        }

        val timeAgo = Duration.between(gps.time, Instant.now())
        views.time.text = getString(R.string.time_ago, formatService.formatDuration(timeAgo))
    }
}

private data class LocationViews(
    val root: View,
    val locationTitle: Toolbar,
    val accuracy: TextView,
    val satellites: TextView,
    val time: TextView,
    val datum: TextView
)

@Composable
private fun LocationHost(
    views: LocationViews,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { views.root }
    )
}

private fun createLocationViews(context: Context): LocationViews {
    val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    val title = Toolbar(context, null).apply {
        id = R.id.location_title
        rightButton.setImageResource(R.drawable.ic_share)
        rightButton.isVisible = true
    }
    root.addView(
        title,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    )

    val stats = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
    }
    root.addView(
        stats,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = 8.dp(context)
        }
    )

    val accuracy = createCenteredStat(context, R.id.accuracy)
    stats.addView(accuracy, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

    val satellites = createCenteredStat(context, R.id.satellites)
    stats.addView(satellites, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

    val time = createCenteredStat(context, R.id.time)
    stats.addView(time, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

    val datum = TextView(context).apply {
        id = R.id.datum
        text = context.getString(R.string.wgs84)
        gravity = android.view.Gravity.CENTER_VERTICAL
        compoundDrawablePadding = 8.dp(context)
        setCompoundDrawables(24.dp(context), left = R.drawable.ic_datum)
        CustomUiUtils.setImageColor(this, Resources.androidTextColorSecondary(context))
    }
    root.addView(
        datum,
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = android.view.Gravity.CENTER_HORIZONTAL
            topMargin = 16.dp(context)
        }
    )

    return LocationViews(root, title, accuracy, satellites, time, datum)
}

private fun createCenteredStat(context: Context, id: Int): TextView {
    return TextView(context).apply {
        this.id = id
        textAlignment = View.TEXT_ALIGNMENT_CENTER
    }
}

private fun Int.dp(context: Context): Int {
    return Resources.dp(context, toFloat()).toInt()
}

@Preview(showBackground = true)
@Composable
private fun LocationHostPreview() {
    TrailSenseTheme {
        LocationHost(
            views = createLocationViews(LocalContext.current)
        )
    }
}
