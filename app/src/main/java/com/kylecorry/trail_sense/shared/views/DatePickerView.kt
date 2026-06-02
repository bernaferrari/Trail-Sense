package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.pickers.material.AndromedaDayViewDecorator
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import java.time.LocalDate

class DatePickerView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    var date: LocalDate = LocalDate.now()
        set(value) {
            val changed = field != value
            field = value
            if (changed) {
                calendar.text = formatter.formatRelativeDate(value)
                onDateChange(value)
            }
        }

    var searchEnabled: Boolean = false
        set(value) {
            field = value
            search.isVisible = value
        }

    private val formatter = FormatService.getInstance(context)
    private val calendar: MaterialButton
    private val search: MaterialButton
    private val next: MaterialButton
    private val previous: MaterialButton
    private var onDateChange: (LocalDate) -> Unit = {}
    private var onCalendarLongPress: () -> Unit = {
        date = LocalDate.now()
    }
    private var onSearch: () -> Unit = {}
    private var dayViewDecorator: AndromedaDayViewDecorator? = null

    init {
        val root = LinearLayout(context).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
        }
        addView(
            root,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
        )

        previous = createButton(R.id.prev_date, R.drawable.ic_keyboard_arrow_right).apply {
            rotation = 180f
        }
        root.addView(previous)

        root.addView(createSpacer(context))

        dateText = TextView(context).apply {
            id = R.id.date
        }
        root.addView(
            dateText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        calendar = createButton(R.id.date_btn, R.drawable.ic_date)
        root.addView(
            calendar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = Resources.dp(context, 16f).toInt()
            }
        )

        search = createButton(R.id.search_btn, R.drawable.ic_search).apply {
            visibility = View.GONE
        }
        root.addView(
            search,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = Resources.dp(context, 16f).toInt()
            }
        )

        root.addView(createSpacer(context))

        next = createButton(R.id.next_date, R.drawable.ic_keyboard_arrow_right)
        root.addView(next)

        calendar.text = formatter.formatRelativeDate(date)

        calendar.setOnClickListener {
            CustomUiUtils.pickDate(context, date, dayViewDecorator) {
                if (it != null) {
                    date = it
                }
            }
        }

        calendar.setOnLongClickListener {
            onCalendarLongPress()
            true
        }

        next.setOnClickListener {
            date = date.plusDays(1)
        }
        CustomUiUtils.setRepeatingLongPressAction(next) {
            date = date.plusDays(1)
        }

        previous.setOnClickListener {
            date = date.minusDays(1)
        }
        CustomUiUtils.setRepeatingLongPressAction(previous) {
            date = date.minusDays(1)
        }

        search.setOnClickListener {
            onSearch()
        }

    }

    fun setOnDateChangeListener(listener: (LocalDate) -> Unit) {
        onDateChange = listener
    }

    fun setOnCalendarLongPressListener(listener: () -> Unit) {
        onCalendarLongPress = listener
    }

    fun setOnSearchListener(listener: () -> Unit) {
        onSearch = listener
    }

    fun setDayViewDecorator(decorator: AndromedaDayViewDecorator?) {
        dayViewDecorator = decorator
    }

    fun reset() {
        date = LocalDate.now()
    }

    private fun createButton(id: Int, icon: Int): ImageButton {
        return ImageButton(context).apply {
            this.id = id
            setBackgroundColor(Color.TRANSPARENT)
            setImageResource(icon)
            imageTintList = ColorStateList.valueOf(Resources.androidTextColorSecondary(context))
        }
    }

    private fun createSpacer(context: Context): View {
        return View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1f
            )
        }
    }

}
