package com.kylecorry.andromeda.core.cache

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlin.reflect.KClass

object DependencyRegistry {
    private val lock = SynchronizedObject()
    private val singletons = mutableMapOf<KClass<*>, Lazy<Any>>()
    private val transients = mutableMapOf<KClass<*>, () -> Any>()

    inline fun <reified T : Any> addSingleton(service: T) = addSingleton(T::class, service)
    fun <T : Any> addSingleton(type: KClass<T>, service: T) {
        synchronized(lock) { singletons[type] = lazyOf(service) }
    }
    fun <T : Any> addSingleton(type: KClass<T>, service: () -> T) {
        synchronized(lock) { singletons[type] = lazy { service() } }
    }
    inline fun <reified T : Any> addTransient(noinline service: () -> T) = addTransient(T::class, service)
    fun <T : Any> addTransient(type: KClass<T>, service: () -> T) {
        synchronized(lock) { transients[type] = service }
    }
    inline fun <reified T : Any> remove() = remove(T::class)
    fun <T : Any> remove(type: KClass<T>) {
        synchronized(lock) { singletons.remove(type); transients.remove(type) }
    }
    inline fun <reified T : Any> get(): T = get(T::class)
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(type: KClass<T>): T {
        val (singleton, transient) = synchronized(lock) { singletons[type] to transients[type] }
        return (singleton?.value ?: transient?.invoke()) as? T
            ?: throw IllegalStateException("No service registered for ${type.qualifiedName}")
    }
}
