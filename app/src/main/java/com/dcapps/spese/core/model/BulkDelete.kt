package com.dcapps.spese.core.model

import kotlinx.serialization.Serializable

/** Body comune per gli endpoint di cancellazione massiva: `{ "ids": [...] }`. */
@Serializable
data class BulkDeleteRequest(val ids: List<Long>)

/** Risposta comune dei bulk-delete: `{ "deleted": n, "message": "..." }`. */
@Serializable
data class BulkDeleteResponse(val deleted: Int = 0, val message: String? = null)
