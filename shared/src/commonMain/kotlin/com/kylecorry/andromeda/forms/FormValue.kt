package com.kylecorry.andromeda.forms

interface FormValue<T> {
    val id: String
    var value: T
}

class MutableFormValue<T>(override val id: String, override var value: T) : FormValue<T>
