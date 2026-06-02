package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.FormatService
import java.time.Duration

class PlayBarView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    private val playBarTitle: DataPointView
    private val playButton: ImageButton

    var title: String
        get() = playBarTitle.title
        set(value) {
            playBarTitle.title = value
        }

    var subtitle: String
        get() = playBarTitle.description
        set(value) {
            useDefaultSubtitle = false
            playBarTitle.description = value
        }

    private val formatter = FormatService.getInstance(context)

    private var useDefaultSubtitle: Boolean = true

    init {
        val playBar = LinearLayout(context).apply {
            id = R.id.play_bar
            background = AppCompatResources.getDrawable(context, R.drawable.rounded_rectangle)
            backgroundTintList = android.content.res.ColorStateList.valueOf(
                Resources.getAndroidColorAttr(
                    context,
                    com.google.android.material.R.attr.colorSurfaceContainer
                )
            )
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
            setPadding(
                Resources.dp(context, 16f).toInt(),
                Resources.dp(context, 16f).toInt(),
                Resources.dp(context, 16f).toInt(),
                Resources.dp(context, 16f).toInt()
            )
        }
        addView(
            playBar,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
        )

        playBarTitle = DataPointView(context, null).apply {
            id = R.id.play_bar_title
        }
        playBar.addView(
            playBarTitle,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        playButton = ImageButton(context).apply {
            id = R.id.play_btn
            background = AppCompatResources.getDrawable(context, R.drawable.rounded_rectangle)
            elevation = Resources.dp(context, 2f)
        }
        playBar.addView(
            playButton,
            LinearLayout.LayoutParams(
                Resources.dp(context, 40f).toInt(),
                Resources.dp(context, 40f).toInt()
            ).apply {
                setMargins(
                    Resources.dp(context, 2f).toInt(),
                    Resources.dp(context, 2f).toInt(),
                    Resources.dp(context, 2f).toInt(),
                    Resources.dp(context, 2f).toInt()
                )
            }
        )

        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.PlayBarView, 0, 0)
        val icon = a.getResourceId(R.styleable.PlayBarView_playBarIcon, -1)
        setImageResource(if (icon == -1) null else icon)
        title = a.getString(R.styleable.PlayBarView_playBarTitle) ?: ""
        val subtitleText = a.getString(R.styleable.PlayBarView_playBarSubtitle) ?: ""
        if (subtitleText.isNotEmpty()) {
            subtitle = subtitleText
        }
        a.recycle()
        setState(false)
    }

    fun setImageResource(@DrawableRes res: Int?) {
        playBarTitle.setImageResource(res)
    }

    fun setShowSubtitle(showSubtitle: Boolean) {
        playBarTitle.setShowDescription(showSubtitle)
    }

    fun setState(state: FeatureState, frequency: Duration? = null) {
        setState(state == FeatureState.On, frequency)
    }

    fun setState(isOn: Boolean, frequency: Duration? = null) {
        playButton.setImageResource(if (isOn) R.drawable.ic_baseline_stop_24 else R.drawable.ic_baseline_play_arrow_24)
        if (useDefaultSubtitle) {
            playBarTitle.description = if (isOn) {
                context.getString(R.string.on)
            } else {
                context.getString(R.string.off)
            } + (frequency?.let {
                " ${context.getString(R.string.dash)} ${
                    formatter.formatDuration(
                        frequency,
                        short = true,
                        includeSeconds = true
                    )
                }"
            } ?: "")
        }
        CustomUiUtils.setButtonState(playButton, true)
    }

    fun setOnSubtitleClickListener(action: (() -> Unit)?) {
        playBarTitle.setOnDescriptionClickListener(action)
    }

    fun setOnPlayButtonClickListener(action: () -> Unit) {
        playButton.setOnClickListener { action() }
    }

}
