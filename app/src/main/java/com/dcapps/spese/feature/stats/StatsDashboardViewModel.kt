package com.dcapps.spese.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dcapps.spese.core.data.ExpensesRepository
import com.dcapps.spese.core.model.ExpenseStats
import com.dcapps.spese.core.network.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/** Periodo selezionabile nella dashboard; determina from/to e la granularità dei bucket. */
enum class StatsPeriod { THIS_MONTH, LAST_6_MONTHS, THIS_YEAR }

data class StatsDashboardUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val period: StatsPeriod = StatsPeriod.THIS_MONTH,
    val stats: ExpenseStats? = null,
)

/**
 * Dashboard statistiche SPESE personali. Online-only: l'aggregazione è calcolata dal backend
 * (`GET /api/expenses/stats`) sulle sole spese pagate dall'utente. Cambiare [StatsPeriod] rilancia
 * la query con from/to/granularity coerenti.
 */
@HiltViewModel
class StatsDashboardViewModel @Inject constructor(
    private val expensesRepository: ExpensesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsDashboardUiState())
    val uiState: StateFlow<StatsDashboardUiState> = _uiState.asStateFlow()

    init { load(StatsPeriod.THIS_MONTH) }

    fun selectPeriod(period: StatsPeriod) {
        if (period == _uiState.value.period && _uiState.value.stats != null) return
        load(period)
    }

    fun retry() = load(_uiState.value.period)

    private fun load(period: StatsPeriod) {
        _uiState.update { it.copy(isLoading = true, error = null, period = period) }
        val (from, to, granularity) = windowOf(period)
        viewModelScope.launch {
            when (val result = expensesRepository.getExpenseStats(from, to, granularity)) {
                is ApiResult.Success ->
                    _uiState.update { it.copy(isLoading = false, error = null, stats = result.data) }
                is ApiResult.Error ->
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }

    /** Calcola (from, to, granularity) per il periodo scelto. Date in yyyy-MM-dd. */
    private fun windowOf(period: StatsPeriod): Triple<String, String, String> {
        val today = LocalDate.now()
        return when (period) {
            StatsPeriod.THIS_MONTH -> Triple(
                today.withDayOfMonth(1).format(FMT),
                today.format(FMT),
                "week",
            )
            StatsPeriod.LAST_6_MONTHS -> Triple(
                today.minusMonths(5).withDayOfMonth(1).format(FMT),
                today.format(FMT),
                "month",
            )
            StatsPeriod.THIS_YEAR -> Triple(
                today.withDayOfYear(1).format(FMT),
                today.format(FMT),
                "month",
            )
        }
    }

    private companion object {
        private val FMT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }
}
