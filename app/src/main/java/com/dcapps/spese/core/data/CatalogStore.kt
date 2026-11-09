package com.dcapps.spese.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dcapps.spese.core.model.ExpenseType
import com.dcapps.spese.core.model.MeasurementUnit
import com.dcapps.spese.core.model.UserRole
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.catalogDataStore by preferencesDataStore(name = "catalog_cache")

/**
 * Cache su disco (DataStore) dei cataloghi (tipi spesa, unità di misura, ruoli). Serve all'uso
 * OFFLINE: al primo caricamento con rete le liste vengono salvate qui, così restano disponibili
 * anche dopo un riavvio dell'app senza connessione. Scrittura/lettura asincrone (coroutine).
 */
@Singleton
class CatalogStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) {
    private val keyExpenseTypes = stringPreferencesKey("expense_types_json")
    private val keyUnits = stringPreferencesKey("units_json")
    private val keyRoles = stringPreferencesKey("roles_json")

    private val expenseTypesSerializer = ListSerializer(ExpenseType.serializer())
    private val unitsSerializer = ListSerializer(MeasurementUnit.serializer())
    private val rolesSerializer = ListSerializer(UserRole.serializer())

    suspend fun saveExpenseTypes(list: List<ExpenseType>) {
        context.catalogDataStore.edit { it[keyExpenseTypes] = json.encodeToString(expenseTypesSerializer, list) }
    }

    /** Tipi spesa cachati, o null se mai salvati / non deserializzabili. */
    suspend fun loadExpenseTypes(): List<ExpenseType>? {
        val raw = context.catalogDataStore.data.firstOrNull()?.get(keyExpenseTypes) ?: return null
        return runCatching { json.decodeFromString(expenseTypesSerializer, raw) }.getOrNull()
    }

    suspend fun saveUnits(list: List<MeasurementUnit>) {
        context.catalogDataStore.edit { it[keyUnits] = json.encodeToString(unitsSerializer, list) }
    }

    /** Unità di misura cachate, o null se mai salvate / non deserializzabili. */
    suspend fun loadUnits(): List<MeasurementUnit>? {
        val raw = context.catalogDataStore.data.firstOrNull()?.get(keyUnits) ?: return null
        return runCatching { json.decodeFromString(unitsSerializer, raw) }.getOrNull()
    }

    suspend fun saveRoles(list: List<UserRole>) {
        context.catalogDataStore.edit { it[keyRoles] = json.encodeToString(rolesSerializer, list) }
    }

    /** Ruoli cachati, o null se mai salvati / non deserializzabili. */
    suspend fun loadRoles(): List<UserRole>? {
        val raw = context.catalogDataStore.data.firstOrNull()?.get(keyRoles) ?: return null
        return runCatching { json.decodeFromString(rolesSerializer, raw) }.getOrNull()
    }
}
