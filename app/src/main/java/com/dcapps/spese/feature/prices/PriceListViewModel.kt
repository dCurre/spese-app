package com.dcapps.spese.feature.prices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dcapps.spese.core.data.CatalogRepository
import com.dcapps.spese.core.data.PriceRepository
import com.dcapps.spese.core.model.CreateProductRequest
import com.dcapps.spese.core.model.MeasurementUnit
import com.dcapps.spese.core.model.Product
import com.dcapps.spese.core.network.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PriceListUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val products: List<Product> = emptyList(),
    val units: List<MeasurementUnit> = emptyList(),
    val query: String = "",
    val message: String? = null,
) {
    val visibleProducts: List<Product>
        get() = products.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
}

@HiltViewModel
class PriceListViewModel @Inject constructor(
    private val priceRepository: PriceRepository,
    private val catalogRepository: CatalogRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PriceListUiState())
    val uiState: StateFlow<PriceListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val products = priceRepository.getProducts()
            val units = catalogRepository.getMeasurementUnits()
            if (products is ApiResult.Error) {
                _uiState.update { it.copy(isLoading = false, error = products.message) }
                return@launch
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    products = (products as ApiResult.Success).data,
                    units = (units as? ApiResult.Success)?.data ?: emptyList(),
                )
            }
        }
    }

    fun onQueryChange(q: String) = _uiState.update { it.copy(query = q) }

    fun createProduct(name: String, brand: String?, defaultUnitId: Long?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val result = priceRepository.createProduct(
                CreateProductRequest(name = name.trim(), brand = brand?.takeIf { it.isNotBlank() }, defaultUnitId = defaultUnitId)
            )
            when (result) {
                is ApiResult.Success -> load()
                is ApiResult.Error -> _uiState.update { it.copy(message = result.message) }
            }
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }
}
