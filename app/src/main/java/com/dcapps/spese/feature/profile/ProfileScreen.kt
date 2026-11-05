package com.dcapps.spese.feature.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.dcapps.spese.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    onOpenDrawer: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showEditName by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) viewModel.uploadAvatar(context, uri) }

    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    com.dcapps.spese.ui.components.ErrorSnackbarEffect(
        state.message, snackbarHostState, viewModel::clearMessage,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title)) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.drawer_open))
                    }
                },
            )
        },
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
                        contentDescription = stringResource(R.string.profile_avatar),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
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
                        contentDescription = stringResource(R.string.profile_change_avatar),
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

            // Nome con azione di modifica.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Text(
                    user?.displayName ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = { showEditName = true }) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.profile_edit_name), modifier = Modifier.size(18.dp))
                }
            }
            // Email con azione di copia.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(user?.email ?: "", style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                        as android.content.ClipboardManager
                    clipboard.setPrimaryClip(
                        android.content.ClipData.newPlainText("email", user?.email ?: ""),
                    )
                }) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.profile_copy_email),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

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
                            contentDescription = stringResource(R.string.profile_previous_photo),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                // Bordo in contrasto col tema: rende visibili i PNG trasparenti.
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable { viewModel.selectAvatar(url) },
                        )
                    }
                }
            }

            // Tema
            Card(Modifier.fillMaxWidth().padding(top = 20.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.profile_theme), style = MaterialTheme.typography.titleMedium)
                    androidx.compose.foundation.layout.FlowRow(
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val current = user?.themePreference ?: "auto"
                        ThemeChip(stringResource(R.string.profile_theme_light), "light", current, viewModel::setTheme)
                        ThemeChip(stringResource(R.string.profile_theme_dark), "dark", current, viewModel::setTheme)
                        ThemeChip(stringResource(R.string.profile_theme_legacy), "legacy", current, viewModel::setTheme)
                        ThemeChip(stringResource(R.string.profile_theme_auto), "auto", current, viewModel::setTheme)
                    }
                }
            }

            // Lingua
            Card(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.profile_language), style = MaterialTheme.typography.titleMedium)
                    androidx.compose.foundation.layout.FlowRow(
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // La lingua selezionata: preferisci quella salvata sull'utente, altrimenti
                        // quella attualmente applicata all'app.
                        val currentLang = user?.languagePreference?.takeIf { it.isNotBlank() }
                            ?: com.dcapps.spese.core.locale.LocaleManager.currentLanguage()
                        LanguageChip(stringResource(R.string.profile_language_it), "it", currentLang, viewModel::setLanguage)
                        LanguageChip(stringResource(R.string.profile_language_en), "en", currentLang, viewModel::setLanguage)
                    }
                }
            }

            // Statistiche
            Card(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.profile_stats), style = MaterialTheme.typography.titleMedium)
                    if (!state.statsLoaded) {
                        CircularProgressIndicator(Modifier.size(20.dp).padding(top = 8.dp))
                    } else {
                        StatRow(stringResource(R.string.profile_stats_owned), state.stats.listsOwned)
                        StatRow(stringResource(R.string.profile_stats_participating), state.stats.listsParticipating)
                        StatRow(stringResource(R.string.profile_stats_expenses), state.stats.expenses)
                    }
                }
            }

            // Informazioni / Amministrazione / Controlla aggiornamenti sono ora nel menu laterale
            // (drawer), gestito da SpeseNavHost.

            OutlinedButton(
                onClick = { viewModel.signOut(context) },
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.profile_logout), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }

    if (showEditName) {
        EditNameDialog(
            initialName = user?.name ?: "",
            initialSurname = user?.surname ?: "",
            onDismiss = { showEditName = false },
            onConfirm = { n, s -> viewModel.updateNameSurname(n, s); showEditName = false },
        )
    }
}

@Composable
private fun EditNameDialog(
    initialName: String,
    initialSurname: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, surname: String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var surname by remember { mutableStateOf(initialSurname) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_edit_name)) },
        text = {
            Column {
                androidx.compose.material3.OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                androidx.compose.material3.OutlinedTextField(
                    value = surname,
                    onValueChange = { surname = it },
                    label = { Text(stringResource(R.string.profile_surname)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name, surname) },
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageChip(label: String, tag: String, current: String, onSelect: (String) -> Unit) {
    FilterChip(
        selected = current == tag,
        onClick = { onSelect(tag) },
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
