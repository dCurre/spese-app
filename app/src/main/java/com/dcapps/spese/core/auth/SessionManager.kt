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

    val isLoggedIn: Boolean get() = _currentUser.value != null

    fun setUser(user: User?) {
        _currentUser.value = user
    }

    fun clear() {
        _currentUser.value = null
    }
}
