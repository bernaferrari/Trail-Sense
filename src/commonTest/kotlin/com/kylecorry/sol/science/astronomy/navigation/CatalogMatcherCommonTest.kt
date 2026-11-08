package com.kylecorry.sol.science.astronomy.navigation

import com.kylecorry.sol.math.Vector3Precise
import com.kylecorry.sol.math.geometry.Rotation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CatalogMatcherCommonTest {
    @Test
    fun matchesCatalogVectors() {
        val directions = listOf(Vector3Precise(1.0, 0.0, 0.0), Vector3Precise(0.0, 1.0, 0.0))
        val matcher = CatalogMatcher(directions, null, 180.0, 0.1) { it }
        assertEquals(1, matcher.getPairs(90.0, 0.1).size)
        val rotation = assertNotNull(Rotation.triad(directions[0], directions[1], directions[0], directions[1]))
        assertEquals(listOf(0, 1), matcher.associate(directions, rotation).map { it.catalogIndex })
    }
}
