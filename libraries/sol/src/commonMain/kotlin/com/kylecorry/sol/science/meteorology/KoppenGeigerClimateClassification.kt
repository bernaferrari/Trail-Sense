package com.kylecorry.sol.science.meteorology

public data class KoppenGeigerClimateClassification(
    val climateGroup: KoppenGeigerClimateGroup,
    val seasonalPrecipitationPattern: KoppenGeigerSeasonalPrecipitationPattern?,
    val temperaturePattern: KoppenGeigerTemperaturePattern?,
) {
    val code: String =
        "${climateGroup.code}${seasonalPrecipitationPattern?.code ?: ""}${temperaturePattern?.code ?: ""}"
}

public enum class KoppenGeigerClimateGroup(public val code: Char) {
    Tropical('A'),
    Dry('B'),
    Temperate('C'),
    Continental('D'),
    Polar('E'),
}

public enum class KoppenGeigerSeasonalPrecipitationPattern(public val code: Char) {
    Rainforest('f'),
    Monsoon('m'),
    Savanna('s'),
    Desert('W'),
    Steppe('S'),
    DrySummer('s'),
    DryWinter('w'),
    NoDrySeason('f'),
    Tundra('T'),
    IceCap('F'),
}

public enum class KoppenGeigerTemperaturePattern(public val code: Char) {
    Hot('h'),
    Cold('k'),
    HotSummer('a'),
    WarmSummer('b'),
    ColdSummer('c'),
    VeryColdWinter('d'),
}
