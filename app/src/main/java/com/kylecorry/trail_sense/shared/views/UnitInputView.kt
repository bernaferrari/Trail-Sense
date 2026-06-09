package com.kylecorry.trail_sense.shared.views

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.luna.text.toDoubleCompat
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeRootView

class UnitInputView<Units : Enum<*>>(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private var _unit: Units? = null
    private var _amount: Number? = null

    override fun isEnabled(): Boolean {
        return amountEdit.isEnabled
    }

    override fun setEnabled(enabled: Boolean) {
        amountEdit.isEnabled = enabled
        unitBtn.isEnabled = enabled
        unitBtnState.isEnabled = enabled
    }

    var units: List<DisplayUnit<Units>> = listOf()
        set(value) {
            field = value
            val unit = this.unit
            if (unit != null && value.none { it.unit == unit }) {
                this.unit = null
            }
        }

    var unit: Units?
        get() = _unit
        set(value) {
            val changed = _unit != value
            _unit = value
            if (changed) {
                setSelectedUnitText(value)
                onChange?.invoke(amount, unit)
            }
        }

    var amount: Number?
        get() = _amount
        set(value) {
            val changed = value != _amount
            _amount = value
            if (changed) {
                setAmountEditText(value)
                onChange?.invoke(amount, unit)
            }
        }

    var hint: CharSequence?
        get() = amountEditHolder.hint
        set(value) {
            amountEditHolder.hint = value
        }

    var allowNegative: Boolean = true
        set(value) {
            field = value
            amountEdit.inputType = InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_FLAG_DECIMAL or
                    if (value) InputType.TYPE_NUMBER_FLAG_SIGNED else 0
        }

    private var unitPickerTitle: CharSequence = ""


    var onChange: ((amount: Number?, unit: Units?) -> Unit)? = null

    private var amountEdit: TextInputEditText
    private var amountEditHolder: TextInputLayout
    private var unitBtn: View
    private var unitBtnState: UnitPickerButtonState

    private fun setSelectedUnitText(unit: Units?) {
        if (unit != null) {
            val displayUnit = units.firstOrNull { it.unit == unit }
            if (displayUnit == null) {
                _unit = null
                unitBtnState.text = ""
            } else {
                unitBtnState.text = displayUnit.shortName
            }
        } else {
            unitBtnState.text = ""
        }
    }

    private fun setAmountEditText(amount: Number?) {
        amountEdit.setText(amount.toString())
    }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        amountEditHolder = TextInputLayout(context).apply {
            id = R.id.amount_holder
        }
        amountEdit = TextInputEditText(context).apply {
            id = R.id.amount
            inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }
        amountEditHolder.addView(
            amountEdit,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        addView(
            amountEditHolder,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        unitBtnState = UnitPickerButtonState()
        unitBtn = TrailSenseComposeRootView(
            context,
            ViewCompositionStrategy.DisposeOnDetachedFromWindow
        ) {
            unitBtnState.Content()
        }.apply {
            id = R.id.units
        }
        addView(
            unitBtn,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = Resources.dp(context, 8f).toInt()
            }
        )
        allowNegative = true

        amountEdit.addTextChangedListener {
            _amount = it?.toString()?.toDoubleCompat()
            onChange?.invoke(amount, unit)
        }

        unitBtnState.onClick = {
            Pickers.item(
                getContext(),
                unitPickerTitle,
                units.map { it.longName },
                units.indexOfFirst { it.unit == unit }) { idx ->
                if (idx != null) {
                    unit = units[idx].unit
                }
            }
        }
    }

    data class DisplayUnit<Units : Enum<*>>(
        val unit: Units,
        val shortName: String,
        val longName: String
    )

}
