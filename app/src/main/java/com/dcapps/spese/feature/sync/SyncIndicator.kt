package com.dcapps.spese.feature.sync

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dcapps.spese.R

/**
 * Piccolo indicatore non invasivo dello stato di sincronizzazione, in alto a destra:
 * - SYNCING: spinner;
 * - OFFLINE: nuvoletta tenue + "Offline" (neutro: non è un errore, le modifiche si sincronizzano
 *   al ritorno online);
 * - ERROR: nuvoletta rossa + "Sync non riuscita", toccabile per nasconderla;
 * - HIDDEN: invisibile (tutto a posto / nessun sync in corso).
 */
@Composable
fun SyncIndicator(
    modifier: Modifier = Modifier,
    viewModel: SyncIndicatorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AnimatedVisibility(
        visible = state != SyncIndicatorState.HIDDEN,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        when (state) {
            SyncIndicatorState.SYNCING -> Pill(container = MaterialTheme.colorScheme.secondaryContainer) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }

            SyncIndicatorState.OFFLINE -> LabeledPill(
                icon = Icons.Outlined.CloudQueue,
                label = stringResource(R.string.sync_status_offline),
                container = MaterialTheme.colorScheme.surfaceVariant,
                content = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SyncIndicatorState.ERROR -> LabeledPill(
                icon = Icons.Default.CloudOff,
                label = stringResource(R.string.sync_status_error_hint),
                container = MaterialTheme.colorScheme.errorContainer,
                content = MaterialTheme.colorScheme.onErrorContainer,
                onClick = { viewModel.acknowledgeError() },
            )

            SyncIndicatorState.HIDDEN -> Unit
        }
    }
}

@Composable
private fun Pill(
    container: Color,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        content = { content() },
    )
}

@Composable
private fun LabeledPill(
    icon: ImageVector,
    label: String,
    container: Color,
    content: Color,
    onClick: (() -> Unit)? = null,
) {
    Pill(container = container, onClick = onClick) {
        Icon(
            icon,
            contentDescription = label,
            tint = content,
            modifier = Modifier.size(16.dp),
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = content,
        )
    }
}
