package com.dcapps.spese.core.data

import com.dcapps.spese.core.model.UpdateUserRequest
import com.dcapps.spese.core.model.UpsertUserRequest
import com.dcapps.spese.core.model.User
import com.dcapps.spese.core.network.ApiResult
import com.dcapps.spese.core.network.SpeseApi
import com.dcapps.spese.core.network.apiCall
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
}
