package com.dcapps.spese.core.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.guestProfileDataStore by preferencesDataStore(name = "guest_profile")

/** Dati di profilo dell'ospite (modalità guest), persistiti localmente. */
data class GuestProfileData(
    val displayName: String,
    val localPhotoUri: String?,
)

/**
 * Profilo LOCALE dell'ospite (DataStore). In modalità guest non c'è un [User] di backend: nome e foto
 * vivono qui, finché l'ospite non fa l'upgrade a un account. Alla prima lettura genera e persiste un
 * nome `User_<uuid_corto>` stabile, così l'identità dell'ospite resta coerente tra i riavvii.
 */
@Singleton
class GuestProfile @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val keyName = stringPreferencesKey("guest_display_name")
    private val keyPhoto = stringPreferencesKey("guest_local_photo_uri")

    /** Nome + foto dell'ospite; genera (e persiste) un nome di default la prima volta. */
    suspend fun get(): GuestProfileData {
        val prefs = context.guestProfileDataStore.data.firstOrNull()
        val existingName = prefs?.get(keyName)
        val name = existingName ?: generateDefaultName().also { setName(it) }
        return GuestProfileData(displayName = name, localPhotoUri = prefs?.get(keyPhoto))
    }

    /** Flusso reattivo del profilo ospite (per la UI). Il nome di default non viene persistito qui:
     *  se assente lo si mostra comunque, ma la generazione stabile avviene in [get]. */
    fun observe(): Flow<GuestProfileData> =
        context.guestProfileDataStore.data.map { prefs ->
            GuestProfileData(
                displayName = prefs[keyName] ?: generateDefaultName(),
                localPhotoUri = prefs[keyPhoto],
            )
        }

    suspend fun setName(name: String) {
        context.guestProfileDataStore.edit { it[keyName] = name }
    }

    suspend fun setPhoto(uri: String?) {
        context.guestProfileDataStore.edit {
            if (uri == null) it.remove(keyPhoto) else it[keyPhoto] = uri
        }
    }

    /** Pulisce il profilo ospite (es. dopo l'upgrade ad account). */
    suspend fun clear() {
        context.guestProfileDataStore.edit { it.clear() }
    }

    private fun generateDefaultName(): String =
        "User_" + UUID.randomUUID().toString().take(8)
}
