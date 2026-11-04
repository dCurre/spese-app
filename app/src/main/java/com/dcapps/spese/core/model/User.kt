package com.dcapps.spese.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Utente. Il backend identifica l'utente per email (nessun token): dopo il login Google si
 * chiama upsert-by-email e si usa [id] in tutte le chiamate successive.
 */
@Serializable
data class User(
    val id: Long,
    val name: String? = null,
    val surname: String? = null,
    val email: String = "",
    @SerialName("profile_image") val profileImage: String? = null,
    @SerialName("profile_images_history") val profileImagesHistory: List<String> = emptyList(),
    @SerialName("paid_list_shown") val paidListShown: Boolean = true,
    @SerialName("theme_preference") val themePreference: String = "auto",
    val role: String? = null,
    @SerialName("is_guest") val isGuest: Boolean = false,
) {
    val displayName: String
        get() = listOfNotNull(name?.takeIf { it.isNotBlank() }, surname?.takeIf { it.isNotBlank() })
            .joinToString(" ")
            .ifBlank { email }
}

/** Body di POST /api/users (usato per creare un ospite: {name, is_guest:true}). */
@Serializable
data class CreateUserRequest(
    val name: String,
    val surname: String? = null,
    val email: String? = null,
    @SerialName("is_guest") val isGuest: Boolean = false,
)

/** Body di POST /api/users/upsert-by-email */
@Serializable
data class UpsertUserRequest(
    val email: String,
    val name: String = "",
    val surname: String = "",
    @SerialName("profile_image") val profileImage: String = "",
)

/** Body parziale per PUT /api/users/{id} */
@Serializable
data class UpdateUserRequest(
    val name: String? = null,
    val surname: String? = null,
    val email: String? = null,
    @SerialName("profile_image") val profileImage: String? = null,
    @SerialName("paid_list_shown") val paidListShown: Boolean? = null,
    @SerialName("theme_preference") val themePreference: String? = null,
    @SerialName("role_id") val roleId: Long? = null,
    @SerialName("caller_email") val callerEmail: String? = null,
)

@Serializable
data class UsersResponse(val users: List<User> = emptyList())

/** Risposta di creazione generica: { "id": .., "message": ".." } */
@Serializable
data class CreatedResponse(val id: Long? = null, val message: String? = null)

/** Risposta con solo messaggio. */
@Serializable
data class MessageResponse(val message: String? = null)

/** Risposta upload/select immagine profilo. */
@Serializable
data class ProfileImageResponse(
    val url: String,
    val history: List<String> = emptyList(),
)

/** Body per selezionare un'immagine profilo dallo storico ({"url": "..."}). */
@Serializable
data class SelectImageRequest(val url: String)
