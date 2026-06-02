package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.ColorInt
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R

class ColorButton(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    private val backgroundView: ImageView
    private val foregroundView: ImageView

    var isButtonSelected: Boolean
        get() = _isSelected
        set(value) {
            _isSelected = value
            backgroundView.visibility = if (value) View.VISIBLE else View.INVISIBLE
        }

    private var _isSelected = false

    init {
        backgroundView = ImageView(context).apply {
            id = R.id.color_btn_background
            setImageResource(R.drawable.bubble)
            imageTintList = ColorStateList.valueOf(Resources.androidTextColorPrimary(context))
        }
        addView(
            backgroundView,
            LayoutParams(
                Resources.dp(context, 32f).toInt(),
                Resources.dp(context, 32f).toInt(),
                Gravity.CENTER
            )
        )

        foregroundView = ImageView(context).apply {
            id = R.id.color_btn_foreground
            setImageResource(R.drawable.bubble)
        }
        addView(
            foregroundView,
            LayoutParams(
                Resources.dp(context, 24f).toInt(),
                Resources.dp(context, 24f).toInt(),
                Gravity.CENTER
            )
        )

        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.ColorButton,
            0,
            0
        )
        val color = a.getColor(R.styleable.ColorButton_buttonColor, Color.BLACK)
        a.recycle()

        foregroundView.imageTintList = ColorStateList.valueOf(color)
        isButtonSelected = false
    }

    fun setButtonColor(@ColorInt color: Int) {
        foregroundView.imageTintList = ColorStateList.valueOf(color)
    }
}
