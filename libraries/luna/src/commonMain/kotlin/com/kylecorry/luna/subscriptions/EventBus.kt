package com.kylecorry.luna.subscriptions

import com.kylecorry.luna.subscriptions.generic.Subscription
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

class EventBus<T> {
    private val subscriptions = mutableMapOf<String, Subscription<T>>()
    private val subscriptionLock = SynchronizedObject()

    fun publish(topicId: String, value: T) {
        val subscription = synchronized(subscriptionLock) {
            subscriptions[topicId]
        }
        subscription?.publish(value)
    }

    fun subscribe(topicId: String, callback: suspend (T) -> Unit) {
        synchronized(subscriptionLock) {
            val topic = subscriptions.getOrPut(topicId) {
                Subscription()
            }
            topic.subscribe(callback)
        }
    }

    fun unsubscribe(topicId: String, callback: suspend (T) -> Unit) {
        synchronized(subscriptionLock) {
            val topic = subscriptions[topicId]
            topic?.unsubscribe(callback)
        }
    }
}
