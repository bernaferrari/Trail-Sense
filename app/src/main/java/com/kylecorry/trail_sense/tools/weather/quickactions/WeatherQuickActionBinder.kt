package com.kylecorry.trail_sense.tools.weather.quickactions

import com.kylecorry.trail_sense.shared.quickactions.ComposeQuickActionButtonView
import com.kylecorry.trail_sense.shared.quickactions.IQuickActionBinder
import com.kylecorry.trail_sense.shared.quickactions.QuickActionFactory
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherPreferences
import com.kylecorry.trail_sense.tools.weather.ui.WeatherFragment

class WeatherQuickActionBinder(
    private val fragment: WeatherFragment,
    private val leftButton: ComposeQuickActionButtonView,
    private val rightButton: ComposeQuickActionButtonView,
    private val prefs: WeatherPreferences
) : IQuickActionBinder {

    override fun bind() {
        val factory = QuickActionFactory()
        val left = factory.create(
            prefs.leftButton,
            leftButton,
            fragment
        )
        val right = factory.create(
            prefs.rightButton,
            rightButton,
            fragment
        )
        left.bind(fragment)
        right.bind(fragment)
    }

}
