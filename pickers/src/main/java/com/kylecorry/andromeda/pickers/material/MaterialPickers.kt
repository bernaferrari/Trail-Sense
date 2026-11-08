package com.kylecorry.andromeda.pickers.material

import androidx.fragment.app.FragmentManager
import com.google.android.material.datepicker.MaterialDatePicker
import java.time.LocalDate
import java.time.ZoneId

object MaterialPickers {

    fun date(
        fragmentManager: FragmentManager,
        default: LocalDate = LocalDate.now(),
        dayViewDecorator: AndromedaDayViewDecorator? = null,
        onDatePick: (date: LocalDate?) -> Unit
    ) {
        val builder = MaterialDatePicker.Builder.datePicker()
            .setSelection(default.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000)

        if (dayViewDecorator != null) {
            builder.setDayViewDecorator(dayViewDecorator.toMaterialDayViewDecorator())
        }

        val picker = builder.build()

        picker.addOnPositiveButtonClickListener {
            onDatePick(LocalDate.ofEpochDay(it / 86400000))
        }

        picker.addOnCancelListener { onDatePick(null) }
        picker.addOnNegativeButtonClickListener { onDatePick(null) }

        picker.show(fragmentManager, picker.toString())
    }

}