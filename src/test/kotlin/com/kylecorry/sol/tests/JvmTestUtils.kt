package com.kylecorry.sol.tests

import java.time.Duration
import java.time.LocalDateTime
import java.time.ZonedDateTime
import kotlin.time.toKotlinDuration
import kotlinx.datetime.toKotlinLocalDateTime
import kotlin.time.toKotlinInstant

// Temporary JVM-only overloads while tests are ported to commonTest.
fun assertDate(expected: ZonedDateTime?, actual: ZonedDateTime?, maxDifference: Duration) =
    assertDate(expected?.toInstant()?.toKotlinInstant(), actual?.toInstant()?.toKotlinInstant(), maxDifference.toKotlinDuration())

fun assertDate(expected: LocalDateTime?, actual: LocalDateTime?, maxDifference: Duration) =
    assertDate(expected?.toKotlinLocalDateTime(), actual?.toKotlinLocalDateTime(), maxDifference.toKotlinDuration())

fun assertDuration(expected: Duration?, actual: Duration?, maxDifference: Duration) =
    assertDuration(expected?.toKotlinDuration(), actual?.toKotlinDuration(), maxDifference.toKotlinDuration())
