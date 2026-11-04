package com.dcapps.spese.feature.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    onOpenAdmin: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
    updateViewModel: com.dcapps.spese.feature.update.UpdateViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) viewModel.uploadAvatar(context, uri) }

    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    com.dcapps.spese.ui.components.MessageSnackbarEffect(
        state.message, snackbarHostState, viewModel::clearMessage,
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("Profilo") }) },
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Avatar
            Box(contentAlignment = Alignment.BottomEnd) {
                val img = user?.profileImage
                if (!img.isNullOrBlank()) {
                    AsyncImage(
                        model = img,
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(96.dp).clip(CircleShape),
                    )
                } else {
                    Box(
                        Modifier.size(96.dp).clip(CircleShape)
                            .clickable { }.padding(0.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(56.dp))
                    }
                }
                if (state.uploading) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                } else {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Cambia avatar",
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable {
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                    )
                }
            }

            Text(
                user?.displayName ?: "",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(user?.email ?: "", style = MaterialTheme.typography.bodyMedium)

            // Storico avatar
            val history = user?.profileImagesHistory ?: emptyList()
            if (history.size > 1) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(history) { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = "Foto precedente",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable { viewModel.selectAvatar(url) },
                        )
                    }
                }
            }

            // Tema
            Card(Modifier.fillMaxWidth().padding(top = 20.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Tema", style = MaterialTheme.typography.titleMedium)
                    androidx.compose.foundation.layout.FlowRow(
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val current = user?.themePreference ?: "auto"
                        ThemeChip("Chiaro", "light", current, viewModel::setTheme)
                        ThemeChip("Scuro", "dark", current, viewModel::setTheme)
                        ThemeChip("Legacy", "legacy", current, viewModel::setTheme)
                        ThemeChip("Auto", "auto", current, viewModel::setTheme)
                    }
                }
            }

            // Statistiche
            Card(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Statistiche", style = MaterialTheme.typography.titleMedium)
                    if (!state.statsLoaded) {
                        CircularProgressIndicator(Modifier.size(20.dp).padding(top = 8.dp))
                    } else {
                        StatRow("Liste possedute", state.stats.listsOwned)
                        StatRow("Partecipazioni", state.stats.listsParticipating)
                        StatRow("Spese totali", state.stats.expenses)
                    }
                }
            }

            // Controllo aggiornamenti manuale.
            val updateState by updateViewModel.uiState.collectAsStateWithLifecycle()
            com.dcapps.spese.ui.components.MessageSnackbarEffect(
                updateState.checkMessage, snackbarHostState, updateViewModel::clearCheckMessage,
            )
            OutlinedButton(
                onClick = { updateViewModel.checkForUpdateManual() },
                enabled = !updateState.checking,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            ) {
                Text(if (updateState.checking) "Controllo…" else "Controlla aggiornamenti")
            }

            val role = user?.role
            if (role == "admin" || role == "superadmin") {
                OutlinedButton(
                    onClick = onOpenAdmin,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                ) {
                    Text("Amministrazione")
                }
            }

            OutlinedButton(
                onClick = { viewModel.signOut(context) },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Esci", modifier = Modifier.padding(start = 8.dp))
            }

            // Mostra anche il popup di aggiornamento se disponibile (dal controllo manuale).
            val info = updateState.info
            if (updateState.showDialog && info != null) {
                com.dcapps.spese.feature.update.UpdateDialog(
                    state = updateState,
                    info = info,
                    canInstall = updateViewModel.canInstall(),
                    onUpdate = updateViewModel::onUpdateClick,
                    onDismiss = updateViewModel::dismiss,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeChip(label: String, value: String, current: String, onSelect: (String) -> Unit) {
    FilterChip(
        selected = current == value,
        onClick = { onSelect(value) },
        label = { Text(label) },
    )
}

@Composable
private fun StatRow(label: String, value: Int) {
    Row(
        Modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text("$value", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
