package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.FrameLayout
import androidx.core.view.setPadding
import com.google.android.flexbox.AlignContent
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.JustifyContent
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.colors.AppColor

class ColorPickerView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    private val flex: FlexboxLayout
    private var changeListener: ((color: AppColor?) -> Unit)? = null

    var color: AppColor? = null
        set(value) {
            if (field != null) {
                (flex.getChildAt(field?.ordinal ?: 0) as ColorButton).isButtonSelected = false
            }
            if (value != null) {
                (flex.getChildAt(value.ordinal) as ColorButton).isButtonSelected = true
            }
            field = value
        }


    init {
        val scroll = ScrollView(context)
        addView(
            scroll,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        flex = FlexboxLayout(context).apply {
            id = R.id.color_picker_flex
            alignContent = AlignContent.STRETCH
            alignItems = AlignItems.STRETCH
            flexDirection = FlexDirection.ROW
            flexWrap = FlexWrap.WRAP
            justifyContent = JustifyContent.CENTER
        }
        scroll.addView(
            flex,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        for (color in AppColor.values()) {
            val colorView = ColorButton(context, null)
            colorView.setButtonColor(color.color)
            colorView.setPadding(Resources.dp(context, 16f).toInt())
            colorView.setOnClickListener {
                this.color = color
                changeListener?.invoke(color)
            }
            flex.addView(colorView)
        }

    }

    fun setOnColorChangeListener(listener: ((color: AppColor?) -> Unit)?) {
        changeListener = listener
    }
}
