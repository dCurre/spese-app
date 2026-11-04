package com.dcapps.spese.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Ruolo utente (superadmin/admin/user + eventuali custom). */
@Serializable
data class UserRole(
    val id: Long,
    val name: String,
    val protected: Boolean = false,
)

@Serializable
data class UserRolesResponse(@SerialName("user_roles") val userRoles: List<UserRole> = emptyList())

/** Tipologia di spesa. */
@Serializable
data class ExpenseType(
    val id: Long,
    val name: String,
    val protected: Boolean = false,
)

@Serializable
data class ExpenseTypesResponse(
    @SerialName("expense_types") val expenseTypes: List<ExpenseType> = emptyList(),
)

/** Unità di misura (categorie: peso/volume/numero/lunghezza/energia). */
@Serializable
data class MeasurementUnit(
    val id: Long,
    val name: String,
    val symbol: String,
    val category: String,
    val protected: Boolean = false,
)

@Serializable
data class MeasurementUnitsResponse(
    @SerialName("measurement_units") val measurementUnits: List<MeasurementUnit> = emptyList(),
)

/** Body creazione per catalog item con solo nome. */
@Serializable
data class NameRequest(val name: String)
