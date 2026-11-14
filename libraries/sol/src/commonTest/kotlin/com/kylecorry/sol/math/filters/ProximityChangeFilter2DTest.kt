package com.kylecorry.sol.math.filters

import kotlin.test.Test

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.kylecorry.sol.math.Vector2

class ProximityChangeFilter2DTest {

    @Test
    fun filter() {
        val list = listOf(1f, 2f, 4f, 5f, 6f, 7f, 20f).mapIndexed { index, fl -> Vector2(index.toFloat(), fl) }
        val threshold = 2f
        val filter = ProximityChangeFilter2D(threshold)
        val expected = listOf(
            Vector2(0f, 1f),
            Vector2(0f, 1f),
            Vector2(2f, 4f),
            Vector2(2f, 4f),
            Vector2(4f, 6f),
            Vector2(4f, 6f),
            Vector2(6f, 20f)
        )

        val actual = filter.filter(list)

        assertThat(actual).isEqualTo(expected)
    }
}