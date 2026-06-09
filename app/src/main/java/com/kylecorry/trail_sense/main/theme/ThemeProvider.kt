package com.kylecorry.trail_sense.main.theme

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem
import com.kylecorry.trail_sense.tools.astronomy.domain.AstronomyService

enum class AppThemeMode {
    Light,
    Dark,
    System
}

class ThemeProvider {

    private val prefs = getAppService<UserPreferences>()
    private val astronomy = AstronomyService()
    private val sensors = getAppService<SensorSubsystem>()

    fun getThemeMode(): AppThemeMode {
        return when (prefs.theme) {
            UserPreferences.Theme.Light -> AppThemeMode.Light
            UserPreferences.Theme.Dark, UserPreferences.Theme.Black, UserPreferences.Theme.Night -> AppThemeMode.Dark
            UserPreferences.Theme.System, UserPreferences.Theme.SystemBlack -> AppThemeMode.System
            UserPreferences.Theme.SunriseSunset -> sunriseSunsetTheme()
        }
    }

    private fun sunriseSunsetTheme(): AppThemeMode {
        val location = sensors.lastKnownLocation
        if (location == Coordinate.zero) {
            return AppThemeMode.System
        }
        val isSunUp = astronomy.isSunUp(location)
        return if (isSunUp) {
            AppThemeMode.Light
        } else {
            AppThemeMode.Dark
        }
    }
}
