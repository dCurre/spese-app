package com.dcapps.spese

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dcapps.spese.feature.auth.AuthViewModel
import com.dcapps.spese.feature.bugreport.BugReportGateViewModel
import com.dcapps.spese.feature.auth.SignInScreen
import com.dcapps.spese.feature.update.UpdateDialog
import com.dcapps.spese.feature.update.UpdateViewModel
import com.dcapps.spese.navigation.SpeseNavHost
import com.dcapps.spese.ui.theme.SpeseTheme
import com.dcapps.spese.ui.theme.ThemePreference
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SpeseApp() }
    }
}

@Composable
private fun SpeseApp(viewModel: AuthViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()

    val preference = when (user?.themePreference) {
        "light" -> ThemePreference.LIGHT
        "dark" -> ThemePreference.DARK
        "legacy" -> ThemePreference.LEGACY
        else -> ThemePreference.AUTO
    }

    SpeseTheme(preference = preference) {
        when {
            state.isRestoring -> Scaffold(Modifier.fillMaxSize()) { p ->
                LoadingScreen(Modifier.padding(p))
            }
            user != null -> {
                // Navigazione dell'area loggata (ogni schermata ha la propria Scaffold).
                SpeseNavHost()
            }
            else -> Scaffold(Modifier.fillMaxSize()) { p ->
                SignInScreen(
                    state = state,
                    onGoogleSignIn = { ctx -> viewModel.signIn(ctx) },
                    modifier = Modifier.padding(p),
                )
            }
        }

        // Controllo aggiornamenti: mostra il popup se su GitHub c'è una versione maggiore.
        UpdateGate()

        // Segnalazione bug: dopo un crash (all'avvio) o su richiesta (azione "Segnala"
        // delle snackbar di errore).
        BugReportGate()
    }
}

@Composable
private fun BugReportGate(
    bugReportViewModel: com.dcapps.spese.feature.bugreport.BugReportViewModel = hiltViewModel(),
    controllerVm: BugReportGateViewModel = hiltViewModel(),
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val requested by controllerVm.visible.collectAsStateWithLifecycle()

    // All'avvio: se c'è un crash salvato, apri subito la segnalazione.
    var crashPending by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(
            com.dcapps.spese.core.bugreport.CrashStore.read(context) != null,
        )
    }

    val show = crashPending || requested
    androidx.compose.runtime.LaunchedEffect(show) {
        if (show) bugReportViewModel.prefillFromLastError()
    }
    if (show) {
        com.dcapps.spese.feature.bugreport.BugReportDialog(
            onDismiss = {
                crashPending = false
                controllerVm.dismiss()
            },
            viewModel = bugReportViewModel,
        )
    }
}

@Composable
private fun UpdateGate(updateViewModel: UpdateViewModel = hiltViewModel()) {
    val updateState by updateViewModel.uiState.collectAsStateWithLifecycle()

    // Ricontrolla gli aggiornamenti ogni volta che l'app torna in primo piano.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                updateViewModel.checkForUpdate()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val info = updateState.info
    if (updateState.showDialog && info != null) {
        UpdateDialog(
            state = updateState,
            info = info,
            canInstall = updateViewModel.canInstall(),
            onUpdate = updateViewModel::onUpdateClick,
            onDismiss = updateViewModel::dismiss,
        )
    }
}

@Composable
private fun LoadingScreen(modifier: Modifier = Modifier) {
    com.dcapps.spese.ui.components.LoadingIndicator(modifier)
}
