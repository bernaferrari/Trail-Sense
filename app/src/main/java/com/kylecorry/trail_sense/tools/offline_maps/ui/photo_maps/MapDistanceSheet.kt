package com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.core.ui.flatten
import com.kylecorry.andromeda.views.toolbar.Toolbar
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units


class MapDistanceSheet(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    private val formatter by lazy { FormatService.getInstance(context) }

    var cancelListener: (() -> Unit)? = null
    var undoListener: (() -> Unit)? = null
    var createPathListener: (() -> Unit)? = null

    private val toolbar: Toolbar

    init {
        val root = LinearLayout(context).apply {
            background = ColorDrawable(
                Resources.getAndroidColorAttr(
                    context,
                    com.google.android.material.R.attr.colorSurfaceContainer
                )
            )
            orientation = LinearLayout.VERTICAL
        }
        addView(
            root,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
        )

        toolbar = Toolbar(context, null).apply {
            id = R.id.map_distance_title
            leftButton.setImageResource(R.drawable.ic_undo)
            rightButton.setImageResource(R.drawable.ic_cancel)
            title.text = context.getString(R.string.distance)
            leftButton.flatten()
            rightButton.flatten()
        }
        root.addView(
            toolbar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        toolbar.rightButton.setOnClickListener {
            cancelListener?.invoke()
        }
        toolbar.leftButton.setOnClickListener {
            undoListener?.invoke()
        }

        val createPathButton = Button(context).apply {
            id = R.id.create_path_btn
            text = context.getString(R.string.create_path)
        }
        root.addView(
            createPathButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
                bottomMargin = Resources.dp(context, 8f).toInt()
            }
        )

        createPathButton.setOnClickListener {
            createPathListener?.invoke()
        }

    }

    fun setDistance(distance: Distance) {
        toolbar.subtitle.text =
            formatter.formatDistance(distance, Units.getDecimalPlaces(distance.units), false)
    }

    fun show() {
        isVisible = true
    }

    fun hide() {
        isVisible = false
    }
}
