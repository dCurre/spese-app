package com.dcapps.spese

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.animation.AnticipateInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dcapps.spese.core.bugreport.CrashStore
import com.dcapps.spese.core.locale.LocaleManager
import com.dcapps.spese.core.update.UpdateNotifier
import com.dcapps.spese.feature.auth.AuthViewModel
import com.dcapps.spese.feature.auth.MigrationChoiceDialog
import com.dcapps.spese.feature.auth.OfflineChoiceScreen
import com.dcapps.spese.feature.auth.StartDestination
import com.dcapps.spese.feature.bugreport.BugReportDialog
import com.dcapps.spese.feature.bugreport.BugReportGateViewModel
import com.dcapps.spese.feature.bugreport.BugReportViewModel
import com.dcapps.spese.feature.auth.SignInScreen
import com.dcapps.spese.feature.update.UpdateScreen
import com.dcapps.spese.feature.update.UpdateViewModel
import com.dcapps.spese.feature.update.WhatsNewDialog
import com.dcapps.spese.navigation.SpeseNavHost
import com.dcapps.spese.ui.components.LoadingIndicator
import com.dcapps.spese.ui.theme.SpeseTheme
import com.dcapps.spese.ui.theme.ThemePreference
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // True quando l'app è stata aperta toccando la notifica di aggiornamento: in tal caso la
    // UpdateGate apre direttamente la schermata di update (senza attendere il check in-app).
    private val openUpdateFromNotification =
        androidx.compose.runtime.mutableStateOf(false)

    // Richiesta runtime del permesso notifiche (Android 13+): il risultato non ci serve qui, il
    // worker controlla canNotify() da sé; chiediamo solo una volta per abilitare le notifiche.
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        openUpdateFromNotification.value =
            intent?.getBooleanExtra(UpdateNotifier.EXTRA_OPEN_UPDATE, false) == true
        requestNotificationPermissionIfNeeded()

        // Animazione d'uscita dello splash: la banconota "corre via" verso destra (slide-out +
        // fade). Si anima l'iconView (così scorre via solo l'icona, non lo sfondo); l'iconView può
        // essere null su alcuni device (NPE noto di core-splashscreen 1.0.1, b/243457485, fixato in
        // 1.1.0+/1.2.0), quindi si ricade sulla view intera. In più tutto è avvolto in try/catch:
        // se qualsiasi cosa è null o lancia, si rimuove comunque lo splash senza far crashare l'app.
        splashScreen.setOnExitAnimationListener { provider ->
            try {
                val target = provider.iconView ?: provider.view
                val slide = ObjectAnimator.ofFloat(
                    target, "translationX", 0f, provider.view.width.toFloat() * 1.2f,
                )
                val fade = ObjectAnimator.ofFloat(target, "alpha", 1f, 0f)
                slide.interpolator = AnticipateInterpolator(1.2f)
                slide.duration = 650
                fade.duration = 650
                slide.doOnEnd { provider.remove() }
                slide.start()
                fade.start()
            } catch (t: Throwable) {
                // Fallback sicuro: niente animazione, ma lo splash sparisce e l'app parte.
                provider.remove()
            }
        }

        enableEdgeToEdge()
        setContent { SpeseApp(openUpdate = openUpdateFromNotification.value) }
    }

    // Se l'app è già aperta quando si tocca la notifica, aggiorna il flag (riusa la stessa task).
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(UpdateNotifier.EXTRA_OPEN_UPDATE, false)) {
            openUpdateFromNotification.value = true
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun SpeseApp(
    openUpdate: Boolean = false,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()

    // Tema dell'APP (campo separato da quello del frontend web).
    val preference = when (user?.appThemePreference) {
        "default" -> ThemePreference.DEFAULT
        "light" -> ThemePreference.LIGHT
        "dark" -> ThemePreference.DARK
        "legacy" -> ThemePreference.LEGACY
        "auto" -> ThemePreference.AUTO
        else -> ThemePreference.DEFAULT
    }

    // Applica la lingua dell'APP (campo separato dal web) quando l'utente è disponibile (es. dopo
    // il login o il restore di sessione). Si aggiorna solo se diversa da quella corrente, per
    // evitare ricreazioni inutili dell'Activity.
    val savedLanguage = user?.appLanguagePreference
    androidx.compose.runtime.LaunchedEffect(savedLanguage) {
        if (!savedLanguage.isNullOrBlank() &&
            savedLanguage != LocaleManager.currentLanguage()
        ) {
            LocaleManager.setLanguage(savedLanguage)
        }
    }

    SpeseTheme(preference = preference) {
        when (state.destination) {
            StartDestination.RESTORING -> Scaffold(Modifier.fillMaxSize()) { p ->
                LoadingScreen(Modifier.padding(p))
            }
            // APP vale sia per utente loggato sia per guest (local-first): entrambi usano il NavHost.
            StartDestination.APP -> SpeseNavHost()
            StartDestination.SIGN_IN -> Scaffold(Modifier.fillMaxSize()) { p ->
                SignInScreen(
                    state = state,
                    onGoogleSignIn = { ctx -> viewModel.signIn(ctx) },
                    onContinueAsGuest = { viewModel.continueAsGuest() },
                    modifier = Modifier.padding(p),
                )
            }
            StartDestination.OFFLINE_CHOICE -> Scaffold(Modifier.fillMaxSize()) { p ->
                OfflineChoiceScreen(
                    onRetry = { viewModel.retryRestore() },
                    onContinueAsGuest = { viewModel.continueAsGuest() },
                    modifier = Modifier.padding(p),
                )
            }
        }

        // Upgrade guest→account: dopo un login da ospite con dati locali, chiedi cosa farne.
        if (state.showMigrationChoice) {
            MigrationChoiceDialog(
                inProgress = state.migrationInProgress,
                onMerge = { viewModel.mergeGuestData() },
                onOnlineOnly = { viewModel.discardGuestData() },
            )
        }

        // Controllo aggiornamenti: mostra il popup se su GitHub c'è una versione maggiore.
        // openUpdate = true quando si arriva dalla notifica (apri diretto, niente doppio dialog).
        UpdateGate(openUpdate = openUpdate)

        // Segnalazione bug: dopo un crash (all'avvio) o su richiesta (azione "Segnala"
        // delle snackbar di errore).
        BugReportGate()
    }
}

@Composable
private fun BugReportGate(
    bugReportViewModel: BugReportViewModel = hiltViewModel(),
    controllerVm: BugReportGateViewModel = hiltViewModel(),
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val requested by controllerVm.visible.collectAsStateWithLifecycle()

    // All'avvio: se c'è un crash salvato, apri subito la segnalazione.
    var crashPending by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(
            CrashStore.read(context) != null,
        )
    }

    val show = crashPending || requested
    androidx.compose.runtime.LaunchedEffect(show) {
        if (show) bugReportViewModel.prefillFromLastError()
    }
    if (show) {
        BugReportDialog(
            onDismiss = {
                crashPending = false
                controllerVm.dismiss()
            },
            viewModel = bugReportViewModel,
        )
    }
}

@Composable
private fun UpdateGate(
    openUpdate: Boolean = false,
    updateViewModel: UpdateViewModel = hiltViewModel(),
) {
    val updateState by updateViewModel.uiState.collectAsStateWithLifecycle()

    // Arrivo dalla notifica: forza subito un check così la schermata di update appare senza
    // attendere l'ON_RESUME (che comunque la popolerebbe poco dopo).
    androidx.compose.runtime.LaunchedEffect(openUpdate) {
        if (openUpdate) updateViewModel.checkForUpdate()
    }

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
        UpdateScreen(
            state = updateState,
            info = info,
            canInstall = updateViewModel.canInstall(),
            onUpdate = updateViewModel::onUpdateClick,
            onDismiss = updateViewModel::dismiss,
        )
    }

    // Novità post-update (modale) al primo avvio sulla nuova versione; non insieme all'UpdateScreen.
    if (!updateState.showDialog && updateState.showWhatsNew) {
        WhatsNewDialog(
            entries = updateState.whatsNew,
            onDismiss = updateViewModel::dismissWhatsNew,
        )
    }
}

@Composable
private fun LoadingScreen(modifier: Modifier = Modifier) {
    LoadingIndicator(modifier)
}
