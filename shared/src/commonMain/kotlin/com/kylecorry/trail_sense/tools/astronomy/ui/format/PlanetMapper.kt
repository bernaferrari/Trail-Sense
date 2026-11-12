package com.kylecorry.trail_sense.tools.astronomy.ui.format

import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.sol.science.astronomy.locators.Planet
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.generated.resources.Res
import com.kylecorry.trail_sense.shared.generated.resources.planet_jupiter
import com.kylecorry.trail_sense.shared.generated.resources.planet_mars
import com.kylecorry.trail_sense.shared.generated.resources.planet_mercury
import com.kylecorry.trail_sense.shared.generated.resources.planet_neptune
import com.kylecorry.trail_sense.shared.generated.resources.planet_saturn
import com.kylecorry.trail_sense.shared.generated.resources.planet_uranus
import com.kylecorry.trail_sense.shared.generated.resources.planet_venus
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.text.AppStrings

class PlanetMapper {
    private val strings = getAppService<AppStrings>()

    fun getName(planet: Planet): String {
        return when (planet) {
            Planet.Mercury -> strings.get(Res.string.planet_mercury)
            Planet.Venus -> strings.get(Res.string.planet_venus)
            Planet.Mars -> strings.get(Res.string.planet_mars)
            Planet.Jupiter -> strings.get(Res.string.planet_jupiter)
            Planet.Saturn -> strings.get(Res.string.planet_saturn)
            Planet.Uranus -> strings.get(Res.string.planet_uranus)
            Planet.Neptune -> strings.get(Res.string.planet_neptune)
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
