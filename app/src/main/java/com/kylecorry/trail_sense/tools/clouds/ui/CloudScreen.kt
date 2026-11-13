@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.clouds.ui

import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.shared.views.compose.from
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kylecorry.andromeda.core.system.IntentResultRetriever
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.result.getOrNull
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.units.Reading
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.main.persistence.IReadingRepo
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.dialogs.CoroutineAlerts
import com.kylecorry.trail_sense.shared.dialogs.dialog
import com.kylecorry.trail_sense.shared.getInstance
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.io.FileSubsystem
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
import com.kylecorry.trail_sense.shared.navigation.LocalNavigator
import com.kylecorry.trail_sense.shared.navigation.Route
import com.kylecorry.trail_sense.shared.text.RawTexts
import com.kylecorry.trail_sense.shared.time.toLocalDate
import com.kylecorry.trail_sense.shared.views.CloudsEmptyIllustration
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.AppPermission
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseEmptyState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowGroup
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.trailSenseGroupShape
import com.kylecorry.trail_sense.tools.clouds.domain.classification.SoftmaxCloudClassifier
import com.kylecorry.trail_sense.tools.clouds.infrastructure.CloudDetailsService
import com.kylecorry.trail_sense.tools.clouds.infrastructure.persistence.CloudObservation
import com.kylecorry.trail_sense.tools.clouds.infrastructure.persistence.CloudRepo
import com.kylecorry.trail_sense.tools.guide.infrastructure.UserGuideUtils
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Composable
fun CloudToolScreen(route: Route.Clouds) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val navigator = LocalNavigator.current
    val effects = LocalScreenEffects.current
    // The file picker needs the activity's result launcher; MainActivity is the IntentResultRetriever.
    val resultRetriever = LocalActivity.current as IntentResultRetriever
    val scope = rememberCoroutineScope()
    val viewModel = viewModel { CloudViewModel(CloudRepo.getInstance(appContext)) }
    val formatter = remember(appContext) { FormatService.getInstance(appContext) }
    val cloudDetailsService = remember(appContext) { CloudDetailsService(appContext) }
    val files = remember(appContext) { FileSubsystem.getInstance(appContext) }
    val clouds by viewModel.clouds.collectAsStateWithLifecycle()
    val subtitle = stringResource(
        R.string.last_duration,
        formatter.formatDuration(48.hours, short = true)
    )

    fun openCloudPicker(uri: Uri) {
        navigator.navigate(Route.CloudResults(imageUri = uri.toString()))
    }

    fun addFromCamera() {
        scope.launch {
            if (!effects.requestPermission(AppPermission.Camera)) {
                effects.explainPermissionDenied(AppPermission.Camera)
                return@launch
            }
            val prefs = UserPreferences(appContext).camera
            val uri = effects.capturePhoto(
                SoftmaxCloudClassifier.IMAGE_SIZE,
                SoftmaxCloudClassifier.IMAGE_SIZE,
                prefs.useZeroShutterLag
            )
            uri?.let { openCloudPicker(it.toUri()) }
        }
    }

    LaunchedEffect(viewModel) {
        if (route.openScanner && viewModel.consumeOpenScanner()) {
            addFromCamera()
        }
    }

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.selectedCloudImage = null
        }
    }

    CloudScreen(
        subtitle = subtitle,
        clouds = clouds ?: emptyList(),
        onGuideClick = {
            UserGuideUtils.showGuide(navigator, RawTexts.guide_tool_clouds)
        },
        onCameraClick = ::addFromCamera,
        onFileClick = {
            scope.launch {
                val uri = IntentUriPicker(resultRetriever, context).open(listOf("image/*")).getOrNull()
                val temp = uri?.let { onIO { files.copyToTemp(it) }?.toUri() }
                temp?.let { openCloudPicker(it) }
            }
        },
        onManualClick = { navigator.navigate(Route.CloudResults()) },
        onCloudClick = { CloudDetailsModal(AppHost.from(context)).show(it.value.genus) },
        onCloudImageClick = { viewModel.selectedCloudImage = it.value.genus },
        onDeleteCloud = { reading ->
            scope.launch {
                val cancelled = CoroutineAlerts.dialog(
                    context,
                    context.getString(R.string.delete),
                    cloudDetailsService.getCloudName(reading.value.genus)
                )
                if (!cancelled) {
                    viewModel.delete(reading)
                }
            }
        },
        selectedCloudImage = viewModel.selectedCloudImage,
        onDismissCloudImage = { viewModel.selectedCloudImage = null },
        modifier = Modifier.fillMaxSize(),
        loaded = clouds != null
    )
}
