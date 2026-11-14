package com.kylecorry.sol.science.astronomy.stars

public class Constellation(public val name: String, public val lines: List<List<Int>>) {
    public val allStarIds: Set<Int> = lines.flatten().toSet()

    public val starEdges: List<Pair<Star, Star>>
        get() {
            return lines.flatMap { line ->
                line.zipWithNext { a, b ->
                    Pair(star(a), star(b))
                }
            }
        }

    public fun containsStar(star: Star): Boolean {
        return allStarIds.contains(star.hipDesignation)
    }
}

private fun star(hipDesignation: Int): Star {
    return STAR_CATALOG_BY_HIP[hipDesignation] ?: error("Hip designation $hipDesignation not found")
}
