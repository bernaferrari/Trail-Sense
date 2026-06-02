package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.setPadding
import com.google.android.material.color.MaterialColors
import com.kylecorry.trail_sense.R

class TileButton(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {

    private var textView: TextView
    private var icon: ImageView

    private var isOn = false

    init {
        icon = ImageView(context).apply {
            id = R.id.tile_btn
            background = AppCompatResources.getDrawable(context, R.drawable.rounded_rectangle)
            cropToPadding = true
            elevation = Resources.dp(context, 2f)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(Resources.dp(context, 32f).toInt())
        }
        addView(
            icon,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            ).apply {
                setMargins(
                    Resources.dp(context, 2f).toInt(),
                    Resources.dp(context, 2f).toInt(),
                    Resources.dp(context, 2f).toInt(),
                    Resources.dp(context, 2f).toInt()
                )
                startToStart = LayoutParams.PARENT_ID
                endToEnd = LayoutParams.PARENT_ID
                topToTop = LayoutParams.PARENT_ID
                bottomToBottom = LayoutParams.PARENT_ID
            }
        )

        textView = TextView(context).apply {
            id = R.id.tile_text
            elevation = Resources.dp(context, 4f)
            isClickable = false
            isFocusable = false
            ellipsize = android.text.TextUtils.TruncateAt.END
            maxLines = 1
            textAlignment = TEXT_ALIGNMENT_CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        }
        addView(
            textView,
            LayoutParams(
                0,
                LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = Resources.dp(context, 8f).toInt()
                marginEnd = Resources.dp(context, 8f).toInt()
                bottomMargin = Resources.dp(context, 8f).toInt()
                startToStart = LayoutParams.PARENT_ID
                endToEnd = LayoutParams.PARENT_ID
                bottomToBottom = LayoutParams.PARENT_ID
            }
        )

        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.TileButton, 0, 0)
        setImageResource(a.getResourceId(R.styleable.TileButton_tileIcon, R.drawable.flashlight))
        val padding = a.getDimensionPixelSize(R.styleable.TileButton_tilePadding, -1)
        val textSize = a.getDimension(R.styleable.TileButton_tileTextSize, -1f)
        textView.text = a.getString(R.styleable.TileButton_tileText)
        a.recycle()
        if (padding != -1) {
            setTilePadding(padding)
        }
        if (textSize != -1f) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
        }
        setState(false)
    }

    fun setImageResource(@DrawableRes resId: Int) {
        icon.setImageResource(resId)
        setState(isOn)
    }

    fun setState(on: Boolean) {
        isOn = on
        val backgroundColor: Int
        val contentColor: Int
        if (on) {
            backgroundColor = getMaterialColor(com.google.android.material.R.attr.colorSecondary)
            contentColor = getMaterialColor(com.google.android.material.R.attr.colorOnSecondary)
        } else {
            backgroundColor = getMaterialColor(com.google.android.material.R.attr.colorSecondaryContainer)
            contentColor = getMaterialColor(com.google.android.material.R.attr.colorOnSecondaryContainer)
        }

        icon.backgroundTintList = ColorStateList.valueOf(backgroundColor)
        icon.imageTintList = ColorStateList.valueOf(contentColor)
        textView.setTextColor(contentColor)
    }

    fun setText(text: String?) {
        textView.text = text
    }

    fun setTilePadding(padding: Int) {
        icon.setPadding(padding)
    }

    private fun getMaterialColor(attribute: Int): Int {
        return MaterialColors.getColor(icon, attribute)
    }

}
