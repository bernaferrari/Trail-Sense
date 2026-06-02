package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.updateLayoutParams
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R

class FloatingActionButtonMenuItem(context: Context, attrs: AttributeSet?) :
    FrameLayout(context, attrs) {

    private var textView: TextView
    private var fab: FloatingActionButton

    init {
        val root = LinearLayout(context).apply {
            clipChildren = false
            clipToPadding = false
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
            setPadding(
                Resources.dp(context, 6f).toInt(),
                Resources.dp(context, 6f).toInt(),
                Resources.dp(context, 6f).toInt(),
                Resources.dp(context, 16f).toInt()
            )
        }
        addView(
            root,
            LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            )
        )

        textView = TextView(context).apply {
            id = R.id.fab_text
            background = AppCompatResources.getDrawable(context, R.drawable.rounded_rectangle)
            backgroundTintList = ColorStateList.valueOf(
                Resources.getAndroidColorAttr(context, android.R.attr.colorBackgroundFloating)
            )
            setPadding(
                Resources.dp(context, 8f).toInt(),
                Resources.dp(context, 4f).toInt(),
                Resources.dp(context, 8f).toInt(),
                Resources.dp(context, 4f).toInt()
            )
        }
        root.addView(
            textView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = Resources.dp(context, 16f).toInt()
            }
        )

        fab = FloatingActionButton(
            context,
            null,
            com.google.android.material.R.attr.floatingActionButtonSmallStyle
        ).apply {
            id = R.id.fab
            isClickable = true
            isFocusable = true
        }
        root.addView(
            fab,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.FloatingActionButtonMenuItem,
            0,
            0
        )
        setImageResource(
            a.getResourceId(
                R.styleable.FloatingActionButtonMenuItem_android_src,
                R.drawable.ic_add
            )
        )
        textView.text = a.getString(R.styleable.FloatingActionButtonMenuItem_android_text)
        a.recycle()
        layoutParams = generateDefaultLayoutParams()
        updateLayoutParams {
            width = ViewGroup.LayoutParams.WRAP_CONTENT
            height = ViewGroup.LayoutParams.WRAP_CONTENT
        }

    }

    fun setImageResource(@DrawableRes resId: Int) {
        fab.setImageResource(resId)
    }

    fun setImageDrawable(image: Drawable?) {
        fab.setImageDrawable(image)
    }

    fun setText(text: String?) {
        textView.text = text
    }

    fun setItemOnClickListener(l: OnClickListener) {
        setOnClickListener(l)
        fab.setOnClickListener(l)
    }

}
