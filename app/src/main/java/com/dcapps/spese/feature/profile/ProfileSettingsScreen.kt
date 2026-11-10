package com.dcapps.spese.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dcapps.spese.R
import com.dcapps.spese.core.locale.LocaleManager
import com.dcapps.spese.ui.components.SpeseTopAppBar
import com.dcapps.spese.ui.theme.SpeseTheme

/** Impostazioni: selettori lingua e tema. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileSettingsScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val user by viewModel.currentUser.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            SpeseTopAppBar(
                title = stringResource(R.string.profile_settings),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Lingua — sezione a righe (stile app: intestazione + chip + divisore).
            SettingsSection(title = stringResource(R.string.profile_language)) {
                val currentLang = user?.appLanguagePreference?.takeIf { it.isNotBlank() }
                    ?: LocaleManager.currentLanguage()
                SelectableChip(stringResource(R.string.profile_language_it), currentLang == "it") { viewModel.setLanguage("it") }
                SelectableChip(stringResource(R.string.profile_language_en), currentLang == "en") { viewModel.setLanguage("en") }
            }

            HorizontalDivider(color = SpeseTheme.colors.divider)

            // Tema
            SettingsSection(title = stringResource(R.string.profile_theme)) {
                val current = user?.appThemePreference ?: "default"
                SelectableChip(stringResource(R.string.profile_theme_default), current == "default") { viewModel.setTheme("default") }
                SelectableChip(stringResource(R.string.profile_theme_light), current == "light") { viewModel.setTheme("light") }
                SelectableChip(stringResource(R.string.profile_theme_dark), current == "dark") { viewModel.setTheme("dark") }
                SelectableChip(stringResource(R.string.profile_theme_legacy), current == "legacy") { viewModel.setTheme("legacy") }
                SelectableChip(stringResource(R.string.profile_theme_auto), current == "auto") { viewModel.setTheme("auto") }
            }

            HorizontalDivider(color = SpeseTheme.colors.divider)

            // Sincronizzazione — flag per mostrare/nascondere il badge "in attesa" sulle righe.
            val showPending by viewModel.showPendingSyncBadges.collectAsStateWithLifecycle()
            SwitchRow(
                title = stringResource(R.string.profile_sync_badges),
                subtitle = stringResource(R.string.profile_sync_badges_desc),
                checked = showPending,
                onCheckedChange = { viewModel.setShowPendingSyncBadges(it) },
            )
        }
    }
}

/** Riga impostazioni con titolo/descrizione a sinistra e interruttore a destra. */
@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** Sezione impostazioni: intestazione + contenuto a chip (FlowRow), coerente con lo stile app. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsSection(title: String, content: @Composable FlowRowScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}
