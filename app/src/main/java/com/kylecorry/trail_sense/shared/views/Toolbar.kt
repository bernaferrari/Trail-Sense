package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.extensions.flatten

class Toolbar(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    val leftButton: MaterialButton
    val rightButton: MaterialButton
    val title: TextView
    val subtitle: TextView
    private val textContainer: LinearLayout

    init {
        setPadding(
            Resources.dp(context, 16f).toInt(),
            Resources.dp(context, 16f).toInt(),
            Resources.dp(context, 16f).toInt(),
            Resources.dp(context, 16f).toInt()
        )

        title = TextView(context).apply {
            id = View.generateViewId()
            gravity = Gravity.CENTER
            textAlignment = TEXT_ALIGNMENT_CENTER
            setTextColor(Resources.androidTextColorPrimary(context))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTypeface(typeface, Typeface.BOLD)
            compoundDrawablePadding = Resources.dp(context, 8f).toInt()
        }

        subtitle = TextView(context).apply {
            id = View.generateViewId()
            gravity = Gravity.CENTER
            textAlignment = TEXT_ALIGNMENT_CENTER
            setTextColor(Resources.androidTextColorSecondary(context))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            compoundDrawablePadding = Resources.dp(context, 8f).toInt()
        }

        addView(
            LinearLayout(context).apply {
                gravity = Gravity.CENTER
                orientation = LinearLayout.VERTICAL
                addView(
                    title,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                )
                addView(
                    subtitle,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = Resources.dp(context, 4f).toInt()
                    }
                )
            },
            LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        )

        leftButton = createToolbarButton(context).apply {
            id = View.generateViewId()
        }
        rightButton = createToolbarButton(context).apply {
            id = View.generateViewId()
        }

        addView(
            leftButton,
            LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                Gravity.START or Gravity.CENTER_VERTICAL
            ).apply {
                val margin = Resources.dp(context, 2f).toInt()
                setMargins(margin, margin, margin, margin)
            }
        )
        addView(
            rightButton,
            LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                Gravity.END or Gravity.CENTER_VERTICAL
            ).apply {
                val margin = Resources.dp(context, 2f).toInt()
                setMargins(margin, margin, margin, margin)
            }
        )

        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.Toolbar, 0, 0)
        title.text = a.getString(R.styleable.Toolbar_title) ?: ""
        subtitle.text = a.getString(R.styleable.Toolbar_subtitle) ?: ""

        subtitle.isVisible = a.getBoolean(R.styleable.Toolbar_showSubtitle, true)

        val leftIcon = a.getResourceId(R.styleable.Toolbar_leftButtonIcon, -1)
        val rightIcon = a.getResourceId(R.styleable.Toolbar_rightButtonIcon, -1)

        if (leftIcon != -1) {
            leftButton.isVisible = true
            leftButton.setIconResource(leftIcon)
        }

        if (rightIcon != -1) {
            rightButton.isVisible = true
            rightButton.setIconResource(rightIcon)
        }

        val flattenQuickActions =
            a.getBoolean(R.styleable.Toolbar_flattenButtons, false)
        if (flattenQuickActions) {
            rightButton.flatten()
            leftButton.flatten()
        }

        a.recycle()
    }

    private fun createToolbarButton(context: Context): MaterialButton {
        return MaterialButton(
            ContextThemeWrapper(context, R.style.Widget_App_QuickActionButton)
        ).apply {
            isCheckable = true
            isToggleCheckedStateOnClick = false
            isVisible = false
        }
    }

}
