package com.kylecorry.andromeda.forms

public interface FormValue<T> {
    public val id: String
    public var value: T
}

public class MutableFormValue<T>(override val id: String, override var value: T) : FormValue<T>
