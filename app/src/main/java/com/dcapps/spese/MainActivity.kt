package com.dcapps.spese

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dcapps.spese.feature.auth.AuthViewModel
import com.dcapps.spese.feature.auth.SignInScreen
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
    }
}

@Composable
private fun LoadingScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
    }
}
