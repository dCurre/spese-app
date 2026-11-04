package com.dcapps.spese.core.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.dcapps.spese.BuildConfig
import com.dcapps.spese.core.data.UserRepository
import com.dcapps.spese.core.model.UpsertUserRequest
import com.dcapps.spese.core.model.User
import com.dcapps.spese.core.network.ApiResult
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Esito di un tentativo di login. */
sealed interface AuthResult {
    data class Success(val user: User) : AuthResult
    data class Error(val message: String) : AuthResult
    /** L'utente ha annullato il selettore account Google. */
    data object Cancelled : AuthResult
}

/**
 * Orchestratore dell'autenticazione:
 * 1. Credential Manager → Google ID token
 * 2. Firebase sign-in con quella credenziale
 * 3. upsert-by-email sul backend → record utente PG
 * 4. aggiorna [SessionManager]
 */
@Singleton
class AuthManager @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val userRepository: UserRepository,
    private val session: SessionManager,
) {

    /** Esegue il login con Google. Richiede un [Context] di Activity per il bottom sheet. */
    suspend fun signInWithGoogle(activityContext: Context): AuthResult {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credentialManager = CredentialManager.create(activityContext)

        val idToken = try {
            val response = credentialManager.getCredential(activityContext, request)
            val credential = response.credential
            if (credential is androidx.credentials.CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                GoogleIdTokenCredential.createFrom(credential.data).idToken
            } else {
                return AuthResult.Error("Credenziale Google non valida")
            }
        } catch (e: GetCredentialCancellationException) {
            // Vero annullamento utente (ha chiuso il selettore): nessun errore.
            return AuthResult.Cancelled
        } catch (e: NoCredentialException) {
            // Nessun account Google disponibile per questo client: tipicamente SHA non
            // registrato su Firebase, oppure nessun account Google sul dispositivo.
            return AuthResult.Error(
                "Nessun account Google disponibile. Verifica di avere un account Google sul " +
                    "dispositivo e che l'app sia autorizzata (firma registrata su Firebase)."
            )
        } catch (e: GetCredentialException) {
            return AuthResult.Error("Login Google non riuscito: ${e.message ?: e.javaClass.simpleName}")
        } catch (e: Exception) {
            return AuthResult.Error(e.message ?: "Errore durante il login Google")
        }

        // Firebase sign-in con il Google ID token
        val firebaseUser = try {
            val authCredential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(authCredential).await().user
        } catch (e: Exception) {
            return AuthResult.Error(e.message ?: "Autenticazione Firebase fallita")
        } ?: return AuthResult.Error("Utente Firebase nullo")

        val email = firebaseUser.email
            ?: return AuthResult.Error("Email non disponibile dall'account Google")

        val (name, surname) = splitDisplayName(firebaseUser.displayName)

        return when (val result = userRepository.upsertByEmail(
            UpsertUserRequest(
                email = email,
                name = name,
                surname = surname,
                profileImage = firebaseUser.photoUrl?.toString() ?: "",
            )
        )) {
            is ApiResult.Success -> {
                session.setUser(result.data)
                AuthResult.Success(result.data)
            }
            is ApiResult.Error -> {
                // Login backend fallito: annulla anche la sessione Firebase per coerenza.
                firebaseAuth.signOut()
                AuthResult.Error(result.message)
            }
        }
    }

    /**
     * Ripristina la sessione all'avvio se Firebase ha già un utente loggato.
     * @return true se la sessione è stata ripristinata.
     */
    suspend fun restoreSession(): Boolean {
        val email = firebaseAuth.currentUser?.email ?: return false
        return when (val result = userRepository.getByEmail(email)) {
            is ApiResult.Success -> {
                session.setUser(result.data)
                true
            }
            is ApiResult.Error -> false
        }
    }

    suspend fun signOut(context: Context) {
        firebaseAuth.signOut()
        runCatching {
            CredentialManager.create(context)
                .clearCredentialState(ClearCredentialStateRequest())
        }
        session.clear()
    }

    private fun splitDisplayName(displayName: String?): Pair<String, String> {
        if (displayName.isNullOrBlank()) return "" to ""
        val parts = displayName.trim().split(" ", limit = 2)
        val name = parts.getOrElse(0) { "" }
        val surname = parts.getOrElse(1) { "" }
        return name to surname
    }
}
