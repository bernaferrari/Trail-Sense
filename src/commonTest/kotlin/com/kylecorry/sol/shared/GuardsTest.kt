package com.kylecorry.sol.shared

import kotlin.test.Test
import kotlin.test.assertFailsWith

class GuardsTest {

    @Test
    fun isPositive() {
        assertFailsWith<IllegalArgumentException> {
            Guards.isPositive(-1)
        }
        assertFailsWith<IllegalArgumentException> {
            Guards.isPositive(0)
        }
        Guards.isPositive(1)
    }

    @Test
    fun isNotEmpty() {
        assertFailsWith<IllegalArgumentException> {
            Guards.isNotEmpty(listOf<Int>())
        }
        Guards.isNotEmpty(listOf(1))
    }

    @Test
    fun areSameSize() {
        assertFailsWith<IllegalArgumentException> {
            Guards.areSameSize(listOf(1), listOf(1, 2))
        }
        Guards.areSameSize(listOf(1), listOf(2))
    }
}