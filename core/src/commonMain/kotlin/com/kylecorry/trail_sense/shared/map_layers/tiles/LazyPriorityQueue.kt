package com.kylecorry.trail_sense.shared.map_layers.tiles

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/** A thread-safe min-heap whose priorities can be recalculated after items change. */
class LazyPriorityQueue<T : Any>(initialCapacity: Int, private val comparator: Comparator<T>) {
    private val lock = SynchronizedObject()
    private val heap = ArrayList<T>(initialCapacity)
    private var prioritiesChanged = false

    fun enqueue(item: T) = synchronized(lock) {
        heap.add(item)
        if (!prioritiesChanged) siftUp(heap.lastIndex)
    }

    fun recalculatePriorities() = synchronized(lock) {
        prioritiesChanged = true
    }

    fun dequeue(count: Int = 1): List<T> = synchronized(lock) {
        require(count >= 0)
        if (prioritiesChanged) {
            for (index in heap.size / 2 - 1 downTo 0) siftDown(index)
            prioritiesChanged = false
        }
        buildList {
            repeat(minOf(count, heap.size)) {
                add(heap[0])
                val last = heap.removeAt(heap.lastIndex)
                if (heap.isNotEmpty()) {
                    heap[0] = last
                    siftDown(0)
                }
            }
        }
    }

    fun clear() = synchronized(lock) {
        heap.clear()
        prioritiesChanged = false
    }

    fun peek(): T? = synchronized(lock) {
        if (prioritiesChanged) {
            for (index in heap.size / 2 - 1 downTo 0) siftDown(index)
            prioritiesChanged = false
        }
        heap.firstOrNull()
    }

    fun count(): Int = synchronized(lock) { heap.size }

    private fun siftUp(start: Int) {
        var index = start
        while (index > 0) {
            val parent = (index - 1) / 2
            if (comparator.compare(heap[parent], heap[index]) <= 0) return
            swap(parent, index)
            index = parent
        }
    }

    private fun siftDown(start: Int) {
        var index = start
        while (true) {
            val left = 2 * index + 1
            if (left >= heap.size) return
            val right = left + 1
            val child = if (right < heap.size && comparator.compare(heap[right], heap[left]) < 0) right else left
            if (comparator.compare(heap[index], heap[child]) <= 0) return
            swap(index, child)
            index = child
        }
    }

    private fun swap(a: Int, b: Int) {
        val value = heap[a]
        heap[a] = heap[b]
        heap[b] = value
    }
}
