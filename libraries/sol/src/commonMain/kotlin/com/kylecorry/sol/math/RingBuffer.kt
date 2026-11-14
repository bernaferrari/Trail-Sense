package com.kylecorry.sol.math

public class RingBuffer<T>(public val capacity: Int) {

    private val list = mutableListOf<T>()

    public fun isFull(): Boolean {
        return list.size == capacity
    }

    public val size: Int
        get() = list.size

    public fun add(value: T): Boolean {
        if (isFull()) {
            list.removeFirstOrNull()
        }
        return list.add(value)
    }

    public fun clear() {
        list.clear()
    }

    public fun toList(): List<T> {
        return list.toList()
    }

}
