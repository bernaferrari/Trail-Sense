package com.kylecorry.trail_sense.tools.ruler.quickactions

import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.system.Screen
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeRootView
import com.kylecorry.trail_sense.tools.ruler.ui.RulerCanvas
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class QuickActionRuler(
    btn: QuickActionButtonView,
    fragment: TrailSenseDestinationHost,
) : QuickActionButton(btn, fragment) {
    private val prefs by lazy { UserPreferences(context) }

    private var ruler: TrailSenseComposeRootView? = null

    override fun onCreate() {
        super.onCreate()

        if (ruler != null) {
            removeRuler()
        }

        val rulerUnits = if (prefs.baseDistanceUnits.isMetric) {
            DistanceUnits.Centimeters
        } else {
            DistanceUnits.Inches
        }
        val ruler = TrailSenseComposeRootView(
            context,
            ViewCompositionStrategy.DisposeOnDetachedFromWindow
        ) {
            RulerCanvas(
                rulerUnits = rulerUnits,
                highlight = null,
                yDpi = Screen.ydpi(context),
                scale = prefs.ruler.rulerScale,
                flipped = prefs.ruler.rulerFlipped,
                onDistanceSelected = { _ ->
                    setState(false)
                    this.ruler?.isVisible = false
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        this.ruler = ruler
        ruler.isVisible = false

        ruler.x = 0f
        ruler.y = 0f
        val layoutParams = CoordinatorLayout.LayoutParams(
            Resources.dp(context, 80f).toInt(),
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        layoutParams.bottomMargin = fragment.getBottomNavigationHeight()
        ruler.layoutParams = layoutParams

        fragment.addOverlayView(ruler)

        setIcon(R.drawable.ruler)
    }

    override fun onClick() {
        super.onClick()
        if (ruler?.isVisible == true) {
            setState(false)
            ruler?.isVisible = false
        } else {
            setState(true)
            ruler?.isVisible = true
        }
    }

    override fun onLongClick(): Boolean {
        super.onLongClick()
        fragment.findNavController().openTool(Tools.RULER)
        return true
    }

    override fun onPause() {
        super.onPause()
        if (ruler?.isVisible == true) {
            setState(false)
            ruler?.isVisible = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeRuler()
    }

    private fun removeRuler() {
        ruler?.let {
            fragment.removeOverlayView(it)
        }
        ruler = null
    }

}
