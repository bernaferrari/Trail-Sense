package com.kylecorry.trail_sense.tools.navigation.ui.layers.compass

interface ICompassLayer {
    fun draw(compass: ICompassView)
    fun invalidate()
}
