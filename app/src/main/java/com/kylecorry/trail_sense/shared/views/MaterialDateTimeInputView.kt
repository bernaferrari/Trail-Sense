package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.sol.time.Time.toZonedDateTime
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import java.time.LocalDateTime

class MaterialDateTimeInputView(context: Context, attrs: AttributeSet?) :
    FrameLayout(context, attrs) {

    private val edittext: TextInputEditText
    private val holder: TextInputLayout
    private var listener: ((LocalDateTime?) -> Unit)? = null

    var datetime: LocalDateTime = LocalDateTime.now()
        private set

    init {
        holder = TextInputLayout(context).apply {
            id = R.id.datetime_input_holder
        }
        edittext = TextInputEditText(context).apply {
            id = R.id.datetime_input
            inputType = InputType.TYPE_NULL or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            isCursorVisible = false
            isFocusable = false
        }
        holder.addView(
            edittext,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        addView(
            holder,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
        )

        setValue(datetime)

        edittext.setOnClickListener {
            val prefs = DependencyRegistry.get<UserPreferences>()
            val supportFragmentManager = (context as? AppCompatActivity)?.supportFragmentManager
            if (supportFragmentManager != null) {
                CustomUiUtils.pickMaterialDatetime(
                    supportFragmentManager,
                    prefs.use24HourTime,
                    datetime
                ) {
                    setValue(it ?: return@pickMaterialDatetime)
                }
            } else {
                CustomUiUtils.pickDatetime(context, prefs.use24HourTime, datetime) {
                    setValue(it ?: return@pickDatetime)
                }
            }
        }
    }

    fun setValue(datetime: LocalDateTime) {
        this.datetime = datetime
        listener?.invoke(datetime)
        val formatter = DependencyRegistry.get<FormatService>()
        edittext.setText(formatter.formatDateTime(datetime.toZonedDateTime(), relative = true))
    }

    fun setHint(hint: CharSequence?) {
        holder.hint = hint
    }

    fun setOnItemSelectedListener(listener: (LocalDateTime?) -> Unit) {
        this.listener = listener
    }

}
