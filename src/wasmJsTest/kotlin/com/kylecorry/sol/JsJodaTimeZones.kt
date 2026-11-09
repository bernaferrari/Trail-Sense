package com.kylecorry.sol

// Loads the IANA time zone database that kotlinx-datetime needs on Wasm/JS
@JsModule("@js-joda/timezone")
external object JsJodaTimeZoneModule

private val jsJodaTz = JsJodaTimeZoneModule
