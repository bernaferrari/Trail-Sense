package com.kylecorry.sol.science.astronomy.stars

import com.kylecorry.sol.science.astronomy.units.EquatorialCoordinate

/**
 * This research has made use of the SIMBAD database,
 * operated at CDS, Strasbourg, France
 *
 * 2000,A&AS,143,9 , "The SIMBAD astronomical database", Wenger et al.
 *
 * https://doi.org/10.1051/aas:2000332
 */

// http://cdsportal.u-strasbg.fr/?target=Rigel
// https://en.wikipedia.org/wiki/List_of_brightest_stars
// https://en.wikipedia.org/wiki/List_of_stars_for_navigation

public class Star internal constructor(
    public val hipDesignation: Int,
    public val name: String,
    internal val coordinate: EquatorialCoordinate,
    public val magnitude: Float,
    public val motion: ProperMotion,
    public val colorIndexBV: Float
)

/**
 * Proper motion of a star in degrees per year
 */
public data class ProperMotion(val declination: Double, val rightAscension: Double)
