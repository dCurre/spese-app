package com.dcapps.spese.feature.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dcapps.spese.core.auth.AuthManager
import com.dcapps.spese.core.auth.SessionManager
import com.dcapps.spese.core.data.UserRepository
import com.dcapps.spese.core.model.UpdateUserRequest
import com.dcapps.spese.core.model.User
import com.dcapps.spese.core.network.ApiResult
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
                    // Conserva la preferenza tema corrente: il reload delle statistiche non
                    // deve sovrascrivere un tema appena scelto dall'utente (che potrebbe non
                    // essere ancora stato persistito sul server).
                    val currentTheme = session.currentUser.value?.themePreference
                    val freshUser = result.data.user.copy(
                        themePreference = currentTheme ?: result.data.user.themePreference,
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

    fun setTheme(theme: String) {
        val user = session.currentUser.value ?: return
        // Aggiorna subito la sessione (il tema si applica live), poi persisti.
        session.setUser(user.copy(themePreference = theme))
        _uiState.update { it.copy(user = user.copy(themePreference = theme)) }
        viewModelScope.launch {
            userRepository.update(user.id, UpdateUserRequest(themePreference = theme))
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
                _uiState.update { it.copy(uploading = false, message = "Impossibile leggere l'immagine") }
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
        viewModelScope.launch {
            when (val result = userRepository.selectProfileImage(user.id, url)) {
                is ApiResult.Success -> {
                    session.setUser(
                        user.copy(
                            profileImage = result.data.url,
                            profileImagesHistory = result.data.history,
                        )
                    )
                    _uiState.update { it.copy(user = session.currentUser.value) }
                }
                is ApiResult.Error -> _uiState.update { it.copy(message = result.message) }
            }
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch { authManager.signOut(context) }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }
}
