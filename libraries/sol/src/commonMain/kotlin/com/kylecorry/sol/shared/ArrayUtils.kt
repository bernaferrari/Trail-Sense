package com.kylecorry.sol.shared

public object ArrayUtils {

    public fun <T> Array<T>.swap(i: Int, j: Int) {
        val temp = this[i]
        this[i] = this[j]
        this[j] = temp
    }

}
