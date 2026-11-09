package com.dcapps.spese.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dcapps.spese.R

/**
 * Schermata di login. Sfondo verde coerente con l'icona dell'app; unico metodo di accesso:
 * bottone "Accedi con Google" nello stile ufficiale (sfondo bianco, logo G, testo).
 */
@Composable
fun SignInScreen(
    state: AuthUiState,
    onGoogleSignIn: (android.content.Context) -> Unit,
    onContinueAsGuest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1D9E75))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displaySmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.signin_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 48.dp),
        )

        if (state.isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp), color = Color.White)
        } else {
            GoogleSignInButton(onClick = { onGoogleSignIn(context) })
            // Ingresso senza account (guest-first): usa l'app in locale, login opzionale dopo.
            TextButton(
                onClick = onContinueAsGuest,
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Text(
                    text = stringResource(R.string.signin_continue_guest),
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        state.error?.let { error ->
            Text(
                text = error,
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
    }
}

/** Bottone "Accedi con Google" in stile ufficiale: superficie bianca, logo G, testo scuro. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoogleSignInButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.height(48.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_google_logo),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(R.string.signin_google),
                color = Color(0xFF1F1F1F),
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}
