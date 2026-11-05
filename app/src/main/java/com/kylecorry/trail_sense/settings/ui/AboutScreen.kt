@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.settings.ui

import com.kylecorry.trail_sense.shared.navigation.Route

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.core.system.Package
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.icons.trail.Trail
import com.kylecorry.trail_sense.shared.icons.trail.TrailIcons
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseButtonStyle
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

private const val GITHUB_URL = "https://github.com/kylecorry31/Trail-Sense"

@Composable
fun AboutToolScreen(host: TrailSenseDestinationHost) {
    AboutScreen(
        version = Package.getVersionName(host.requireContext()),
        onOpenUrl = { host.startActivity(Intents.url(it)) },
        onEmailDeveloper = {
            val intent = Intents.email(host.getString(R.string.email), host.getString(R.string.app_name))
            host.startActivity(Intent.createChooser(intent, host.getString(R.string.pref_email_title)))
        },
        onLicenses = { host.findNavigator().navigate(Route.Licenses) }
    )
}

/**
 * About, in WiFi Weakspot's shape: who the app is (name, one line, version), the two things people come here for
 * (the code and a way to reach the developer) as two cards, and privacy as one quiet line with its links. Each fact
 * appears once.
 */
@Composable
private fun AboutScreen(
    version: String,
    onOpenUrl: (String) -> Unit,
    onEmailDeveloper: () -> Unit,
    onLicenses: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val privacyUrl = stringResource(R.string.privacy_policy_url)
    TrailSensePage(title = stringResource(R.string.pref_category_about), backLabel = stringResource(R.string.back), modifier = Modifier.testTag("about")) {
        item("identity") {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = MaterialShapes.Cookie12Sided.toShape(),
                    color = scheme.primaryContainer,
                    contentColor = scheme.onPrimaryContainer,
                    modifier = Modifier.size(140.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(TrailIcons.Trail, contentDescription = null, modifier = Modifier.size(60.dp))
                    }
                }
                Text(
                    stringResource(R.string.app_name),
                    modifier = Modifier.padding(top = 20.dp),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    stringResource(R.string.about_tagline),
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Surface(
                    shape = CircleShape,
                    color = scheme.surfaceContainerHighest,
                    modifier = Modifier.padding(top = TrailSenseSpacing.lg)
                ) {
                    Text(
                        stringResource(R.string.about_version, version),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.onSurfaceVariant
                    )
                }
            }
        }

        item("actions") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(top = TrailSenseSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                AboutCard(
                    title = stringResource(R.string.pref_github_title),
                    body = stringResource(R.string.about_open_source),
                    icon = Icons.Rounded.Code,
                    container = scheme.primaryContainer,
                    content = scheme.onPrimaryContainer,
                    badge = MaterialShapes.Cookie9Sided.toShape(),
                    shape = RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp, topEnd = 8.dp, bottomEnd = 8.dp),
                    onClick = { onOpenUrl(GITHUB_URL) },
                    modifier = Modifier.weight(1f)
                )
                AboutCard(
                    title = stringResource(R.string.about_feedback_title),
                    body = stringResource(R.string.about_feedback_body),
                    icon = Icons.Rounded.Email,
                    container = scheme.secondaryContainer,
                    content = scheme.onSecondaryContainer,
                    badge = MaterialShapes.Clover4Leaf.toShape(),
                    shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 32.dp, bottomEnd = 32.dp),
                    onClick = onEmailDeveloper,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item("privacy") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = TrailSenseSpacing.sm, start = TrailSenseSpacing.lg, end = TrailSenseSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.sm)
            ) {
                Icon(Icons.Outlined.VerifiedUser, null, Modifier.size(20.dp), tint = scheme.onSurfaceVariant)
                Text(
                    stringResource(R.string.about_privacy_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Row(horizontalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xs)) {
                    TrailSenseButton(
                        text = stringResource(R.string.pref_privacy_policy_title),
                        style = TrailSenseButtonStyle.Text,
                        onClick = { onOpenUrl(privacyUrl) }
                    )
                    TrailSenseButton(
                        text = stringResource(R.string.pref_open_source_licenses_title),
                        style = TrailSenseButtonStyle.Text,
                        onClick = onLicenses
                    )
                }
            }
        }
    }
}

/** One of the two things people come to About for, its icon on an Expressive shape (WiFi Weakspot's action card). */
@Composable
private fun AboutCard(
    title: String,
    body: String,
    icon: ImageVector,
    container: Color,
    content: Color,
    badge: Shape,
    shape: Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = shape,
        color = container,
        contentColor = content,
        modifier = modifier.fillMaxHeight()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(badge)
                        .background(content),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, Modifier.size(22.dp), tint = container)
                }
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.AutoMirrored.Rounded.OpenInNew,
                    null,
                    Modifier.size(18.dp),
                    tint = content.copy(alpha = 0.7f)
                )
            }
            Text(
                title,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(body, style = MaterialTheme.typography.bodySmall, color = content.copy(alpha = 0.8f))
        }
    }
}
