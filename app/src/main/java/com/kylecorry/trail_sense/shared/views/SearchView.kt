package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.trail_sense.R

class SearchView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    private val searchEditText: TextInputEditText
    private var onSearch: ((String) -> Unit)? = null

    var query: String
        get() = searchEditText.text.toString()
        set(value) {
            searchEditText.setText(value)
        }

    init {
        background = AppCompatResources.getDrawable(context, R.drawable.rounded_rectangle)
        backgroundTintList = ColorStateList.valueOf(
            Resources.getAndroidColorAttr(context, android.R.attr.colorBackgroundFloating)
        )
        elevation = Resources.dp(context, 2f)
        setPadding(
            Resources.dp(context, 2f).toInt(),
            Resources.dp(context, 2f).toInt(),
            Resources.dp(context, 2f).toInt(),
            Resources.dp(context, 2f).toInt()
        )

        val inputLayout = TextInputLayout(context).apply {
            id = R.id.search_view_text_input_layout
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_NONE
            endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
            isHintEnabled = false
            startIconDrawable = AppCompatResources.getDrawable(context, R.drawable.ic_search)
        }

        searchEditText = TextInputEditText(context).apply {
            id = R.id.search_view_edit_text
            hint = context.getString(R.string.search)
        }
        inputLayout.addView(
            searchEditText,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        addView(
            inputLayout,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
        )

        searchEditText.addTextChangedListener {
            onSearch?.invoke(searchEditText.text.toString())
        }
    }

    fun setOnSearchListener(listener: ((String) -> Unit)?) {
        onSearch = listener
    }

    fun setCursorPosition(position: Int) {
        searchEditText.setSelection(position)
    }
}
