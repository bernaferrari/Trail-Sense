package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ScrollView
import androidx.core.view.setPadding
import com.google.android.flexbox.AlignContent
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexboxLayout
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.JustifyContent
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.tools.beacons.domain.BeaconIcon

class BeaconIconPickerView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    private val flex: FlexboxLayout
    private val selectableIcons = BeaconIcon.values().filter { it.isUserSelectable }
    private val buttons = mutableMapOf<BeaconIcon?, ImageButton>()
    private var changeListener: ((icon: BeaconIcon?) -> Unit)? = null

    var icon: BeaconIcon? = null
        set(value) {
            deselectButton(field)
            selectButton(value)
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

        addButton(null)
        for (icon in selectableIcons) {
            addButton(icon)
        }

    }

    private fun selectButton(icon: BeaconIcon?) {
        buttons[icon]?.let { CustomUiUtils.setButtonState(it, true) }
    }

    private fun deselectButton(icon: BeaconIcon?) {
        buttons[icon]?.let { CustomUiUtils.setButtonState(it, false) }
    }

    private fun addButton(icon: BeaconIcon?) {
        val button = ImageButton(context, null)
        button.setImageResource(icon?.icon ?: R.drawable.bubble)
        CustomUiUtils.setButtonState(button, false)
        button.setPadding(Resources.dp(context, 16f).toInt())
        button.setOnClickListener {
            this.icon = icon
            changeListener?.invoke(icon)
        }
        buttons[icon] = button
        flex.addView(button)
    }

    fun setOnIconChangeListener(listener: ((icon: BeaconIcon?) -> Unit)?) {
        changeListener = listener
    }
}
