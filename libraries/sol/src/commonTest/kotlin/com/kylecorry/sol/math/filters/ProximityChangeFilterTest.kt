package com.kylecorry.sol.math.filters

import kotlin.test.Test

import assertk.assertThat
import assertk.assertions.isEqualTo

class ProximityChangeFilterTest {
    @Test
    fun filter() {
        val list = listOf(1f, 2f, 4f, 5f, 6f, 7f, 20f)
        val threshold = 2f
        val filter = ProximityChangeFilter(threshold)
        val expected = listOf(1f, 1f, 4f, 4f, 6f, 6f, 20f)

        val actual = list.map { filter.filter(it) }

        assertThat(actual).isEqualTo(expected)
    }
}