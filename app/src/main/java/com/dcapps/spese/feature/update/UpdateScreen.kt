package com.dcapps.spese.feature.update

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dcapps.spese.R
import com.dcapps.spese.core.update.ChangelogEntry
import com.dcapps.spese.core.update.UpdateInfo

/**
 * Schermata di aggiornamento a pagina intera, in stile splash: stesso sfondo verde dello splash,
 * info dell'aggiornamento al centro, bottoni Scarica / Più tardi in basso e una "X" in alto a
 * destra per chiudere (equivalente a "Più tardi"). Sostituisce la vecchia modale.
 */
@Composable
fun UpdateScreen(
    state: UpdateUiState,
    info: UpdateInfo,
    canInstall: Boolean,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    // Sfondo verde dello splash; testo/elementi chiari sopra.
    val bg = colorResource(R.color.ic_launcher_background)
    val onBg = Color.White

    Box(
        Modifier
            .fillMaxSize()
            .background(bg)
            .padding(24.dp),
    ) {
        // X in alto a destra = chiudi (come "Più tardi"); nascosta durante il download.
        if (!state.downloading) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.update_later),
                    tint = onBg,
                )
            }
        }

        // Contenuto centrale: titolo + versione + changelog. Lo scroll è SOLO nel box changelog
        // (altezza fissa), così la colonna esterna resta corta e i bottoni non vengono spinti via.
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.update_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = onBg,
            )
            Text(
                stringResource(R.string.update_available, info.version),
                style = MaterialTheme.typography.bodyLarge,
                color = onBg,
                modifier = Modifier.padding(top = 12.dp),
            )
            // Novità: box scorrevole ad ALTEZZA FISSA, così un changelog lungo non spinge via i
            // bottoni. Mostra le note per versione (dalla installata esclusa alla disponibile).
            if (info.entries.isNotEmpty()) {
                ChangelogBox(info.entries, onBg)
            } else if (info.changelog.isNotBlank()) {
                // Fallback: body grezzo della release, comunque dentro un box scorrevole.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        info.changelog,
                        style = MaterialTheme.typography.bodyMedium,
                        color = onBg.copy(alpha = 0.9f),
                    )
                }
            }
            if (state.downloading) {
                Text(
                    stringResource(R.string.update_downloading, state.progress),
                    style = MaterialTheme.typography.bodyMedium,
                    color = onBg,
                    modifier = Modifier.padding(top = 24.dp),
                )
                LinearProgressIndicator(
                    progress = { state.progress / 100f },
                    color = onBg,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
            state.error?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }

        // Bottoni in basso: Scarica/Installa + Più tardi (nascosti durante il download).
        if (!state.downloading) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Button(
                    onClick = {
                        if (canInstall) {
                            onUpdate()
                        } else {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                    Uri.parse("package:${context.packageName}"),
                                ),
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = onBg,
                        contentColor = bg,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (canInstall) stringResource(R.string.update_action)
                        else stringResource(R.string.update_allow_install),
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.update_later), color = onBg)
                }
            }
        }
    }
}

/**
 * Box scorrevole ad altezza fissa con le novità per versione (titolo "Novità" + sezioni
 * "versione" ed elenco puntato). Altezza limitata così un changelog lungo non spinge via i bottoni.
 */
@Composable
private fun ChangelogBox(entries: List<ChangelogEntry>, onBg: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .heightIn(max = 240.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            stringResource(R.string.update_whats_new),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = onBg,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        entries.forEach { entry ->
            Text(
                entry.version.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = onBg,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
            )
            entry.lines.forEach { line ->
                Text(
                    "•  $line",
                    style = MaterialTheme.typography.bodyMedium,
                    color = onBg.copy(alpha = 0.9f),
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                )
            }
        }
    }
}
