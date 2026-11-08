package com.kylecorry.andromeda.forms

import android.view.View

abstract class FormField<T>(
    override val id: String,
    val view: View,
) : FormValue<T> {
    abstract override var value: T
}