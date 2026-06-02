package com.kylecorry.trail_sense.tools.weather.quickactions

import com.kylecorry.andromeda.views.toolbar.Toolbar
import com.kylecorry.trail_sense.shared.quickactions.IQuickActionBinder
import com.kylecorry.trail_sense.shared.quickactions.QuickActionFactory
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherPreferences
import com.kylecorry.trail_sense.tools.weather.ui.WeatherFragment

class WeatherQuickActionBinder(
    private val fragment: WeatherFragment,
    private val weatherTitle: Toolbar,
    private val prefs: WeatherPreferences
) : IQuickActionBinder {

    override fun bind() {
        val factory = QuickActionFactory()
        val left = factory.create(prefs.leftButton, weatherTitle.leftButton, fragment)
        val right = factory.create(prefs.rightButton, weatherTitle.rightButton, fragment)
        left.bind(fragment)
        right.bind(fragment)
    }

}
