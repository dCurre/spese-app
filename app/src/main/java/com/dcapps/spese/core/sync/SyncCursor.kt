package com.dcapps.spese.core.sync

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

private val Context.syncDataStore by preferencesDataStore(name = "sync_cursor")

/**
 * Memorizza il cursore `since` del delta sync (il `server_time` dell'ultimo pull riuscito), per
 * scaricare dal backend solo ciò che è cambiato da allora. Un cursore per entità/sezione (qui
 * "checklist"); in futuro altre sezioni useranno chiavi diverse.
 */
@Singleton
class SyncCursor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private fun key(name: String) = stringPreferencesKey("since_$name")

    suspend fun get(name: String): String? =
        context.syncDataStore.data.firstOrNull()?.get(key(name))

    suspend fun set(name: String, serverTime: String) {
        context.syncDataStore.edit { it[key(name)] = serverTime }
    }

    suspend fun clear(name: String) {
        context.syncDataStore.edit { it.remove(key(name)) }
    }

    companion object {
        const val CHECKLIST = "checklist"
        const val EXPENSES = "expenses"
        const val PRICES = "prices"
    }
}
