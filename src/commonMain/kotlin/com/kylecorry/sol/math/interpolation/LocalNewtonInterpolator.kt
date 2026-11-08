package com.kylecorry.sol.math.interpolation

import com.kylecorry.sol.math.Vector2
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * A newton interpolator that only uses the points surrounding the x to interpolate
 * rather than the points from the beginning.
 */
@OptIn(ExperimentalAtomicApi::class)
class LocalNewtonInterpolator(points: List<Vector2>, private val order: Int) : Interpolator {
    private val sortedPoints = points.sortedBy { it.x }
    private val cache = AtomicReference<Pair<List<Vector2>, NewtonInterpolator>?>(null)

    override fun interpolate(x: Float): Float {
        if (sortedPoints.isEmpty()) {
            return 0f
        }

        val requiredPoints = order + 1
        val beforePoints = requiredPoints / 2

        val lastBeforeIndex = sortedPoints.indexOfLast { it.x <= x }
        var startIndex = lastBeforeIndex - beforePoints + 1
        var endIndex = startIndex + order

        if (startIndex < 0) {
            startIndex = 0
            endIndex = order
        }

        if (endIndex >= sortedPoints.size) {
            endIndex = sortedPoints.size - 1
            startIndex = (endIndex - order).coerceAtLeast(0)
        }

        if ((endIndex - startIndex) < order) {
            val linear = LinearInterpolator(sortedPoints)
            return linear.interpolate(x)
        }

        val points = sortedPoints.subList(startIndex, endIndex + 1)
        return interpolate(x, points)
    }


    private fun interpolate(
        x: Float,
        points: List<Vector2>
    ): Float {
        val current = cache.load()
        val interpolator = if (current != null && current.first == points) {
            current.second
        } else {
            NewtonInterpolator(points, order).also { cache.store(points.toList() to it) }
        }
        return interpolator.interpolate(x)
    }
}
