package com.kylecorry.andromeda.views.toolbar

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun Toolbar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigation: @Composable () -> Unit = {},
    actions: @Composable () -> Unit = {},
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Column {
                Text(title)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
        },
        navigationIcon = navigation,
        actions = { actions() },
    )
}
