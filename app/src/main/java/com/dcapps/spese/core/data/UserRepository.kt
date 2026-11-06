package com.dcapps.spese.core.data

import com.dcapps.spese.core.model.ProfileImageResponse
import com.dcapps.spese.core.model.SelectImageRequest
import com.dcapps.spese.core.model.UpdateUserRequest
import com.dcapps.spese.core.model.UpsertUserRequest
import com.dcapps.spese.core.model.User
import com.dcapps.spese.core.model.UserWithExpensesListsResponse
import com.dcapps.spese.core.network.ApiResult
import com.dcapps.spese.core.network.SpeseApi
import com.dcapps.spese.core.network.apiCall
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Accesso ai dati utente. */
@Singleton
class UserRepository @Inject constructor(
    private val api: SpeseApi,
    private val json: Json,
) {
    /** Login lato backend: crea o aggiorna l'utente per email e lo restituisce. */
    suspend fun upsertByEmail(body: UpsertUserRequest): ApiResult<User> =
        apiCall(json) { api.upsertUserByEmail(body) }

    suspend fun getById(id: Long): ApiResult<User> =
        apiCall(json) { api.getUser(id) }

    suspend fun getByEmail(email: String): ApiResult<User> =
        apiCall(json) { api.getUserByEmail(email) }

    suspend fun update(id: Long, body: UpdateUserRequest): ApiResult<Unit> =
        apiCall(json) { api.updateUser(id, body); Unit }

    /** Tutti gli utenti (per la gestione admin). */
    suspend fun getAll(): ApiResult<List<User>> =
        apiCall(json) { api.getUsers().users }

    /**
     * Cambia il ruolo di [userId]. Richiede [callerEmail] (l'admin che esegue l'azione):
     * il backend verifica i permessi in base a quella email.
     */
    suspend fun changeRole(userId: Long, roleId: Long, callerEmail: String): ApiResult<Unit> =
        apiCall(json) {
            api.updateUser(userId, UpdateUserRequest(roleId = roleId, callerEmail = callerEmail)); Unit
        }

    /** Utente + sue liste spese (usato per le statistiche del profilo). */
    suspend fun getWithLists(email: String): ApiResult<UserWithExpensesListsResponse> =
        apiCall(json) { api.getUserExpensesLists(email) }

    /** Carica una nuova immagine profilo. [bytes] = contenuto del file scelto. */
    suspend fun uploadProfileImage(
        userId: Long,
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
    ): ApiResult<ProfileImageResponse> = apiCall(json) {
        val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", fileName, body)
        api.uploadProfileImage(userId, part)
    }

    /** Seleziona come immagine profilo una già presente nello storico. */
    suspend fun selectProfileImage(userId: Long, url: String): ApiResult<ProfileImageResponse> =
        apiCall(json) {
            api.selectProfileImage(userId, SelectImageRequest(url))
        }
}
