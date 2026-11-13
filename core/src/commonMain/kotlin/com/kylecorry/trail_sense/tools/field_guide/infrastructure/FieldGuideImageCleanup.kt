package com.kylecorry.trail_sense.tools.field_guide.infrastructure

/** Deletes stored field guide photos that no page uses any more; supplied by the platform that stores them. */
fun interface FieldGuideImageCleanup {
    suspend fun cleanup()
}
