package com.kylecorry.sol.science.shared

public data class DichotomousKey<T>(
    val label: String,
    val yes: DichotomousKey<T>?,
    val no: DichotomousKey<T>?,
    val value: T?
) {
    public companion object {
        public fun <T> question(
            label: String,
            yes: DichotomousKey<T>,
            no: DichotomousKey<T>
        ): DichotomousKey<T> {
            return DichotomousKey(label, yes, no, null)
        }

        public fun <T> answer(label: String, value: T): DichotomousKey<T> {
            return DichotomousKey(label, null, null, value)
        }

        public fun answer(value: String): DichotomousKey<String> {
            return answer(value, value)
        }
    }
}
