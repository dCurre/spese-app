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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dcapps.spese.R
import com.dcapps.spese.core.sync.SyncState

/**
 * Piccolo indicatore non invasivo dello stato di sincronizzazione: uno spinner mentre il sync è in
 * corso, un'icona "cloud off" (toccabile per nasconderla) se l'ultimo ciclo è fallito con un errore
 * reale. Invisibile quando è tutto a posto (IDLE) o in guest (il guest non sincronizza → resta IDLE).
 */
@Composable
fun SyncIndicator(
    modifier: Modifier = Modifier,
    viewModel: SyncIndicatorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AnimatedVisibility(
        visible = state != SyncState.IDLE,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        val isError = state == SyncState.ERROR
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(
                    if (isError) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.secondaryContainer,
                )
                .then(if (isError) Modifier.clickable { viewModel.acknowledgeError() } else Modifier)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            if (isError) {
                Icon(
                    Icons.Default.CloudOff,
                    contentDescription = stringResource(R.string.sync_status_error),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(16.dp),
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}
