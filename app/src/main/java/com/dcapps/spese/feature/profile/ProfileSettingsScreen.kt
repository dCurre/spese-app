package com.dcapps.spese.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dcapps.spese.R

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
            TopAppBar(
                title = { Text(stringResource(R.string.profile_settings)) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // Lingua
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.profile_language), style = MaterialTheme.typography.titleMedium)
                    FlowRow(
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val currentLang = user?.languagePreference?.takeIf { it.isNotBlank() }
                            ?: com.dcapps.spese.core.locale.LocaleManager.currentLanguage()
                        SelectableChip(stringResource(R.string.profile_language_it), currentLang == "it") { viewModel.setLanguage("it") }
                        SelectableChip(stringResource(R.string.profile_language_en), currentLang == "en") { viewModel.setLanguage("en") }
                    }
                }
            }

            // Tema
            Card(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.profile_theme), style = MaterialTheme.typography.titleMedium)
                    FlowRow(
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val current = user?.themePreference ?: "auto"
                        SelectableChip(stringResource(R.string.profile_theme_light), current == "light") { viewModel.setTheme("light") }
                        SelectableChip(stringResource(R.string.profile_theme_dark), current == "dark") { viewModel.setTheme("dark") }
                        SelectableChip(stringResource(R.string.profile_theme_legacy), current == "legacy") { viewModel.setTheme("legacy") }
                        SelectableChip(stringResource(R.string.profile_theme_auto), current == "auto") { viewModel.setTheme("auto") }
                    }
                }
            }
        }
    }
}
