package com.kylecorry.trail_sense.tools.whistle.quickactions

import androidx.lifecycle.lifecycleScope

import com.kylecorry.trail_sense.shared.icons.trail.*
import android.annotation.SuppressLint
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.andromeda.sound.ISoundPlayer
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.tools.whistle.infrastructure.Whistle
import kotlinx.coroutines.launch

class QuickActionWhistle(btn: QuickActionButtonView, host: TrailSenseDestinationHost) :
    QuickActionButton(btn, host) {

    private var whistle: ISoundPlayer? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()

        host.lifecycleOwner.lifecycleScope.launch {
            onDefault {
                try {
                    whistle = Whistle()
                } catch (e: Exception) {
                    e.printStackTrace()
                    // The whistle couldn't be instantiated
                    onMain {
                        button.isVisible = false
                    }
                }
            }
        }

        setIcon(TrailIcons.ToolWhistle)
        button.setOnPressListener { pressed ->
            if (pressed) whistle?.on() else whistle?.off()
            isRunning = pressed
            setState(pressed)
        }
    }

    override fun onResume() {
        super.onResume()
        setState(false)
    }

    override fun onPause() {
        super.onPause()
        whistle?.off()
        isRunning = false
    }

    override fun onDestroy() {
        super.onDestroy()
        whistle?.release()
        isRunning = false
    }

    companion object {
        var isRunning = false
            private set
    }

}
