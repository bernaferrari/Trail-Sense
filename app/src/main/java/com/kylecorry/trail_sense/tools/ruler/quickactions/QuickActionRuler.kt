package com.kylecorry.trail_sense.tools.ruler.quickactions

import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.system.Screen
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.openTool
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.tools.ruler.ui.RulerCanvas
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

class QuickActionRuler(
    btn: QuickActionButtonView,
    fragment: Fragment,
) : QuickActionButton(btn, fragment) {
    private val prefs by lazy { UserPreferences(context) }

    private var ruler: ComposeView? = null

    override fun onCreate() {
        super.onCreate()

        if (ruler != null) {
            removeRuler()
        }

        val activity = fragment.activity as? MainActivity ?: return
        val ruler = ComposeView(context)
        this.ruler = ruler
        ruler.isVisible = false
        ruler.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        ruler.setContent {
            TrailSenseTheme {
                RulerCanvas(
                    rulerUnits = if (prefs.baseDistanceUnits.isMetric) {
                        DistanceUnits.Centimeters
                    } else {
                        DistanceUnits.Inches
                    },
                    highlight = null,
                    yDpi = Screen.ydpi(context),
                    scale = prefs.ruler.rulerScale,
                    flipped = prefs.ruler.rulerFlipped,
                    onDistanceSelected = {
                        setState(false)
                        ruler.isVisible = false
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        ruler.x = 0f
        ruler.y = 0f
        val layoutParams = CoordinatorLayout.LayoutParams(
            Resources.dp(context, 80f).toInt(),
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        val bottomNavigation = activity.getBottomNavigationView()
        if (bottomNavigation.height > 0) {
            layoutParams.bottomMargin = bottomNavigation.height
        } else {
            bottomNavigation.doOnLayout {
                val params = ruler.layoutParams as? CoordinatorLayout.LayoutParams ?: return@doOnLayout
                params.bottomMargin = it.height
                ruler.layoutParams = params
            }
        }
        ruler.layoutParams = layoutParams

        activity.addOverlayView(ruler)

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
            (fragment.activity as? MainActivity)?.removeOverlayView(it)
        }
        ruler = null
    }

}
