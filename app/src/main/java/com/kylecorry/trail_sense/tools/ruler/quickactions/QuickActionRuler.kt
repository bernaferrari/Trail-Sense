package com.kylecorry.trail_sense.tools.ruler.quickactions

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.core.system.Screen
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.icons.trail.Ruler
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.shared.views.compose.AppOverlay
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.tools.ruler.ui.RulerCanvas

/** Shows an 80 dp ruler along the screen's start edge, above the navigation bar, until tapped again. */
class QuickActionRuler(
    btn: QuickActionButtonView,
    host: TrailSenseDestinationHost,
) : QuickActionButton(btn, host) {
    private val prefs by lazy { UserPreferences(context) }

    private var overlay: AppOverlay? = null
    private var isRulerVisible by mutableStateOf(false)

    override fun onCreate() {
        super.onCreate()

        removeRuler()

        val rulerUnits = if (prefs.baseDistanceUnits.isMetric) {
            DistanceUnits.Centimeters
        } else {
            DistanceUnits.Inches
        }
        val yDpi = Screen.ydpi(context)
        val scale = prefs.ruler.rulerScale
        val flipped = prefs.ruler.rulerFlipped
        isRulerVisible = false
        overlay = AppHost.from(context).addOverlay {
            if (isRulerVisible) {
                RulerCanvas(
                    rulerUnits = rulerUnits,
                    highlight = null,
                    yDpi = yDpi,
                    scale = scale,
                    flipped = flipped,
                    onDistanceSelected = { _ -> showRuler(false) },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .width(80.dp)
                        .fillMaxHeight()
                )
            }
        }

        setIcon(TrailIcons.Ruler)
    }

    override fun onClick() {
        super.onClick()
        showRuler(!isRulerVisible)
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        host.navigator.navigate(Route.Ruler)
        return true
    }

    override fun onPause() {
        super.onPause()
        if (isRulerVisible) {
            showRuler(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeRuler()
    }

    private fun showRuler(show: Boolean) {
        setState(show)
        isRulerVisible = show
    }

    private fun removeRuler() {
        overlay?.remove()
        overlay = null
    }

}
