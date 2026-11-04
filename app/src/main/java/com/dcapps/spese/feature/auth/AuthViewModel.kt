package com.dcapps.spese.feature.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dcapps.spese.core.auth.AuthManager
import com.dcapps.spese.core.auth.AuthResult
import com.dcapps.spese.core.auth.SessionManager
import com.dcapps.spese.core.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isRestoring: Boolean = true,
    val user: User? = null,
    val error: String? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authManager: AuthManager,
    session: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val currentUser: StateFlow<User?> = session.currentUser

    init {
        restoreSession()
    }

    private fun restoreSession() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRestoring = true)
            val restored = authManager.restoreSession()
            _uiState.value = _uiState.value.copy(isRestoring = false, user = currentUser.value)
            if (!restored) _uiState.value = _uiState.value.copy(user = null)
        }
    }

    fun signIn(activityContext: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = authManager.signInWithGoogle(activityContext)) {
                is AuthResult.Success ->
                    _uiState.value = _uiState.value.copy(isLoading = false, user = result.user)
                is AuthResult.Error ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                AuthResult.Cancelled ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            authManager.signOut(context)
            _uiState.value = _uiState.value.copy(user = null)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
