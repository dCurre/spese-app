package com.dcapps.spese

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.animation.AnticipateInterpolator
import androidx.core.animation.doOnEnd
import androidx.activity.compose.setContent
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
import com.dcapps.spese.feature.auth.AuthViewModel
import com.dcapps.spese.feature.bugreport.BugReportGateViewModel
import com.dcapps.spese.feature.auth.SignInScreen
import com.dcapps.spese.feature.update.UpdateScreen
import com.dcapps.spese.feature.update.UpdateViewModel
import com.dcapps.spese.navigation.SpeseNavHost
import com.dcapps.spese.ui.theme.SpeseTheme
import com.dcapps.spese.ui.theme.ThemePreference
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

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
        setContent { SpeseApp() }
    }
}

@Composable
private fun SpeseApp(viewModel: AuthViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()

    val preference = when (user?.themePreference) {
        "default" -> ThemePreference.DEFAULT
        "light" -> ThemePreference.LIGHT
        "dark" -> ThemePreference.DARK
        "legacy" -> ThemePreference.LEGACY
        "auto" -> ThemePreference.AUTO
        else -> ThemePreference.DEFAULT
    }

    // Applica la lingua salvata sul backend quando l'utente è disponibile (es. dopo il login o
    // il restore di sessione). Si aggiorna solo se diversa da quella corrente, per evitare
    // ricreazioni inutili dell'Activity.
    val savedLanguage = user?.languagePreference
    androidx.compose.runtime.LaunchedEffect(savedLanguage) {
        if (!savedLanguage.isNullOrBlank() &&
            savedLanguage != com.dcapps.spese.core.locale.LocaleManager.currentLanguage()
        ) {
            com.dcapps.spese.core.locale.LocaleManager.setLanguage(savedLanguage)
        }
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
        UpdateScreen(
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
