package com.kylecorry.trail_sense.tools.flashlight.widgets

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSubsystem

class FlashlightWidgetActivityWorkaround : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FlashlightSubsystem.getInstance(this).toggle()
        finishAffinity()
    }

}
