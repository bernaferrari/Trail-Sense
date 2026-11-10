package com.kylecorry.sol.math

import kotlin.test.Test
import kotlin.test.assertEquals
import com.kylecorry.sol.tests.arg

class RangeExtensionsTest {
    @Test
    fun mergeIntersecting() {
        provideMergeIntersectingRanges().forEach { args -> mergeIntersecting(args[0].arg<String>(), args[1].arg<List<Range<Int>>>(), args[2].arg<List<Range<Int>>>()) }
    }

    private fun mergeIntersecting(
        name: String,
        ranges: List<Range<Int>>,
        expected: List<Range<Int>>
    ) {
        assertEquals(expected, ranges.mergeIntersecting(), name)
    }

    companion object {
        fun provideMergeIntersectingRanges(): List<List<Any?>> = listOf(
            listOf(
                "empty list",
                emptyList<Range<Int>>(),
                emptyList<Range<Int>>()
            ),
            listOf(
                "single range",
                listOf(Range(1, 10)),
                listOf(Range(1, 10))
            ),
            listOf(
                "disjoint ranges",
                listOf(Range(1, 3), Range(5, 7), Range(9, 11)),
                listOf(Range(1, 3), Range(5, 7), Range(9, 11))
            ),
            listOf(
                "partially overlapping ranges",
                listOf(Range(1, 5), Range(3, 7)),
                listOf(Range(1, 7))
            ),
            listOf(
                "overlapping ranges with second start before first start",
                listOf(Range(3, 7), Range(1, 5)),
                listOf(Range(1, 7))
            ),
            listOf(
                "ranges touching at endpoint",
                listOf(Range(1, 5), Range(5, 10)),
                listOf(Range(1, 10))
            ),
            listOf(
                "range containing next range",
                listOf(Range(1, 10), Range(3, 7)),
                listOf(Range(1, 10))
            ),
            listOf(
                "ranges with equal starts",
                listOf(Range(1, 5), Range(1, 10)),
                listOf(Range(1, 10))
            ),
            listOf(
                "identical ranges",
                listOf(Range(1, 5), Range(1, 5), Range(1, 5)),
                listOf(Range(1, 5))
            ),
            listOf(
                "chain of overlapping ranges",
                listOf(Range(1, 4), Range(3, 7), Range(6, 10)),
                listOf(Range(1, 10))
            ),
            listOf(
                "contained range between overlapping ranges",
                listOf(Range(1, 10), Range(3, 5), Range(8, 15)),
                listOf(Range(1, 15))
            ),
            listOf(
                "multiple groups",
                listOf(
                    Range(1, 5),
                    Range(3, 7),
                    Range(10, 15),
                    Range(12, 20),
                    Range(25, 30)
                ),
                listOf(Range(1, 7), Range(10, 20), Range(25, 30))
            ),
            listOf(
                "negative values",
                listOf(Range(-10, -5), Range(-7, 0), Range(1, 5)),
                listOf(Range(-10, 0), Range(1, 5))
            )
        )
    }
}