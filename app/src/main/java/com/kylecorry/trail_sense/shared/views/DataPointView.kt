package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.core.ui.setTextDistinct
import com.kylecorry.trail_sense.R

class DataPointView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    private val dataPoint: ConstraintLayout
    private val dataPointImage: ImageView
    private val dataPointTitle: TextView
    private val dataPointDescription: TextView
    private var tint: Int

    var title: String
        get() = dataPointTitle.text.toString()
        set(value) {
            dataPointTitle.setTextDistinct(value)
        }

    var description: String
        get() = dataPointDescription.text.toString()
        set(value) {
            dataPointDescription.setTextDistinct(value)
        }

    init {
        dataPoint = ConstraintLayout(context).apply {
            id = R.id.data_point
        }
        addView(
            dataPoint,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        )

        dataPointImage = ImageView(context).apply {
            id = R.id.data_point_image
            alpha = 0.75f
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        dataPoint.addView(
            dataPointImage,
            ConstraintLayout.LayoutParams(
                Resources.dp(context, 20f).toInt(),
                Resources.dp(context, 20f).toInt()
            ).apply {
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            }
        )

        dataPointTitle = TextView(context).apply {
            id = R.id.data_point_title
        }
        dataPoint.addView(
            dataPointTitle,
            ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = Resources.dp(context, 16f).toInt()
                goneStartMargin = 0
                startToEnd = R.id.data_point_image
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            }
        )

        dataPointDescription = TextView(context).apply {
            id = R.id.data_point_desc
        }
        dataPoint.addView(
            dataPointDescription,
            ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                startToStart = R.id.data_point_title
                topToBottom = R.id.data_point_title
            }
        )

        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.DataPointView, 0, 0)
        tint = a.getColor(
            R.styleable.DataPointView_dataPointTint,
            Resources.androidTextColorPrimary(context)
        )
        val icon = a.getResourceId(R.styleable.DataPointView_dataPointIcon, -1)
        setImageResource(if (icon == -1) null else icon)
        title = a.getString(R.styleable.DataPointView_dataPointText) ?: ""
        description = a.getString(R.styleable.DataPointView_dataPointDescription) ?: ""
        dataPoint.layoutParams =
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).also {
                it.gravity = a.getInt(R.styleable.DataPointView_android_gravity, Gravity.START)
            }
        a.recycle()
    }


    fun setImageResource(@DrawableRes res: Int?) {
        if (res != null) {
            dataPointImage.setImageResource(res)
            Colors.setImageColor(
                dataPointImage,
                if (tint == -1) Resources.androidTextColorPrimary(context) else tint
            )
        }
        dataPointImage.isVisible = res != null
    }

    fun setContentGravity(gravity: Int) {
        dataPoint.layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT
        ).also {
            it.gravity = gravity
        }
    }

    fun setShowDescription(showDescription: Boolean) {
        dataPointDescription.isVisible = showDescription
    }

    fun setOnDescriptionClickListener(listener: (() -> Unit)?) {
        if (listener != null) {
            dataPointDescription.setOnClickListener { listener() }
        } else {
            dataPointDescription.setOnClickListener(null)
        }
    }

}
