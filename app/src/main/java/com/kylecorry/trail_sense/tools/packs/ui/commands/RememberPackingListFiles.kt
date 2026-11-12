package com.kylecorry.trail_sense.tools.packs.ui.commands

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.kylecorry.andromeda.core.system.IntentResultRetriever
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import com.kylecorry.trail_sense.tools.packs.domain.Pack
import com.kylecorry.trail_sense.tools.packs.ui.PackingListFiles

/** LighterPack CSV import/export through the Android file picker (the activity retrieves the results). */
@Composable
fun rememberPackingListFiles(): PackingListFiles {
    val context = LocalContext.current
    val resultRetriever = LocalActivity.current as IntentResultRetriever
    val navigator = LocalNavigator.current
    val effects = LocalScreenEffects.current
    val scope = rememberCoroutineScope()
    return remember(context, resultRetriever, navigator, effects, scope) {
        object : PackingListFiles {
            override fun import() {
                ImportPackingListCommand(context, resultRetriever, scope, effects, navigator).execute()
            }

            override fun export(pack: Pack) {
                ExportPackingListCommand(context, resultRetriever, scope, effects).execute(pack)
            }
        }
    }
}
