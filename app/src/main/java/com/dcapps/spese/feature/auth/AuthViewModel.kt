package com.dcapps.spese.feature.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dcapps.spese.core.auth.AuthManager
import com.dcapps.spese.core.auth.AuthResult
import com.dcapps.spese.core.auth.RestoreResult
import com.dcapps.spese.core.auth.SessionManager
import com.dcapps.spese.core.model.User
import com.dcapps.spese.core.sync.ChecklistSyncEngine
import com.dcapps.spese.core.sync.ExpensesSyncEngine
import com.dcapps.spese.core.sync.GuestMigration
import com.dcapps.spese.core.sync.PricesSyncEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Schermata da mostrare all'avvio dopo il ripristino sessione. */
enum class StartDestination { RESTORING, SIGN_IN, APP, OFFLINE_CHOICE }

data class AuthUiState(
    val isLoading: Boolean = false,
    val isRestoring: Boolean = true,
    val destination: StartDestination = StartDestination.RESTORING,
    val user: User? = null,
    val error: String? = null,
    // Upgrade guest→account: dopo un login da ospite con dati locali, chiedi "Unisci / Solo online".
    val showMigrationChoice: Boolean = false,
    // In corso l'applicazione della scelta di migrazione (blocca il dialog).
    val migrationInProgress: Boolean = false,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val session: SessionManager,
    private val checklistSync: ChecklistSyncEngine,
    private val expensesSync: ExpensesSyncEngine,
    private val pricesSync: PricesSyncEngine,
    private val guestMigration: GuestMigration,
) : ViewModel() {

    /** Allinea col backend tutte le sezioni local-first (no-op in guest). */
    private fun syncAll() {
        viewModelScope.launch { checklistSync.sync() }
        viewModelScope.launch { expensesSync.sync() }
        viewModelScope.launch { pricesSync.sync() }
    }

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val currentUser: StateFlow<User?> = session.currentUser
    val isGuest: StateFlow<Boolean> = session.isGuestFlow

    init {
        restoreSession()
    }

    private fun restoreSession() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRestoring = true, destination = StartDestination.RESTORING)
            when (authManager.restoreSession()) {
                RestoreResult.LOGGED_IN -> {
                    _uiState.value = _uiState.value.copy(isRestoring = false, user = currentUser.value, destination = StartDestination.APP)
                    syncAll() // allinea col backend all'avvio
                }
                RestoreResult.NONE -> {
                    // Mai loggato → guest-first: entra come ospite.
                    session.enterGuest()
                    _uiState.value = _uiState.value.copy(isRestoring = false, user = null, destination = StartDestination.APP)
                }
                RestoreResult.OFFLINE_NO_CACHE ->
                    _uiState.value = _uiState.value.copy(isRestoring = false, user = null, destination = StartDestination.OFFLINE_CHOICE)
            }
        }
    }

    /** Dalla schermata "sei offline": continua come ospite. */
    fun continueAsGuest() {
        session.enterGuest()
        _uiState.value = _uiState.value.copy(destination = StartDestination.APP)
    }

    /** Dalla schermata "sei offline": riprova il ripristino (es. tornata la rete). */
    fun retryRestore() = restoreSession()

    fun signIn(activityContext: Context) {
        viewModelScope.launch {
            // Catturato PRIMA del login: signInWithGoogle fa setUser() che spegne la modalità guest.
            val wasGuest = session.isGuest
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = authManager.signInWithGoogle(activityContext)) {
                is AuthResult.Success -> {
                    // Upgrade da ospite con dati locali → chiedi cosa farne prima di sincronizzare.
                    if (wasGuest && guestMigration.hasLocalData()) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false, user = result.user,
                            destination = StartDestination.APP, showMigrationChoice = true,
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false, user = result.user, destination = StartDestination.APP)
                        syncAll() // primo allineamento dopo il login
                    }
                }
                is AuthResult.Error ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                AuthResult.Cancelled ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    /** Upgrade "Unisci": i dati guest diventano dell'account (ricreati via sync). */
    fun mergeGuestData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(migrationInProgress = true)
            guestMigration.merge()
            _uiState.value = _uiState.value.copy(showMigrationChoice = false, migrationInProgress = false)
            syncAll() // ricrea sul backend i dati locali e poi fa il pull
        }
    }

    /** Upgrade "Solo online": scarta i dati guest locali, usa quelli dell'account. */
    fun discardGuestData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(migrationInProgress = true)
            guestMigration.discardLocal()
            _uiState.value = _uiState.value.copy(showMigrationChoice = false, migrationInProgress = false)
            syncAll() // pull dei dati dell'account
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            authManager.signOut(context)
            // Dopo il logout: schermata di accesso (NON guest in automatico).
            _uiState.value = _uiState.value.copy(user = null, destination = StartDestination.SIGN_IN)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
