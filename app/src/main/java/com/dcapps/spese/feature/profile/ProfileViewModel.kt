package com.dcapps.spese.feature.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dcapps.spese.R
import com.dcapps.spese.core.auth.AuthManager
import com.dcapps.spese.core.auth.SessionManager
import com.dcapps.spese.core.data.UserRepository
import com.dcapps.spese.core.model.UpdateUserRequest
import com.dcapps.spese.core.model.User
import com.dcapps.spese.core.network.ApiResult
import com.dcapps.spese.core.ui.launchMutation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileStats(
    val listsOwned: Int = 0,
    val listsParticipating: Int = 0,
    val expenses: Int = 0,
)

data class ProfileUiState(
    val user: User? = null,
    val stats: ProfileStats = ProfileStats(),
    val statsLoaded: Boolean = false,
    val uploading: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authManager: AuthManager,
    private val session: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(user = session.currentUser.value))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    val currentUser: StateFlow<User?> = session.currentUser

    init {
        loadStats()
    }

    private fun loadStats() {
        val user = session.currentUser.value ?: return
        viewModelScope.launch {
            when (val result = userRepository.getWithLists(user.email)) {
                is ApiResult.Success -> {
                    // Conserva tema/lingua APP correnti: il reload delle statistiche non deve
                    // sovrascrivere una scelta appena fatta dall'utente (che potrebbe non essere
                    // ancora stata persistita sul server).
                    val current = session.currentUser.value
                    val freshUser = result.data.user.copy(
                        appThemePreference = current?.appThemePreference ?: result.data.user.appThemePreference,
                        appLanguagePreference = current?.appLanguagePreference ?: result.data.user.appLanguagePreference,
                    )
                    session.setUser(freshUser)
                    val lists = result.data.expensesLists
                    _uiState.update {
                        it.copy(
                            user = freshUser,
                            statsLoaded = true,
                            stats = ProfileStats(
                                listsOwned = lists.count { l -> l.userId == user.id },
                                listsParticipating = lists.count { l -> l.userId != user.id },
                                expenses = lists.sumOf { l -> l.expensesCount ?: 0 },
                            ),
                        )
                    }
                }
                is ApiResult.Error ->
                    _uiState.update { it.copy(statsLoaded = true, message = result.message) }
            }
        }
    }

    /** Modifica nome e cognome dell'utente. */
    fun updateNameSurname(name: String, surname: String) {
        val user = session.currentUser.value ?: return
        launchMutation(
            onError = { e -> _uiState.update { it.copy(message = e.message) } },
            onSuccess = {
                session.setUser(user.copy(name = name.trim(), surname = surname.trim()))
                _uiState.update { it.copy(user = session.currentUser.value) }
            },
            call = {
                userRepository.update(
                    user.id,
                    UpdateUserRequest(name = name.trim(), surname = surname.trim()),
                )
            },
        )
    }

    fun setTheme(theme: String) {
        val user = session.currentUser.value ?: return
        // Tema SOLO dell'app (campo separato dal web). Aggiorna subito la sessione (si applica live).
        session.setUser(user.copy(appThemePreference = theme))
        _uiState.update { it.copy(user = user.copy(appThemePreference = theme)) }
        viewModelScope.launch {
            userRepository.update(user.id, UpdateUserRequest(appThemePreference = theme))
        }
    }

    /**
     * Cambia la lingua: aggiorna la sessione (sorgente di verità) e persiste sul backend.
     * NON chiama direttamente LocaleManager: l'applicazione effettiva della locale è reattiva al
     * cambiamento della sessione (LaunchedEffect in MainActivity), esattamente come per il tema.
     * Così c'è una sola applicazione della lingua e si evita il flicker alla prima scelta.
     */
    fun setLanguage(language: String) {
        val user = session.currentUser.value ?: return
        // Lingua SOLO dell'app (campo separato dal web). L'applicazione effettiva è reattiva
        // al cambiamento della sessione (LaunchedEffect in MainActivity), come per il tema.
        session.setUser(user.copy(appLanguagePreference = language))
        _uiState.update { it.copy(user = user.copy(appLanguagePreference = language)) }
        viewModelScope.launch {
            userRepository.update(user.id, UpdateUserRequest(appLanguagePreference = language))
        }
    }

    fun uploadAvatar(context: Context, uri: Uri) {
        val user = session.currentUser.value ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(uploading = true, message = null) }
            val resolver = context.contentResolver
            val bytes = runCatching {
                resolver.openInputStream(uri)?.use { it.readBytes() }
            }.getOrNull()
            if (bytes == null) {
                _uiState.update { it.copy(uploading = false, message = context.getString(R.string.vm_image_read_error)) }
                return@launch
            }
            val mime = resolver.getType(uri) ?: "image/jpeg"
            val ext = when {
                mime.contains("png") -> "png"
                mime.contains("webp") -> "webp"
                mime.contains("gif") -> "gif"
                else -> "jpg"
            }
            when (val result = userRepository.uploadProfileImage(user.id, bytes, "avatar.$ext", mime)) {
                is ApiResult.Success -> {
                    session.setUser(
                        user.copy(
                            profileImage = result.data.url,
                            profileImagesHistory = result.data.history,
                        )
                    )
                    _uiState.update { it.copy(uploading = false, user = session.currentUser.value) }
                }
                is ApiResult.Error ->
                    _uiState.update { it.copy(uploading = false, message = result.message) }
            }
        }
    }

    fun selectAvatar(url: String) {
        val user = session.currentUser.value ?: return
        launchMutation(
            onError = { e -> _uiState.update { it.copy(message = e.message) } },
            onSuccess = { data ->
                session.setUser(
                    user.copy(profileImage = data.url, profileImagesHistory = data.history)
                )
                _uiState.update { it.copy(user = session.currentUser.value) }
            },
            call = { userRepository.selectProfileImage(user.id, url) },
        )
    }

    fun signOut(context: Context) {
        viewModelScope.launch { authManager.signOut(context) }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }
}
