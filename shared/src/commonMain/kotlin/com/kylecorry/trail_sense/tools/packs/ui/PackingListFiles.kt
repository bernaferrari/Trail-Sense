package com.kylecorry.trail_sense.tools.packs.ui

import com.kylecorry.trail_sense.tools.packs.domain.Pack

/** Packing list file import/export (LighterPack CSV), supplied by the platform shell. */
interface PackingListFiles {
    /** Picks a CSV, imports it as a new pack and opens it. */
    fun import()

    /** Exports [pack] as a CSV. */
    fun export(pack: Pack)
}
