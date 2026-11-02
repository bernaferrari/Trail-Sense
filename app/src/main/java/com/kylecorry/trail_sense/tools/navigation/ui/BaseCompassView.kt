package com.kylecorry.trail_sense.tools.navigation.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.navigation.ui.layers.compass.ICompassLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.compass.ICompassView

abstract class BaseCompassView(
    protected val context: Context
) : ICompassView {

    private val bitmapLoader by lazy { BitmapLoader(context) }
    protected val prefs by lazy { UserPreferences(context) }
    protected var renderVersion by mutableIntStateOf(0)

    private var _compassCenter by mutableStateOf(Coordinate.zero)
    private var _useTrueNorth by mutableStateOf(false)
    private var _declination by mutableFloatStateOf(0f)
    private var _azimuth by mutableFloatStateOf(0f)
    private var contentWidth by mutableIntStateOf(0)
    private var contentHeight by mutableIntStateOf(0)
    private var contentVisible by mutableStateOf(true)

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

    var isVisible: Boolean
        get() = contentVisible
        set(value) {
            contentVisible = value
            if (!value) {
                bitmapLoader.clear()
            }
        }

    var isInvisible: Boolean
        get() = !contentVisible
        set(value) {
            isVisible = !value
        }

    val width: Int
        get() = contentWidth

    val height: Int
        get() = contentHeight

    fun invalidate() {
        requestRedraw()
    }

    @Composable
    abstract fun Content(modifier: Modifier = Modifier)

    protected fun requestRedraw() {
        renderVersion++
    }

    protected fun updateContentSize(width: Int, height: Int) {
        contentWidth = width
        contentHeight = height
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
