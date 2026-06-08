package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kylecorry.trail_sense.R

class TextInputView(context: Context, attrs: AttributeSet?) :
    ConstraintLayout(context, attrs) {

    private val edittext: TextInputEditText
    private val holder: TextInputLayout

    init {
        holder = TextInputLayout(context).apply {
            id = R.id.text_input_holder
        }
        edittext = TextInputEditText(context).apply {
            id = R.id.text_input
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_NONE
            inputType = InputType.TYPE_CLASS_TEXT
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
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                startToStart = LayoutParams.PARENT_ID
                endToEnd = LayoutParams.PARENT_ID
                topToTop = LayoutParams.PARENT_ID
                bottomToBottom = LayoutParams.PARENT_ID
            }
        )

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.TextInputView)
        try {
            val hint = typedArray.getString(R.styleable.TextInputView_android_hint)
            setHint(hint)
        } finally {
            typedArray.recycle()
        }
    }

    fun setHint(hint: String?) {
        holder.hint = hint
    }

    fun setInputType(inputType: Int) {
        edittext.inputType = inputType
    }

    fun setError(error: CharSequence?) {
        holder.error = error
    }

    fun setOnTextChangeListener(callback: (text: CharSequence?) -> Unit) {
        edittext.addTextChangedListener { text ->
            callback(text)
        }
    }

    var text: CharSequence?
        get() = edittext.text
        set(value) {
            edittext.setText(value)
        }
}
