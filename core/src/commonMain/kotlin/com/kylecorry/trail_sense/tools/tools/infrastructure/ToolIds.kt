package com.kylecorry.trail_sense.tools.tools.infrastructure

/**
 * Stable identifiers for tools and quick actions. They are persisted (pinned tools, quick action slots, bottom
 * navigation), so values must never change; the platform tool registry maps them to tools.
 */
object ToolIds {
    const val TOOL_QUICK_ACTION_OFFSET = 1000

    // Tool IDs
    const val FLASHLIGHT = 1L
    const val WHISTLE = 2L
    const val RULER = 3L
    const val PEDOMETER = 4L
    const val CLIFF_HEIGHT = 5L
    const val NAVIGATION = 6L
    const val BEACONS = 7L
    const val OFFLINE_MAPS = 8L
    const val PATHS = 9L
    const val TRIANGULATE_LOCATION = 10L
    const val CLINOMETER = 11L
    const val BUBBLE_LEVEL = 12L
    const val CLOCK = 13L
    const val ASTRONOMY = 14L
    const val WATER_BOIL_TIMER = 15L
    const val TIDES = 16L
    const val BATTERY = 17L
    const val SOLAR_PANEL_ALIGNER = 18L
    const val LIGHT_METER = 19L
    const val WEATHER = 20L
    const val CLIMATE = 21L
    const val TEMPERATURE_ESTIMATION = 22L
    const val CLOUDS = 23L
    const val LIGHTNING_STRIKE_DISTANCE = 24L
    const val AUGMENTED_REALITY = 25L
    const val CONVERT = 26L
    const val PACKING_LISTS = 27L
    const val METAL_DETECTOR = 28L
    const val WHITE_NOISE = 29L
    const val NOTES = 30L
    const val QR_CODE_SCANNER = 31L
    const val SENSORS = 32L
    const val DIAGNOSTICS = 33L
    const val SETTINGS = 34L
    const val USER_GUIDE = 35L
    const val EXPERIMENTATION = 36L
    const val MIRROR_CAMERA = 37L
    const val TURN_BACK = 38L
    const val LOCAL_MESSAGING = 39L
    const val LOCAL_TALK = 40L
    const val SURVIVAL_GUIDE = 41L
    const val FIELD_GUIDE = 42L
    const val SIGNAL_FINDER = 43L
    const val BALLISTICS = 44L
    const val DECLINATION = 46L
    const val MAP = 47L
    const val MAGNIFIER = 48L

    // Quick Action IDs
    const val QUICK_ACTION_NONE = -1
    const val QUICK_ACTION_FLASHLIGHT = 1
    const val QUICK_ACTION_RULER = 5
    const val QUICK_ACTION_WHISTLE = 8
    const val QUICK_ACTION_WHITE_NOISE = 9
    const val QUICK_ACTION_LOW_POWER_MODE = 10
    const val QUICK_ACTION_SUNSET_ALERT = 13
    const val QUICK_ACTION_NIGHT_MODE = 14
    const val QUICK_ACTION_BACKTRACK = 15
    const val QUICK_ACTION_WEATHER_MONITOR = 16
    const val QUICK_ACTION_PEDOMETER = 17
    const val QUICK_ACTION_SCREEN_FLASHLIGHT = 18
    const val QUICK_ACTION_CREATE_NOTE = 19
    const val QUICK_ACTION_CREATE_BEACON = 20
    const val QUICK_ACTION_SCAN_CLOUD = 21
    const val QUICK_ACTION_OPEN_PHOTO_MAP = 22
    const val QUICK_ACTION_USER_GUIDE = 23
    const val QUICK_ACTION_SETTINGS = 24
    const val QUICK_ACTION_SUNRISE_ALERT = 25
    const val QUICK_ACTION_TOOL_WIDGETS = 26
    const val QUICK_ACTION_CONVERT = 27
    const val QUICK_ACTION_SURVIVAL_GUIDE = 28
    const val QUICK_ACTION_RECALIBRATE_ALTIMETER = 29
    const val QUICK_ACTION_RECORD_SIGHTING = 30
}
