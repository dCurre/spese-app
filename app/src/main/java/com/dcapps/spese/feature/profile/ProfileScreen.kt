package com.dcapps.spese.feature.profile

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
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
import com.dcapps.spese.feature.feedback.FeedbackDialog
import com.dcapps.spese.ui.components.SpeseTopAppBar
import com.dcapps.spese.ui.theme.SpeseTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onOpenProfileDetail: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenStatsDashboard: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenAdmin: () -> Unit,
    onCheckUpdates: () -> Unit,
    updateChecking: Boolean,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isAdmin = user?.role == "admin" || user?.role == "superadmin"
    var showFeedback by remember { mutableStateOf(false) }

    val context = LocalContext.current
    // Stato "notifiche abilitate": ricalcolato a ogni ritorno in primo piano (ON_RESUME), così se
    // l'utente le riattiva dalle impostazioni di sistema la riga si aggiorna al rientro nell'app.
    var notificationsEnabled by remember {
        mutableStateOf(
            androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled(),
        )
    }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                notificationsEnabled =
                    androidx.core.app.NotificationManagerCompat.from(context)
                        .areNotificationsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showFeedback) {
        FeedbackDialog(onDismiss = { showFeedback = false })
    }

    Scaffold(
        topBar = { SpeseTopAppBar(title = stringResource(R.string.profile_title)) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
        ) {
            // Intestazione account: avatar (non cliccabile) con sotto il nome + freccetta,
            // allineati a sinistra. Solo il nome apre il dettaglio account.
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                val img = state.photo
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
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(96.dp),
                    )
                }
                // Nome + freccetta: cliccabile, apre il dettaglio account.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable(onClick = onOpenProfileDetail)
                        .padding(top = 12.dp, bottom = 4.dp, end = 8.dp),
                ) {
                    Text(
                        state.displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 4.dp).size(28.dp),
                    )
                }
            }

            // Gruppo: Impostazioni, Statistiche (righe + divisori, stile liste).
            Spacer(Modifier.height(16.dp))
            ProfileGroupDivider()
            ProfileListItem(
                icon = Icons.Default.Settings,
                title = stringResource(R.string.profile_settings),
                subtitle = stringResource(R.string.profile_settings_subtitle),
                onClick = onOpenSettings,
            )
            ProfileGroupDivider()
            ProfileListItem(
                icon = Icons.Default.QueryStats,
                title = stringResource(R.string.profile_stats),
                onClick = onOpenStats,
            )
            ProfileGroupDivider()
            ProfileListItem(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                title = stringResource(R.string.stats_title),
                subtitle = stringResource(R.string.stats_subtitle),
                onClick = onOpenStatsDashboard,
            )
            ProfileGroupDivider()

            // Gruppo staccato: Controlla aggiornamenti, Amministrazione (admin), Informazioni.
            Spacer(Modifier.height(24.dp))
            ProfileGroupDivider()
            ProfileListItem(
                icon = Icons.Default.Refresh,
                title = stringResource(R.string.profile_check_updates),
                onClick = { if (!updateChecking) onCheckUpdates() },
                trailing = if (updateChecking) {
                    { CircularProgressIndicator(Modifier.size(20.dp)) }
                } else null,
            )
            ProfileGroupDivider()
            // Notifiche di aggiornamento: stato attuale + tap per aprire le impostazioni notifiche
            // dell'app (unico modo per riattivarle se l'utente le ha negate).
            ProfileListItem(
                icon = Icons.Default.Notifications,
                title = stringResource(R.string.profile_update_notifications),
                subtitle = stringResource(
                    if (notificationsEnabled) R.string.profile_notifications_on
                    else R.string.profile_notifications_off,
                ),
                onClick = { openAppNotificationSettings(context) },
            )
            ProfileGroupDivider()
            ProfileListItem(
                icon = Icons.Default.Lightbulb,
                title = stringResource(R.string.profile_feedback),
                onClick = { showFeedback = true },
            )
            ProfileGroupDivider()
            if (isAdmin) {
                ProfileListItem(
                    icon = Icons.Default.AdminPanelSettings,
                    title = stringResource(R.string.profile_admin),
                    onClick = onOpenAdmin,
                )
                ProfileGroupDivider()
            }
            ProfileListItem(
                icon = Icons.Default.Info,
                title = stringResource(R.string.profile_about),
                onClick = onOpenAbout,
            )
            ProfileGroupDivider()
        }
    }
}

@Composable
private fun ProfileGroupDivider() {
    HorizontalDivider(
        color = SpeseTheme.colors.divider,
    )
}

/**
 * Apre le impostazioni notifiche dell'app (Android 8+). Da qui l'utente può riattivare le notifiche
 * di aggiornamento se le aveva negate. Fallback alle impostazioni generali dell'app su versioni più
 * vecchie o se l'intent dedicato non è gestito.
 */
private fun openAppNotificationSettings(context: android.content.Context) {
    val intent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else {
        android.content.Intent(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            android.net.Uri.fromParts("package", context.packageName, null),
        )
    }
    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
