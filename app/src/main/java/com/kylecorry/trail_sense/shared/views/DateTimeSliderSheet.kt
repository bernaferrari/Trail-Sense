package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.slider.LabelFormatter
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.luna.hooks.Hooks
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.UUID

class DateTimeSliderSheet(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    private val formatter = getAppService<FormatService>()
    private val hooks = Hooks(
        stateThrottleMs = 300,
        stateTriggerOnStart = false,
        stateOnChange = {
            onTimeChanged?.invoke(currentTime.toInstant())
        }
    )
    private var trigger by hooks.state("")

    var onTimeChanged: ((Instant?) -> Unit)? = null

    private val datePicker: DatePickerView
    private val timeText: TextView
    private val timeSlider: Slider

    private var currentTime: ZonedDateTime = ZonedDateTime.now()

    init {
        val root = LinearLayout(context).apply {
            background = android.graphics.drawable.ColorDrawable(
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

        datePicker = DatePickerView(context, null).apply {
            id = R.id.date_picker
        }
        root.addView(
            datePicker,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = Resources.dp(context, 16f).toInt()
                marginEnd = Resources.dp(context, 16f).toInt()
                topMargin = Resources.dp(context, 16f).toInt()
            }
        )

        timeText = TextView(context).apply {
            id = R.id.time_text
            val typedValue = TypedValue()
            if (context.theme.resolveAttribute(
                    com.google.android.material.R.attr.textAppearanceBody1,
                    typedValue,
                    true
                )
            ) {
                setTextAppearance(typedValue.resourceId)
            }
        }
        root.addView(
            timeText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                topMargin = Resources.dp(context, 16f).toInt()
            }
        )

        timeSlider = Slider(context, null).apply {
            id = R.id.time_slider
        }
        root.addView(
            timeSlider,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = Resources.dp(context, 16f).toInt()
                marginEnd = Resources.dp(context, 16f).toInt()
                bottomMargin = Resources.dp(context, 16f).toInt()
            }
        )

        timeSlider.valueFrom = 0f
        timeSlider.valueTo = Duration.ofDays(1).toMinutes().toFloat()
        timeSlider.labelBehavior = LabelFormatter.LABEL_GONE

        datePicker.setOnDateChangeListener {
            currentTime = currentTime.with(it)
            updateUI()
            notifyChanged()
        }

        timeSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val minutes = value.toInt()
                val seconds = (minutes * 60L).coerceAtMost(LocalTime.MAX.toSecondOfDay().toLong())
                val time = LocalTime.ofSecondOfDay(seconds)
                currentTime = currentTime.with(time)
                updateTimeText()
                notifyChanged()
            }
        }

        timeText.setOnClickListener {
            val prefs = UserPreferences(context)
            CustomUiUtils.pickTime(context, prefs.use24HourTime, currentTime.toLocalTime()) {
                if (it != null) {
                    currentTime = currentTime.with(it)
                    updateUI()
                    notifyChanged()
                }
            }
        }
    }

    fun setTime(instant: Instant?) {
        currentTime = instant?.toZonedDateTime() ?: ZonedDateTime.now()
        updateUI()
    }

    private fun updateUI() {
        datePicker.date = currentTime.toLocalDate()
        updateTimeText()
        timeSlider.value = (currentTime.hour * 60 + currentTime.minute).toFloat()
    }

    private fun updateTimeText() {
        timeText.text = formatter.formatTime(currentTime.toLocalTime(), includeSeconds = false)
    }

    private fun notifyChanged() {
        trigger = UUID.randomUUID().toString()
    }

    fun show() {
        hooks.startStateUpdates()
        isVisible = true
    }

    fun hide() {
        hooks.stopStateUpdates()
        isVisible = false
    }
}
