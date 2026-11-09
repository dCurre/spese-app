package com.dcapps.spese.core.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dcapps.spese.core.model.User
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userDataStore by preferencesDataStore(name = "user_cache")

/**
 * Cache locale dell'ultimo [User] loggato (DataStore). Serve al login offline: se all'avvio
 * Firebase ha la sessione ma il backend è irraggiungibile per rete, si entra con questo utente
 * cachato invece di rimandare al login. Scrittura/lettura asincrone (coroutine), senza bloccare.
 */
@Singleton
class UserCache @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) {
    private val keyUser = stringPreferencesKey("last_user_json")

    /** Salva l'ultimo utente loggato (chiamato quando la sessione viene aggiornata dal backend). */
    suspend fun save(user: User) {
        context.userDataStore.edit { it[keyUser] = json.encodeToString(User.serializer(), user) }
    }

    /** Ultimo utente cachato, o null se assente/non deserializzabile. */
    suspend fun load(): User? {
        val raw = context.userDataStore.data.firstOrNull()?.get(keyUser) ?: return null
        return runCatching { json.decodeFromString(User.serializer(), raw) }.getOrNull()
    }

    /** Pulisce la cache (al logout). */
    suspend fun clear() {
        context.userDataStore.edit { it.remove(keyUser) }
    }
}
