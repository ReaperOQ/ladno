package app.ladno.domain

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsModule

@OptIn(ExperimentalWasmJsInterop::class)
@JsModule("@js-joda/timezone")
private external object JsJodaTimeZoneModule

private val timeZoneModule = JsJodaTimeZoneModule

internal actual fun initializeTimeZones() {
    // Referencing the module initializes its IANA rules before TimeZone.of is called.
    timeZoneModule
}
