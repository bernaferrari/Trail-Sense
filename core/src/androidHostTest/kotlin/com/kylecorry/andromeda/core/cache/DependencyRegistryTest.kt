package com.kylecorry.andromeda.core.cache

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class DependencyRegistryTest {

    @AfterEach
    fun cleanup() {
        DependencyRegistry.remove(TestService::class)
        DependencyRegistry.remove(OtherService::class)
    }

    @Test
    fun addSingletonAndGetService() {
        val service = TestService("test")

        DependencyRegistry.addSingleton(TestService::class, service)

        assertSame(service, DependencyRegistry.get(TestService::class))
    }

    @Test
    fun addSingletonAndGetServiceUsingReifiedType() {
        val service = TestService("test")

        DependencyRegistry.addSingleton(service)

        assertSame(service, DependencyRegistry.get<TestService>())
    }

    @Test
    fun addSingletonLazyServiceOnlyCreatesItOnce() {
        var calls = 0
        DependencyRegistry.addSingleton(TestService::class) {
            calls++
            TestService("test")
        }

        val first = DependencyRegistry.get(TestService::class)
        val second = DependencyRegistry.get(TestService::class)

        assertSame(first, second)
        assertEquals("test", first.value)
        assertEquals(1, calls)
    }

    @Test
    fun lazyServiceCanGetAnotherServiceDuringInstantiation() {
        DependencyRegistry.addSingleton(TestService::class) {
            TestService(DependencyRegistry.get<OtherService>().value)
        }
        DependencyRegistry.addSingleton(OtherService("other"))

        val service = DependencyRegistry.get<TestService>()

        assertEquals("other", service.value)
    }

    @Test
    fun addTransientAndGetService() {
        var calls = 0
        DependencyRegistry.addTransient(TestService::class) {
            calls++
            TestService("test $calls")
        }

        val first = DependencyRegistry.get(TestService::class)
        val second = DependencyRegistry.get(TestService::class)

        assertEquals("test 1", first.value)
        assertEquals("test 2", second.value)
        assertEquals(2, calls)
    }

    @Test
    fun addTransientAndGetServiceUsingReifiedType() {
        DependencyRegistry.addTransient {
            TestService("test")
        }

        assertEquals("test", DependencyRegistry.get<TestService>().value)
    }

    @Test
    fun getUsesSingletonBeforeTransient() {
        val singleton = TestService("singleton")
        DependencyRegistry.addTransient(TestService::class) {
            TestService("transient")
        }
        DependencyRegistry.addSingleton(TestService::class, singleton)

        val service = DependencyRegistry.get<TestService>()

        assertSame(singleton, service)
    }

    @Test
    fun addSingletonReplacesExistingService() {
        val first = TestService("first")
        val second = TestService("second")

        DependencyRegistry.addSingleton(TestService::class, first)
        DependencyRegistry.addSingleton(TestService::class, second)

        assertSame(second, DependencyRegistry.get(TestService::class))
    }

    @Test
    fun removeRemovesService() {
        DependencyRegistry.addSingleton(TestService::class, TestService("test"))

        DependencyRegistry.remove(TestService::class)

        assertThrows(IllegalStateException::class.java) {
            DependencyRegistry.get(TestService::class)
        }
    }

    @Test
    fun removeRemovesServiceUsingReifiedType() {
        DependencyRegistry.addSingleton(OtherService("test"))

        DependencyRegistry.remove<OtherService>()

        assertThrows(IllegalStateException::class.java) {
            DependencyRegistry.get<OtherService>()
        }
    }

    @Test
    fun getThrowsWhenServiceIsNotRegistered() {
        val exception = assertThrows(IllegalStateException::class.java) {
            DependencyRegistry.get(TestService::class)
        }

        assertEquals(
            "No service registered for ${TestService::class.qualifiedName}",
            exception.message
        )
    }

    private data class TestService(val value: String)

    private data class OtherService(val value: String)
}
