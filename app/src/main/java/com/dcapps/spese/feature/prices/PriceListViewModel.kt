package com.dcapps.spese.feature.prices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dcapps.spese.core.data.CatalogRepository
import com.dcapps.spese.core.prefs.SortPreferences
import com.dcapps.spese.core.ui.SelectionController
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
    // Filtro per marca: insieme di marche selezionate; vuoto = tutte le marche.
    val brandFilter: Set<String> = emptySet(),
    val message: String? = null,
) {
    /** Marche presenti tra i prodotti (per il selettore filtro), ordinate alfabeticamente. */
    val availableBrands: List<String>
        get() = products.mapNotNull { it.brand?.takeIf { b -> b.isNotBlank() } }
            .distinct()
            .sortedBy { it.lowercase() }

    val visibleProducts: List<Product>
        get() {
            val q = query.trim()
            val filtered = products.filter { p ->
                // La ricerca copre nome E marca.
                val matchesQuery = q.isBlank() ||
                    p.name.contains(q, ignoreCase = true) ||
                    (p.brand?.contains(q, ignoreCase = true) == true)
                // Filtro marca: nessuna marca selezionata = tutte.
                val matchesBrand = brandFilter.isEmpty() || p.brand in brandFilter
                matchesQuery && matchesBrand
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
    private val sortPreferences: SortPreferences,
) : ViewModel() {

    // Ordinamento ricordato dall'ultima volta (issue #9).
    private val savedSort = sortPreferences.get(SortPreferences.PRICES)
    private val _uiState = MutableStateFlow(
        PriceListUiState(
            sort = savedSort?.criterion?.let { runCatching { ProductSort.valueOf(it) }.getOrNull() }
                ?: ProductSort.NAME,
            sortAsc = savedSort?.ascending ?: true,
        ),
    )
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
    fun onSortChange(sort: ProductSort) {
        _uiState.update {
            if (it.sort == sort) it.copy(sortAsc = !it.sortAsc) else it.copy(sort = sort, sortAsc = true)
        }
        // Ricorda la scelta per la prossima apertura (issue #9).
        _uiState.value.let { sortPreferences.set(SortPreferences.PRICES, it.sort.name, it.sortAsc) }
    }

    /** Imposta le marche selezionate (insieme vuoto = tutte). */
    fun setBrandFilter(brands: Set<String>) = _uiState.update { it.copy(brandFilter = brands) }

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

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            when (val result = priceRepository.deleteProduct(product.id)) {
                is ApiResult.Success -> load(showLoading = false)
                is ApiResult.Error -> _uiState.update { it.copy(message = result.message) }
            }
        }
    }

    // --- Selezione multipla (issue #10) ---
    // Stato di selezione gestito dal controller condiviso (niente duplicazione tra ViewModel).
    val selection = SelectionController()

    /** Elimina tutti i prodotti selezionati in UNA chiamata bulk, poi esce dalla selezione. */
    fun deleteSelected() {
        val ids = selection.ids.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val r = priceRepository.deleteProductsBulk(ids)
            selection.clear()
            if (r is ApiResult.Error) _uiState.update { it.copy(message = r.message) }
            load(showLoading = false)
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }
}
