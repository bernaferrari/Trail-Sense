package com.kylecorry.trail_sense.tools.offline_maps.infrastructure.create

import com.kylecorry.trail_sense.tools.offline_maps.domain.CreateMapResult

interface ICreateMapCommand {
    suspend fun execute(): CreateMapResult
}
