package com.kylecorry.sol.science.geography

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import com.kylecorry.sol.tests.arg

import com.kylecorry.sol.science.geography.formatting.*
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate

class CoordinateFormatterTest {

    @Test
    fun toUTM() {
        val format = UTMCoordinateFormat()
        assertEquals("19T 0282888E 4674752N", format.toString(Coordinate(42.1948, -71.6295)))
        assertEquals("14T 0328056E 5290773N", format.toString(Coordinate(47.7474, -101.2939)))
        assertEquals("13R 0393008E 3051634N", format.toString(Coordinate(27.5844, -106.0840)))
        assertEquals("21L 0359923E 9098523N", format.toString(Coordinate(-8.1534, -58.2715)))
        assertEquals("34H 0674432E 6430470N", format.toString(Coordinate(-32.2489, 22.8516)))
        assertEquals("34H 0674432E 6430470N", format.toString(Coordinate(-32.2489, 22.8516)))
        assertEquals("34D 0528288E 2071725N", format.toString(Coordinate(-71.4545, 21.7969)))
        assertEquals("40X 0545559E 9051365N", format.toString(Coordinate(81.5113, 59.7656)))
        assertEquals("17M 0784692E 9999203N", format.toString(Coordinate(-0.0072, -78.4424)))
        assertEquals("09E 0353004E 3573063N", format.toString(Coordinate(-57.9598, -131.4844)))

        // Different precisions
        assertEquals("19T 0282888E 4674752N", UTMCoordinateFormat(7).toString(Coordinate(42.1948, -71.6295)))
        assertEquals("19T 0282880E 4674750N", UTMCoordinateFormat(6).toString(Coordinate(42.1948, -71.6295)))
        assertEquals("19T 0282800E 4674700N", UTMCoordinateFormat(5).toString(Coordinate(42.1948, -71.6295)))
        assertEquals("19T 0282000E 4674000N", UTMCoordinateFormat(4).toString(Coordinate(42.1948, -71.6295)))
        assertEquals("19T 0280000E 4670000N", UTMCoordinateFormat(3).toString(Coordinate(42.1948, -71.6295)))
        assertEquals("19T 0200000E 4600000N", UTMCoordinateFormat(2).toString(Coordinate(42.1948, -71.6295)))
        assertEquals("19T 0000000E 4000000N", UTMCoordinateFormat(1).toString(Coordinate(42.1948, -71.6295)))

        // Polar zones
        assertEquals("A 1998062E 1888990N", format.toString(Coordinate(-89.0, -179.0)))
        assertEquals("A 1998062E 2111009N", format.toString(Coordinate(-89.0, -1.0)))
        assertEquals("B 2019279E 2109339N", format.toString(Coordinate(-89.0, 10.0)))
        assertEquals("B 2000000E 2111026N", format.toString(Coordinate(-89.0, 0.0)))
        assertEquals("B 2000000E 2000000N", format.toString(Coordinate(-90.0, 0.0)))
        assertEquals("B 2000000E 2000000N", format.toString(Coordinate(-90.0, -80.0)))

        assertEquals("Y 1998062E 2111009N", format.toString(Coordinate(89.0, -179.0)))
        assertEquals("Y 1998062E 1888990N", format.toString(Coordinate(89.0, -1.0)))
        assertEquals("Z 2019279E 1890660N", format.toString(Coordinate(89.0, 10.0)))
        assertEquals("Z 2000000E 1888973N", format.toString(Coordinate(89.0, 0.0)))
        assertEquals("Z 2000000E 2000000N", format.toString(Coordinate(90.0, 0.0)))
        assertEquals("Z 2000000E 2000000N", format.toString(Coordinate(90.0, -80.0)))
    }

    @Test
    fun canAddDistance() {
        val start = Coordinate(40.0, 10.0)
        val bearing = Bearing.from(100f)
        val distance = 10000.0

        val expected = Coordinate(39.984444, 10.115556)
        val actual = start.plus(distance, bearing)
        assertEquals(expected.latitude, actual.latitude, 0.01)
        assertEquals(expected.longitude, actual.longitude, 0.01)
    }

    @Test
    fun toDDM() {
        provideDDM().forEach { args -> toDDM(args[0].arg<String>(), args[1].arg<Coordinate>(), args[2].arg<Int>()) }
    }

    private fun toDDM(expected: String, coordinate: Coordinate, precision: Int) {
        assertEquals(expected, DegreesDecimalMinutesCoordinateFormat(precision).toString(coordinate))
    }

    @Test
    fun toDMS() {
        provideDMS().forEach { args -> toDMS(args[0].arg<String>(), args[1].arg<Coordinate>(), args[2].arg<Int>()) }
    }

    private fun toDMS(expected: String, coordinate: Coordinate, precision: Int) {
        assertEquals(expected, DegreesMinutesSecondsCoordinateFormat(precision).toString(coordinate))
    }

    @Test
    fun toDD() {
        provideDD().forEach { args -> toDD(args[0].arg<String>(), args[1].arg<Coordinate>(), args[2].arg<Int>()) }
    }

    private fun toDD(expected: String, coordinate: Coordinate, precision: Int) {
        assertEquals(expected, DecimalDegreesCoordinateFormat(precision).toString(coordinate))
    }

    @Test
    fun toMGRS() {
        provideMGRS().forEach { args -> toMGRS(args[0].arg<String>(), args[1].arg<Coordinate>(), args[2].arg<Int>()) }
    }

    private fun toMGRS(expected: String, coordinate: Coordinate, precision: Int) {
        assertEquals(expected, MGRSCoordinateFormat(precision).toString(coordinate))
    }

    @Test
    fun toUSNG() {
        provideUSNG().forEach { args -> toUSNG(args[0].arg<String>(), args[1].arg<Coordinate>(), args[2].arg<Int>()) }
    }

    private fun toUSNG(expected: String, coordinate: Coordinate, precision: Int) {
        assertEquals(expected, USNGCoordinateFormat(precision).toString(coordinate))
    }

    @Test
    fun toOSGB() {
        provideOSGB().forEach { args -> toOSGB(args[0].arg<String>(), args[1].arg<Coordinate>()) }
    }

    private fun toOSGB(expected: String, coordinate: Coordinate) {
        assertEquals(expected, OSGBCoordinateFormat().toString(coordinate))
    }

    @Test
    fun parse() {
        provideLocationStrings().forEach { args -> parse(args[0].arg<String>(), args[1].arg<Coordinate?>()) }
    }

    private fun parse(locationString: String, expected: Coordinate?) {
        assertCoordinatesEqual(Coordinate.parse(locationString), expected, 0.001)
    }

    private fun assertCoordinatesEqual(
        actual: Coordinate?,
        expected: Coordinate?,
        precision: Double
    ) {
        if (expected == null) {
            assertNull(actual)
            return
        }
        assertNotNull(actual)
        assertEquals(expected.latitude, actual!!.latitude, precision)
        assertEquals(expected.longitude, actual.longitude, precision)
    }

    companion object {
        fun provideDDM(): List<List<Any?>> {
            return listOf(
                listOf("10°2.1'N    77°30.5'E", Coordinate(10.03472, 77.508333), 1),
                listOf("10°2.1'S    77°30.5'E", Coordinate(-10.03472, 77.508333), 1),
                listOf("10°2.08'N    77°30.5'W", Coordinate(10.03472, -77.508333), 2),
                listOf("10°2.0832'S    77°30.5'W", Coordinate(-10.03472, -77.508333), 4),
            )
        }
        fun provideDMS(): List<List<Any?>> {
            return listOf(
                listOf("10°2'5.0\"N    77°30'30.0\"E", Coordinate(10.03472, 77.508333), 1),
                listOf("10°2'5.0\"S    77°30'30.0\"E", Coordinate(-10.03472, 77.508333), 1),
                listOf("10°2'4.99\"N    77°30'30.0\"W", Coordinate(10.03472, -77.508333), 2),
                listOf("10°7'23.16\"S    77°7'23.232\"W", Coordinate(-10.1231, -77.12312), 3),
            )
        }
        fun provideDD(): List<List<Any?>> {
            return listOf(
                listOf("42.1948°,  -71.6295°", Coordinate(42.1948, -71.6295), 4),
                listOf("-90°,  -180°", Coordinate(-90.0, -180.0), 0),
                listOf("-42.19°,  -71.63°", Coordinate(-42.1948, -71.6295), 2),
                listOf("1.2°,  1.4°", Coordinate(1.2, 1.4), 1),
            )
        }
        fun provideMGRS(): List<List<Any?>> {
            return listOf(
                listOf("27PXM 09601 05579", Coordinate(10.0, -20.0), 5),
                listOf("51DVC 65812 22723", Coordinate(-70.1, 122.1), 5),
                listOf("YZJ 98062 11010", Coordinate(89.0, -179.0), 5),

                // Different precisions
                listOf("27PXM 0960 0558", Coordinate(10.0, -20.0), 4),
                listOf("27PXM 096 056", Coordinate(10.0, -20.0), 3),
                listOf("27PXM 10 06", Coordinate(10.0, -20.0), 2),
                listOf("27PXM 1 1", Coordinate(10.0, -20.0), 1),
            )
        }
        fun provideUSNG(): List<List<Any?>> {
            return listOf(
                listOf("14S NJ 39521 41744", Coordinate(39.2240867222, -98.5421515000), 5),
                listOf("18S UJ 23246 05745", Coordinate(38.8828019136, -77.0377807680), 5),
                listOf("17R KL 70903 50491", Coordinate(27.5589270380, -83.3203125000), 5),
                listOf("05Q KC 72828 79387", Coordinate(20.5998824479, -155.1796880364), 5),
            )
        }
        fun provideOSGB(): List<List<Any?>> {
            return listOf(
                listOf("TG 51409 13177", Coordinate(52.657977, 1.716038)),
                listOf("OR 96706 50582", Coordinate(55.657977, 2.716029)),
                listOf("ST 49851 22534", Coordinate(51.0, -2.716038)),
                listOf("SK 87290 68571", Coordinate(53.2070530000, -0.6945160000)),
                listOf("TQ 22069 82537", Coordinate(51.5285582, -0.241681)),
                listOf("?", Coordinate(42.1948, -71.6295))
            )
        }
        fun provideLocationStrings(): List<List<Any?>> {
            return listOf(
                // DDM
                listOf("10°2.083333' N, 77°30.5' E", Coordinate(10.03472, 77.508333)),
                listOf("10°2.083333' S, 77°30.5' E", Coordinate(-10.03472, 77.508333)),
                listOf("10°2.083333' N, 77°30.5' W", Coordinate(10.03472, -77.508333)),
                listOf("10°2.083333' S, 77°30.5' W", Coordinate(-10.03472, -77.508333)),
                listOf(
                    "10°2.083333′ S, 77°30.5′ W",
                    Coordinate(-10.03472, -77.508333)
                ), // Using prime chars instead of quotes
                // DMS
                listOf("10°2'5.0\" N, 77°30'30.0\" E", Coordinate(10.03472, 77.508333)),
                listOf("10°2'5.0\" S, 77°30'30.0\" E", Coordinate(-10.03472, 77.508333)),
                listOf("10°2'5.0\" N, 77°30'30.0\" W", Coordinate(10.03472, -77.508333)),
                listOf("10°2'5.0\" S, 77°30'30.0\" W", Coordinate(-10.03472, -77.508333)),
                listOf("10°2'5\" S, 77°30'30\" W", Coordinate(-10.03472, -77.508333)),
                listOf(
                    "10°2′5\" S, 77°30′30\" W",
                    Coordinate(-10.03472, -77.508333)
                ), // Using prime chars instead of quotes
                listOf(
                    "10°2'5″ S, 77°30'30″ W",
                    Coordinate(-10.03472, -77.508333)
                ), // Using prime chars instead of quotes
                listOf(
                    "10°2′5″ S, 77°30′30″ W",
                    Coordinate(-10.03472, -77.508333)
                ), // Using prime chars instead of quotes
                // DD
                listOf("42.1948, -71.6295", Coordinate(42.1948, -71.6295)),
                listOf("-90, -180", Coordinate(-90.0, -180.0)),
                listOf("90, 180", Coordinate(90.0, 180.0)),
                listOf("-42.1948, -71.6295", Coordinate(-42.1948, -71.6295)),
                listOf("-42,1948, -71,6295", Coordinate(-42.1948, -71.6295)),
                listOf("-42,1948°, -71,6295°", Coordinate(-42.1948, -71.6295)),
                listOf("1.2,1.4", Coordinate(1.2, 1.4)),
                listOf("1.2°, 1.4°", Coordinate(1.2, 1.4)),
                listOf("1 8", Coordinate(1.0, 8.0)),
                // UTM
                listOf("19T 0282888E 4674752N", Coordinate(42.1948, -71.6295)),
                listOf("14T 0328056E 5290773N", Coordinate(47.7474, -101.2939)),
                listOf("13R 0393008E 3051634N", Coordinate(27.5844, -106.0840)),
                listOf("21L 0359923E 9098523N", Coordinate(-8.1534, -58.2715)),
                listOf("34H 0674432E 6430470N", Coordinate(-32.2489, 22.8516)),
                listOf("34H 0674432E 6430470N", Coordinate(-32.2489, 22.8516)),
                listOf("34D 0528288E 2071725N", Coordinate(-71.4545, 21.7969)),
                listOf("40X 0545559E 9051365N", Coordinate(81.5113, 59.7656)),
                listOf("17M 0784692E 9999203N", Coordinate(-0.0072, -78.4424)),
                listOf("09E 0353004E 3573063N", Coordinate(-57.9598, -131.4844)),
                listOf("09e 0353004e 3573063n", Coordinate(-57.9598, -131.4844)),
                listOf("09 E 0353004 E 3573063 N", Coordinate(-57.9598, -131.4844)),
                listOf("09E 0353004 E 3573063 N", Coordinate(-57.9598, -131.4844)),
                // Polar zones UTM
                listOf("A 1998062E 1888990N", Coordinate(-89.0, -179.0)),
                listOf("A 1998062E 2111009N", Coordinate(-89.0, -1.0)),
                listOf("B 2019279E 2109339N", Coordinate(-89.0, 10.0)),
                listOf("B 2000000E 2111026N", Coordinate(-89.0, 0.0)),
                listOf("B 2000000E 2000000N", Coordinate(-90.0, 0.0)),
                listOf("Y 1998062E 2111009N", Coordinate(89.0, -179.0)),
                listOf("Y 1998062E 1888990N", Coordinate(89.0, -1.0)),
                listOf("Z 2019279E 1890660N", Coordinate(89.0, 10.0)),
                listOf("Z 2000000E 1888973N", Coordinate(89.0, 0.0)),
                listOf("Z 2000000E 2000000N", Coordinate(90.0, 0.0)),
                // MGRS
                listOf("27PXM 09601 05579", Coordinate(10.0, -20.0)),
                listOf("51DVC 65812 22723", Coordinate(-70.1, 122.1)),
                listOf("51DVC6581222723", Coordinate(-70.1, 122.1)),
                listOf("51 D VC 65812 22723", Coordinate(-70.1, 122.1)),
//                listOf("YZJ 98062 11010", Coordinate(89.0, -179.0)), // TODO: This can be generated, but not parsed
                listOf("27PXM 0960 0558", Coordinate(10.0, -20.0)),
                listOf("27PXM 096 056", Coordinate(10.0, -20.0)),
                listOf("27PXM 10 06", Coordinate(10.0038, -19.9963)),
                listOf("27PXM 1 1", Coordinate(10.0399725934, -19.9963)),

                // USNG
                listOf("14S NJ 39521 41744", Coordinate(39.2240867222, -98.5421515000)),
                listOf("18S UJ 23246 05745", Coordinate(38.8828019136, -77.0377807680)),
                listOf("17R KL 70903 50491", Coordinate(27.5589270380, -83.3203125000)),
                listOf("05Q KC 72828 79387", Coordinate(20.5998824479, -155.1796880364)),
                listOf("14SNJ3952141744", Coordinate(39.2240867222, -98.5421515000)),
                listOf("18SUJ2324605745", Coordinate(38.8828019136, -77.0377807680)),
                listOf("17RKL7090350491", Coordinate(27.5589270380, -83.3203125000)),
                listOf("5QKC7282879387", Coordinate(20.5998824479, -155.1796880364)),

                // OSGB (OSGB-36)
                listOf("TG 51409 13177", Coordinate(52.657977, 1.716038)),
                listOf("OR 96706 50582", Coordinate(55.657977, 2.716029)),
                listOf("ST 49851 22534", Coordinate(51.0, -2.716038)),
                listOf("651409,313177", Coordinate(52.657977, 1.716038)),
                listOf("696706,650582", Coordinate(55.657977, 2.716029)),
                listOf("487289,368568", Coordinate(53.2070530000, -0.6945160000)),
                listOf("522067,182536", Coordinate(51.5285582, -0.241681)),

                // Invalid formats / locations
                listOf("91 8", null),
                listOf("-91 8", null),
                listOf("1 181", null),
                listOf("1 -181", null),
                listOf("test", null),
                listOf("1 1231E 1231N", null),
                listOf("1a 1231E 1231N", null),
                listOf("1b 1231E 1231N", null),
                listOf("1y 1231E 1231N", null),
                listOf("1z 1231E 1231N", null),
                listOf("1t 123", null),
                listOf("12m1, 4m1", null),
                listOf("61T 1234E 1234N", null),
                listOf("1", null),
                listOf("", null),
                listOf("10°2'5.0\", 77°30'30.0\"", null),
                listOf("10°2'5.0\" W, 77°30'30.0\" N", null),
                listOf("10°2'5.0 S, 77°30'30.0\" W", null),
                listOf("10°2'5.0 S\", 30'30.0\" W", null),
                listOf("10°2' S\", 77°30'30.0\" W", null),
                listOf("10°5\" S, 77°30'30.0\" W", null)

            )
        }
    }

}