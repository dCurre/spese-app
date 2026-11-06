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

/** Criterio di ordinamento dell'elenco prodotti. */
enum class ProductSort { NAME, BRAND }

data class PriceListUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val products: List<Product> = emptyList(),
    val units: List<MeasurementUnit> = emptyList(),
    val query: String = "",
    val sort: ProductSort = ProductSort.NAME,
    val sortAsc: Boolean = true,
    // Filtro per marca: null = tutte le marche.
    val brandFilter: String? = null,
    val message: String? = null,
) {
    /** Marche presenti tra i prodotti (per il selettore filtro), ordinate alfabeticamente. */
    val availableBrands: List<String>
        get() = products.mapNotNull { it.brand?.takeIf { b -> b.isNotBlank() } }
            .distinct()
            .sortedBy { it.lowercase() }

    val visibleProducts: List<Product>
        get() {
            val filtered = products.filter {
                (query.isBlank() || it.name.contains(query, ignoreCase = true)) &&
                    (brandFilter == null || it.brand.equals(brandFilter, ignoreCase = true))
            }
            val base: Comparator<Product> = when (sort) {
                ProductSort.NAME -> compareBy { it.name.lowercase() }
                // Prodotti senza marca in fondo; a parità di marca, per nome.
                ProductSort.BRAND -> compareBy<Product> { it.brand?.lowercase() ?: "￿" }
                    .thenBy { it.name.lowercase() }
            }
            return filtered.sortedWith(if (sortAsc) base else base.reversed())
        }
}

@HiltViewModel
class PriceListViewModel @Inject constructor(
    private val priceRepository: PriceRepository,
    private val catalogRepository: CatalogRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PriceListUiState())
    val uiState: StateFlow<PriceListUiState> = _uiState.asStateFlow()

    init {
        load(showLoading = true)
    }

    /**
     * Carica i prodotti. [showLoading] = true mostra lo spinner (solo primo caricamento); reload
     * dopo una mutazione usa false (reload silenzioso, niente lampeggio dell'elenco).
     */
    fun load(showLoading: Boolean = false) {
        if (showLoading) _uiState.update { it.copy(isLoading = true, error = null) }
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

    /** Sceglie il criterio di ordinamento; ritoccare quello attivo inverte la direzione. */
    fun onSortChange(sort: ProductSort) = _uiState.update {
        if (it.sort == sort) it.copy(sortAsc = !it.sortAsc) else it.copy(sort = sort, sortAsc = true)
    }

    /** Imposta il filtro per marca (null = tutte). */
    fun onBrandFilterChange(brand: String?) = _uiState.update { it.copy(brandFilter = brand) }

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
