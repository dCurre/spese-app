package com.dcapps.spese.core.auth

import com.dcapps.spese.core.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Stato della sessione utente, condiviso in tutta l'app. */
@Singleton
class SessionManager @Inject constructor() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Modalità guest: l'utente usa l'app in locale (Room) senza account backend. Determina a quale
    // sorgente dati delegano i repository (local vs remote).
    private val _isGuest = MutableStateFlow(false)
    val isGuestFlow: StateFlow<Boolean> = _isGuest.asStateFlow()
    val isGuest: Boolean get() = _isGuest.value

    val isLoggedIn: Boolean get() = _currentUser.value != null

    /** Imposta l'utente loggato (esce dalla modalità guest). */
    fun setUser(user: User?) {
        _currentUser.value = user
        if (user != null) _isGuest.value = false
    }

    /** Entra in modalità guest (nessun utente backend). */
    fun enterGuest() {
        _currentUser.value = null
        _isGuest.value = true
    }

    fun clear() {
        _currentUser.value = null
        _isGuest.value = false
    }
}
