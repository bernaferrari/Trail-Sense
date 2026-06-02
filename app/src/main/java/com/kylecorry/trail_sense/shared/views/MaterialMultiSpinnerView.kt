package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.scale
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R

class MaterialMultiSpinnerView(context: Context, attrs: AttributeSet?) :
    FrameLayout(context, attrs) {

    private val edittext: TextInputEditText
    private val holder: TextInputLayout
    private var listener: ((List<Int>) -> Unit)? = null
    private var items: List<String> = listOf()
    private var descriptions: List<String> = listOf()

    var selectedItemPositions: List<Int> = emptyList()
        private set

    init {
        holder = TextInputLayout(context).apply {
            id = R.id.material_spinner_holder
        }
        edittext = TextInputEditText(context).apply {
            id = R.id.material_spinner_edittext
            inputType = InputType.TYPE_NULL or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            isCursorVisible = false
            isFocusable = false
            setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_drop_down, 0)
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

        edittext.setOnClickListener {
            val pickerItems = items.mapIndexed { index, item ->
                buildSpannedString {
                    val hasDescription = descriptions.getOrNull(index)?.isNotBlank() == true
                    if (hasDescription) {
                        bold {
                            append(item)
                        }
                    } else {
                        append(item)
                    }
                    if (hasDescription) {
                        append("\n")
                        scale(0.2f) {
                            append("\n")
                        }
                        scale(0.65f) {
                            append(descriptions[index])
                        }
                        scale(0.2f) {
                            append("\n")
                        }
                    }
                }
            }

            Pickers.items(context, holder.hint ?: "", pickerItems, selectedItemPositions) {
                it ?: return@items
                setSelection(it)
            }
        }
    }

    fun setItems(items: List<String>) {
        this.items = items
    }

    fun setDescriptions(descriptions: List<String>) {
        this.descriptions = descriptions
    }

    fun setSelection(positions: List<Int>) {
        selectedItemPositions = positions
        listener?.invoke(positions)
        val names = positions.mapNotNull { items.getOrNull(it) }
        edittext.setText(names.joinToString(", "))
    }

    fun setHint(hint: String) {
        holder.hint = hint
    }

    fun setOnSelectionChangeListener(listener: (List<Int>) -> Unit) {
        this.listener = listener
    }

}
