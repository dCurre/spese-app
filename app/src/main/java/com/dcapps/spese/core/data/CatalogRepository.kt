package com.dcapps.spese.core.data

import com.dcapps.spese.core.model.ExpenseType
import com.dcapps.spese.core.model.MeasurementUnit
import com.dcapps.spese.core.model.MeasurementUnitRequest
import com.dcapps.spese.core.model.NameRequest
import com.dcapps.spese.core.model.UserRole
import com.dcapps.spese.core.network.ApiResult
import com.dcapps.spese.core.network.SpeseApi
import com.dcapps.spese.core.network.apiCall
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dati di catalogo (tipi spesa, unità di misura, ruoli). Con cache in memoria: questi dati
 * cambiano raramente e il backend non pagina.
 */
@Singleton
class CatalogRepository @Inject constructor(
    private val api: SpeseApi,
    private val json: Json,
) {
    private var expenseTypesCache: List<ExpenseType>? = null
    private var unitsCache: List<MeasurementUnit>? = null
    private var rolesCache: List<UserRole>? = null

    suspend fun getExpenseTypes(forceRefresh: Boolean = false): ApiResult<List<ExpenseType>> {
        expenseTypesCache?.let { if (!forceRefresh) return ApiResult.Success(it) }
        return apiCall(json) { api.getExpenseTypes().expenseTypes }
            .also { if (it is ApiResult.Success) expenseTypesCache = it.data }
    }

    suspend fun getMeasurementUnits(forceRefresh: Boolean = false): ApiResult<List<MeasurementUnit>> {
        unitsCache?.let { if (!forceRefresh) return ApiResult.Success(it) }
        return apiCall(json) { api.getMeasurementUnits().measurementUnits }
            .also { if (it is ApiResult.Success) unitsCache = it.data }
    }

    suspend fun getUserRoles(forceRefresh: Boolean = false): ApiResult<List<UserRole>> {
        rolesCache?.let { if (!forceRefresh) return ApiResult.Success(it) }
        return apiCall(json) { api.getUserRoles().userRoles }
            .also { if (it is ApiResult.Success) rolesCache = it.data }
    }

    // --- CRUD (admin). Invalidano la cache corrispondente. ---

    suspend fun createExpenseType(name: String): ApiResult<Unit> =
        apiCall(json) { api.createExpenseType(NameRequest(name)); Unit }.also { expenseTypesCache = null }

    suspend fun updateExpenseType(id: Long, name: String): ApiResult<Unit> =
        apiCall(json) { api.updateExpenseType(id, NameRequest(name)); Unit }.also { expenseTypesCache = null }

    suspend fun deleteExpenseType(id: Long): ApiResult<Unit> =
        apiCall(json) { api.deleteExpenseType(id); Unit }.also { expenseTypesCache = null }

    suspend fun createUnit(name: String, symbol: String, category: String): ApiResult<Unit> =
        apiCall(json) { api.createMeasurementUnit(MeasurementUnitRequest(name, symbol, category)); Unit }
            .also { unitsCache = null }

    suspend fun updateUnit(id: Long, name: String, symbol: String, category: String): ApiResult<Unit> =
        apiCall(json) { api.updateMeasurementUnit(id, MeasurementUnitRequest(name, symbol, category)); Unit }
            .also { unitsCache = null }

    suspend fun deleteUnit(id: Long): ApiResult<Unit> =
        apiCall(json) { api.deleteMeasurementUnit(id); Unit }.also { unitsCache = null }

    suspend fun createRole(name: String): ApiResult<Unit> =
        apiCall(json) { api.createUserRole(NameRequest(name)); Unit }.also { rolesCache = null }

    suspend fun updateRole(id: Long, name: String): ApiResult<Unit> =
        apiCall(json) { api.updateUserRole(id, NameRequest(name)); Unit }.also { rolesCache = null }

    suspend fun deleteRole(id: Long): ApiResult<Unit> =
        apiCall(json) { api.deleteUserRole(id); Unit }.also { rolesCache = null }
}
