package com.kylecorry.trail_sense.tools.clinometer.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.kylecorry.sol.science.geology.Geology
import com.kylecorry.sol.units.Distance
import java.time.Duration
import java.time.Instant
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** What lifting the finger did to a hold. */
internal enum class ClinometerHoldResult {
    /** Nothing was held. */
    None,

    /** A tap (too short or too small a sweep): the reading stays locked at the first angle. */
    SingleAngle,

    /** A tap on a locked reading: the lock is cleared. */
    Cleared,

    /** A sweep from the first angle to this one: both are locked. */
    Sweep
}

/**
 * The clinometer's measurement: whether it reads through the camera or along the phone's side, the known distance
 * or height, and the locked readings a tap or a hold-and-sweep leaves. The live angle comes from the sensors in the
 * screen; this decides what is shown and measured from it.
 */
internal class ClinometerViewModel(
    private val baselineDistance: () -> Distance?,
    private val now: () -> Instant = Instant::now
) : ViewModel() {

    var useCamera by mutableStateOf(true)

    /** The distance to the object, to measure its height. */
    var distanceAway by mutableStateOf<Distance?>(null)
        private set

    /** The object's height, to measure the distance to it. */
    var knownHeight by mutableStateOf<Distance?>(null)
        private set

    var lockedAngle1: Float? = null
        private set
    var lockedIncline1: Float? = null
        private set
    var lockedAngle2: Float? = null
        private set
    var lockedIncline2: Float? = null
        private set
    var isHolding = false
        private set

    private var lockStartTime: Instant? = null
    private var hadLock = false

    val isLocked: Boolean
        get() = lockedAngle1 != null

    /** Starts from the saved baseline distance unless a height or distance is already set. */
    fun onResume() {
        if (distanceAway == null && knownHeight == null) {
            distanceAway = baselineDistance()
        }
    }

    fun measureHeight(distanceAway: Distance) {
        this.distanceAway = distanceAway
        knownHeight = null
    }

    fun measureDistance(knownHeight: Distance) {
        this.knownHeight = knownHeight
        distanceAway = null
    }

    /** The angle to show: the sweep's end, else the lock (unless a new hold is under way), else the live one. */
    fun displayedAngle(currentAngle: Float): Float {
        return lockedAngle2 ?: (if (!isHolding) lockedAngle1 else null) ?: currentAngle
    }

    fun displayedIncline(currentIncline: Float): Float {
        return lockedIncline2 ?: (if (!isHolding) lockedIncline1 else null) ?: currentIncline
    }

    /** A finger lands: lock the current reading. Returns false if a hold is already under way. */
    fun startHold(angle: Float, incline: Float): Boolean {
        if (isHolding) {
            return false
        }
        isHolding = true
        hadLock = lockedAngle1 != null
        clearLock()
        lockedAngle1 = angle
        lockedIncline1 = incline
        lockStartTime = now()
        return true
    }

    /** The finger lifts: a sweep locks both ends; a tap keeps the first (or clears a lock that was there). */
    fun endHold(angle: Float, incline: Float): ClinometerHoldResult {
        if (!isHolding) {
            return ClinometerHoldResult.None
        }
        isHolding = false

        val deltaAngle = abs(incline - (lockedIncline1 ?: 0f))
        val deltaTime = Duration.between(lockStartTime, now())
        if (deltaAngle < MINIMUM_HOLD_ANGLE || deltaTime < MINIMUM_HOLD_DURATION) {
            if (hadLock) {
                clearLock()
                return ClinometerHoldResult.Cleared
            }
            return ClinometerHoldResult.SingleAngle
        }

        lockedAngle2 = angle
        lockedIncline2 = incline
        return ClinometerHoldResult.Sweep
    }

    fun clearLock() {
        lockedAngle1 = null
        lockedIncline1 = null
        lockedAngle2 = null
        lockedIncline2 = null
        lockStartTime = null
    }

    /** The object's height from [distanceAway] and the inclines to its bottom and top. */
    fun getHeight(distanceAway: Distance, currentIncline: Float): Distance {
        return Geology.getHeightFromInclination(
            distanceAway,
            getBottomIncline(currentIncline),
            getTopIncline(currentIncline)
        )
    }

    /** The distance to the object from its [height] and the inclines to its bottom and top. */
    fun getDistance(height: Distance, currentIncline: Float): Distance {
        return Geology.getDistanceFromInclination(
            height,
            getBottomIncline(currentIncline),
            getTopIncline(currentIncline)
        )
    }

    private fun getBottomIncline(currentIncline: Float): Float {
        return min(lockedIncline1 ?: 0f, lockedIncline2 ?: currentIncline)
    }

    private fun getTopIncline(currentIncline: Float): Float {
        return max(lockedIncline1 ?: 0f, lockedIncline2 ?: currentIncline)
    }

    companion object {
        private val MINIMUM_HOLD_DURATION = Duration.ofMillis(200)
        private const val MINIMUM_HOLD_ANGLE = 0.5f
    }
}
