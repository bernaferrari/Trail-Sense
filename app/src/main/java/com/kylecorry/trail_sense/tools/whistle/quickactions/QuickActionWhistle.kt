package com.kylecorry.trail_sense.tools.whistle.quickactions

import android.annotation.SuppressLint
import android.view.MotionEvent
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.andromeda.sound.ISoundPlayer
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.QuickActionButton
import com.kylecorry.trail_sense.shared.quickactions.QuickActionButtonView
import com.kylecorry.trail_sense.tools.whistle.infrastructure.Whistle
import kotlinx.coroutines.launch

class QuickActionWhistle(btn: QuickActionButtonView, fragment: TrailSenseDestinationHost) :
    QuickActionButton(btn, fragment) {

    private var whistle: ISoundPlayer? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()

        fragment.lifecycleScope.launch {
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

        setIcon(R.drawable.ic_tool_whistle)
        button.setOnTouchListener { event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                whistle?.on()
                isRunning = true
                setState(true)
            } else if (event.action == MotionEvent.ACTION_UP) {
                whistle?.off()
                isRunning = false
                setState(false)
            }
            true
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
