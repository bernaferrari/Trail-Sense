package com.kylecorry.trail_sense.tools.offline_maps.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.kylecorry.andromeda.files.FileStorage
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.SharedMapFiles
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.diskOfflineMapStorage

@Composable
fun PortableVectorMapScreen(mapId: Long, fileStorage: FileStorage) {
    val files = remember(fileStorage) { SharedMapFiles(diskOfflineMapStorage(fileStorage)) }
    SharedOfflineMap(mapId, files)
}
