package com.kylecorry.trail_sense.tools.offline_maps.ui.commands

import androidx.compose.ui.text.LinkAnnotation
import com.kylecorry.trail_sense.settings.PreferenceKeys
import kotlin.test.Test
import kotlin.test.assertEquals

class OfflineMapsDisclaimerTest {

    @Test
    fun theMapDownloadAddressIsALink() {
        val message = offlineMapsDisclaimerMessage(
            "Always carry a physical map. You can find maps to download at https://kylecorry.com/Trail-Sense/offline_maps.html"
        )

        val links = message.getLinkAnnotations(0, message.length)
        assertEquals(1, links.size)
        assertEquals("https://kylecorry.com/Trail-Sense/offline_maps.html", (links[0].item as LinkAnnotation.Url).url)
    }

    @Test
    fun keepsTheKeyPeopleAlreadyAcceptedItUnder() {
        // Changing this would show the disclaimer again to everyone who already closed it
        assertEquals("photo_map_disclaimer_shown_v2", PreferenceKeys.offlineMapsDisclaimerShown)
    }
}
