package com.dcapps.spese.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dcapps.spese.core.auth.SessionManager
import com.dcapps.spese.core.data.CatalogRepository
import com.dcapps.spese.core.data.UserRepository
import com.dcapps.spese.core.model.ExpenseType
import com.dcapps.spese.core.model.MeasurementUnit
import com.dcapps.spese.core.model.User
import com.dcapps.spese.core.model.UserRole
import com.dcapps.spese.core.network.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUiState(
    val isAdmin: Boolean = false,
    val isLoading: Boolean = true,
    val expenseTypes: List<ExpenseType> = emptyList(),
    val units: List<MeasurementUnit> = emptyList(),
    val roles: List<UserRole> = emptyList(),
    val users: List<User> = emptyList(),
    val message: String? = null,
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val catalogRepository: CatalogRepository,
    private val userRepository: UserRepository,
    private val session: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        val role = session.currentUser.value?.role
        val isAdmin = role == "admin" || role == "superadmin"
        _uiState.update { it.copy(isAdmin = isAdmin) }
        if (isAdmin) load()
        else _uiState.update { it.copy(isLoading = false) }
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val types = catalogRepository.getExpenseTypes(forceRefresh = true)
            val units = catalogRepository.getMeasurementUnits(forceRefresh = true)
            val roles = catalogRepository.getUserRoles(forceRefresh = true)
            val users = userRepository.getAll()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    expenseTypes = (types as? ApiResult.Success)?.data ?: emptyList(),
                    units = (units as? ApiResult.Success)?.data ?: emptyList(),
                    roles = (roles as? ApiResult.Success)?.data ?: emptyList(),
                    users = (users as? ApiResult.Success)?.data?.filter { u -> !u.isGuest } ?: emptyList(),
                )
            }
        }
    }

    /** Cambia il ruolo di un utente (usa l'email dell'admin corrente come caller). */
    fun changeUserRole(userId: Long, roleId: Long) {
        val caller = session.currentUser.value?.email ?: return
        mutate { userRepository.changeRole(userId, roleId, caller) }
    }

    // --- Tipi spesa ---
    fun addExpenseType(name: String) = mutate { catalogRepository.createExpenseType(name) }
    fun updateExpenseType(id: Long, name: String) = mutate { catalogRepository.updateExpenseType(id, name) }
    fun deleteExpenseType(id: Long) = mutate { catalogRepository.deleteExpenseType(id) }

    // --- Unità ---
    fun addUnit(name: String, symbol: String, category: String) =
        mutate { catalogRepository.createUnit(name, symbol, category) }
    fun updateUnit(id: Long, name: String, symbol: String, category: String) =
        mutate { catalogRepository.updateUnit(id, name, symbol, category) }
    fun deleteUnit(id: Long) = mutate { catalogRepository.deleteUnit(id) }

    // --- Ruoli ---
    fun addRole(name: String) = mutate { catalogRepository.createRole(name) }
    fun updateRole(id: Long, name: String) = mutate { catalogRepository.updateRole(id, name) }
    fun deleteRole(id: Long) = mutate { catalogRepository.deleteRole(id) }

    fun clearMessage() = _uiState.update { it.copy(message = null) }

    /** Esegue una mutazione e ricarica; mostra l'eventuale errore. */
    private fun mutate(suspendCall: suspend () -> ApiResult<Unit>) {
        viewModelScope.launch {
            when (val r = suspendCall()) {
                is ApiResult.Success -> load()
                is ApiResult.Error -> _uiState.update { it.copy(message = r.message) }
            }
        }
    }
}
