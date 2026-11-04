package com.kylecorry.trail_sense.tools.astronomy.ui.format

import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.trail_sense.shared.icons.trail.*
import android.content.Context
import com.kylecorry.sol.science.astronomy.locators.Planet
import com.kylecorry.trail_sense.R

class PlanetMapper(private val context: Context) {
    fun getName(planet: Planet): String {
        return when (planet) {
            Planet.Mercury -> context.getString(R.string.planet_mercury)
            Planet.Venus -> context.getString(R.string.planet_venus)
            Planet.Mars -> context.getString(R.string.planet_mars)
            Planet.Jupiter -> context.getString(R.string.planet_jupiter)
            Planet.Saturn -> context.getString(R.string.planet_saturn)
            Planet.Uranus -> context.getString(R.string.planet_uranus)
            Planet.Neptune -> context.getString(R.string.planet_neptune)
            else -> "" // Not including Earth since that isn't going to show up anywhere
        }
    }

    fun getImage(planet: Planet): ImageVector {
        return when (planet) {
            Planet.Mercury -> TrailIcons.PlanetMercury
            Planet.Venus -> TrailIcons.PlanetVenus
            Planet.Mars -> TrailIcons.PlanetMars
            Planet.Jupiter -> TrailIcons.PlanetJupiter
            Planet.Saturn -> TrailIcons.PlanetSaturn
            Planet.Uranus -> TrailIcons.PlanetUranus
            Planet.Neptune -> TrailIcons.PlanetNetpune
            else -> TrailIcons.Bubble // Not including Earth since that isn't going to show up anywhere
        }
    }
}
