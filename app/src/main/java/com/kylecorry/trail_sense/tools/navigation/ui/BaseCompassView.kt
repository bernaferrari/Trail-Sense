package com.kylecorry.trail_sense.tools.navigation.ui

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.navigation.ui.layers.compass.ICompassLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.compass.ICompassView

abstract class BaseCompassView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr), ICompassView {

    private val bitmapLoader by lazy { BitmapLoader(context) }
    protected val prefs by lazy { UserPreferences(context) }
    protected var renderVersion by mutableIntStateOf(0)

    private var _compassCenter by mutableStateOf(Coordinate.zero)
    private var _useTrueNorth by mutableStateOf(false)
    private var _declination by mutableFloatStateOf(0f)
    private var _azimuth by mutableFloatStateOf(0f)

    override var compassCenter: Coordinate
        get() = _compassCenter
        set(value) {
            _compassCenter = value
            requestRedraw()
        }

    override var useTrueNorth: Boolean
        get() = _useTrueNorth
        set(value) {
            _useTrueNorth = value
            requestRedraw()
        }

    override var declination: Float
        get() = _declination
        set(value) {
            _declination = value
            requestRedraw()
        }

    override var azimuth: Float
        get() = _azimuth
        set(value) {
            _azimuth = value
            requestRedraw()
        }

    private val compassLayers = mutableListOf<ICompassLayer>()

    init {
        useTrueNorth = prefs.compass.useTrueNorth
    }

    protected fun requestRedraw() {
        renderVersion++
    }

    protected open fun finalize() {
        bitmapLoader.clear()
    }

    protected fun getBitmap(@DrawableRes id: Int, size: Int): Bitmap {
        return bitmapLoader.load(id, size)
    }

    protected fun drawCompassLayers() {
        compassLayers.forEach { it.draw(this) }
    }

    protected fun dp(value: Float): Float {
        return Resources.dp(context, value)
    }

    protected fun sp(value: Float): Float {
        return Resources.sp(context, value)
    }

    override fun addCompassLayer(layer: ICompassLayer) {
        compassLayers.add(layer)
        requestRedraw()
    }

    override fun removeCompassLayer(layer: ICompassLayer) {
        compassLayers.remove(layer)
        requestRedraw()
    }

    override fun setCompassLayers(layers: List<ICompassLayer>) {
        compassLayers.clear()
        compassLayers.addAll(layers)
        requestRedraw()
    }
}
