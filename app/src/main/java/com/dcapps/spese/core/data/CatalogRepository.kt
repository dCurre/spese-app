package com.dcapps.spese.core.data

import com.dcapps.spese.core.model.ExpenseType
import com.dcapps.spese.core.model.MeasurementUnit
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
}
