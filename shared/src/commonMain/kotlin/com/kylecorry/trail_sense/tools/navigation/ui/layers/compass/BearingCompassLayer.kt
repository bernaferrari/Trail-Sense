package com.kylecorry.trail_sense.tools.navigation.ui.layers.compass
import kotlinx.atomicfu.locks.synchronized
import kotlinx.atomicfu.locks.SynchronizedObject
import com.kylecorry.trail_sense.tools.navigation.ui.IMappableBearing

class BearingCompassLayer : ICompassLayer {

    private val bearings = mutableListOf<IMappableBearing>()
    private val lock = SynchronizedObject()

    fun addBearing(bearing: IMappableBearing) {
        synchronized(lock) {
            bearings.add(bearing)
        }
    }

    fun clearBearings() {
        synchronized(lock) {
            bearings.clear()
        }
    }

    override fun draw(compass: ICompassView) {
        val bearings = synchronized(lock) {
            bearings.toList()
        }
        bearings.forEach {
            compass.draw(it)
        }
    }

    override fun invalidate() {
    }
}
