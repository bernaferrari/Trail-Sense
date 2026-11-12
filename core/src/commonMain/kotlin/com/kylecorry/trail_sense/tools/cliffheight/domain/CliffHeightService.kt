package com.kylecorry.trail_sense.tools.cliffheight.domain



import com.kylecorry.luna.hooks.Hooks
import com.kylecorry.sol.science.geophysics.Geophysics
import com.kylecorry.sol.science.physics.Physics
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import kotlin.time.Instant

class CliffHeightService {

    private val hooks = Hooks()

    fun getCliffHeight(start: Instant, end: Instant, location: Coordinate? = null): Distance {
        val time = (end - start)

        val gravity = hooks.memo(
            "gravity",
            location
        ) {
            location?.let { Geophysics.getGravity(it) } ?: Geophysics.GRAVITY
        }

        return Physics.fallHeight(time, gravity)
    }

}
