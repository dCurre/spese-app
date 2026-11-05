package com.dcapps.spese.feature.profile

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.dcapps.spese.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onOpenProfileDetail: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenAdmin: () -> Unit,
    onCheckUpdates: () -> Unit,
    updateChecking: Boolean,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val isAdmin = user?.role == "admin" || user?.role == "superadmin"

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.profile_title)) }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // Intestazione account: avatar + nome, apre il dettaglio profilo.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .clickable(onClick = onOpenProfileDetail)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val img = user?.profileImage
                if (!img.isNullOrBlank()) {
                    AsyncImage(
                        model = img,
                        contentDescription = stringResource(R.string.profile_avatar),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                    )
                }
                Text(
                    user?.displayName ?: "",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(start = 16.dp),
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            }

            // Gruppo: Informazioni, Statistiche, Impostazioni.
            Card(Modifier.fillMaxWidth().padding(top = 20.dp)) {
                Column {
                    ProfileListItem(
                        icon = Icons.Default.Info,
                        title = stringResource(R.string.profile_about),
                        onClick = onOpenAbout,
                    )
                    ProfileListItem(
                        icon = Icons.Default.QueryStats,
                        title = stringResource(R.string.profile_stats),
                        onClick = onOpenStats,
                    )
                    ProfileListItem(
                        icon = Icons.Default.Settings,
                        title = stringResource(R.string.profile_settings),
                        subtitle = stringResource(R.string.profile_settings_subtitle),
                        onClick = onOpenSettings,
                    )
                }
            }

            // Gruppo: Controlla aggiornamenti (+ Amministrazione se admin).
            Card(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Column {
                    ProfileListItem(
                        icon = Icons.Default.Refresh,
                        title = stringResource(R.string.profile_check_updates),
                        onClick = { if (!updateChecking) onCheckUpdates() },
                        trailing = if (updateChecking) {
                            { CircularProgressIndicator(Modifier.size(20.dp)) }
                        } else null,
                    )
                    if (isAdmin) {
                        ProfileListItem(
                            icon = Icons.Default.AdminPanelSettings,
                            title = stringResource(R.string.profile_admin),
                            onClick = onOpenAdmin,
                        )
                    }
                }
            }
        }
    }
}
