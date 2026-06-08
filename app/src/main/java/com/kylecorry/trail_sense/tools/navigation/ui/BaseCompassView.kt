package com.kylecorry.trail_sense.tools.navigation.ui

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.View
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.isInvisible
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.tools.navigation.ui.layers.compass.ICompassLayer
import com.kylecorry.trail_sense.tools.navigation.ui.layers.compass.ICompassView

abstract class BaseCompassView @JvmOverloads constructor(
    protected val context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
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

    val view = ComposeView(context).apply {
        setContent {
            Content()
        }
    }

    var id: Int
        get() = view.id
        set(value) {
            view.id = value
        }

    var isVisible: Boolean
        get() = view.visibility == View.VISIBLE
        set(value) {
            view.visibility = if (value) View.VISIBLE else View.GONE
        }

    var visibility: Int
        get() = view.visibility
        set(value) {
            view.visibility = value
        }

    var isInvisible: Boolean
        get() = view.isInvisible
        set(value) {
            view.isInvisible = value
        }

    var isClickable: Boolean
        get() = view.isClickable
        set(value) {
            view.isClickable = value
        }

    var isFocusable: Boolean
        get() = view.isFocusable
        set(value) {
            view.isFocusable = value
        }

    val width: Int
        get() = view.width.takeIf { it > 0 } ?: contentWidth

    val height: Int
        get() = view.height.takeIf { it > 0 } ?: contentHeight

    fun setOnClickListener(listener: View.OnClickListener?) {
        view.setOnClickListener(listener)
    }

    fun invalidate() {
        requestRedraw()
        view.invalidate()
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
