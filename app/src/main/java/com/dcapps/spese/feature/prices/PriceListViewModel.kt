package com.dcapps.spese.feature.prices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dcapps.spese.core.data.CatalogRepository
import com.dcapps.spese.core.prefs.SortCriterion
import com.dcapps.spese.core.prefs.SortPreferences
import com.dcapps.spese.core.prefs.comparator
import com.dcapps.spese.core.prefs.toSaved
import com.dcapps.spese.core.prefs.toSortCriteria
import com.dcapps.spese.core.prefs.toggled
import com.dcapps.spese.core.prefs.UiPreferences
import com.dcapps.spese.core.ui.SelectionController
import com.dcapps.spese.core.ui.launchMutation
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
    // Ordinamento multi-campo a cascata (ordine = priorità). Vuoto = default (per nome).
    val sortCriteria: List<SortCriterion<ProductSort>> = emptyList(),
    // Filtro per marca: insieme di marche selezionate; vuoto = tutte le marche.
    val brandFilter: Set<String> = emptySet(),
    val message: String? = null,
    // Mostra il badge "in attesa di sync" sulle righe (preferenza utente).
    val showPendingSync: Boolean = true,
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
            val cmp = sortCriteria.comparator<Product, ProductSort> { key ->
                when (key) {
                    ProductSort.NAME -> { p -> p.name.lowercase() }
                    // Prodotti senza marca in fondo (carattere alto come segnaposto).
                    ProductSort.BRAND -> { p -> p.brand?.lowercase() ?: "￿" }
                }
            } ?: compareBy { it.name.lowercase() } // default: per nome
            return filtered.sortedWith(cmp)
        }
}

@HiltViewModel
class PriceListViewModel @Inject constructor(
    private val priceRepository: PriceRepository,
    private val catalogRepository: CatalogRepository,
    private val sortPreferences: SortPreferences,
    private val uiPreferences: UiPreferences,
) : ViewModel() {

    // Ordinamento ricordato dall'ultima volta (issue #9), ora multi-campo a cascata.
    private val _uiState = MutableStateFlow(
        PriceListUiState(
            sortCriteria = sortPreferences.getList(SortPreferences.PRICES)
                .toSortCriteria { runCatching { ProductSort.valueOf(it) }.getOrNull() },
        ),
    )
    val uiState: StateFlow<PriceListUiState> = _uiState.asStateFlow()

    init {
        // Prodotti: Flow reattivo da Room (local-first) → la UI si aggiorna da sola a ogni modifica.
        viewModelScope.launch {
            priceRepository.observeProducts().collect { products ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        // Risolve il simbolo dell'unità di default dal catalogo (in Room è solo l'id).
                        products = products.map { p -> p.withDefaultUnitSymbol(it.units) },
                    )
                }
            }
        }
        // Unità di misura: catalogo online (non è un dato local-first).
        loadUnits()
        viewModelScope.launch {
            uiPreferences.showPendingSyncBadges.collect { show ->
                _uiState.update { it.copy(showPendingSync = show) }
            }
        }
    }

    /** load() resta per compat: in local-first ricarica solo il catalogo unità. */
    fun load(showLoading: Boolean = false) = loadUnits()

    private fun loadUnits() {
        viewModelScope.launch {
            val units = catalogRepository.getMeasurementUnits()
            if (units is ApiResult.Success) _uiState.update { s ->
                s.copy(
                    units = units.data,
                    // Le unità possono arrivare dopo i prodotti: ri-risolvi i simboli.
                    products = s.products.map { it.withDefaultUnitSymbol(units.data) },
                )
            }
        }
    }

    fun onQueryChange(q: String) = _uiState.update { it.copy(query = q) }

    /** Sceglie il criterio di ordinamento; ritoccare quello attivo inverte la direzione. */
    fun onSortToggle(sort: ProductSort) {
        _uiState.update { it.copy(sortCriteria = it.sortCriteria.toggled(sort)) }
        // Ricorda la cascata per la prossima apertura (issue #9).
        sortPreferences.setList(SortPreferences.PRICES, _uiState.value.sortCriteria.toSaved())
    }

    /** Imposta le marche selezionate (insieme vuoto = tutte). */
    fun setBrandFilter(brands: Set<String>) = _uiState.update { it.copy(brandFilter = brands) }

    fun createProduct(name: String, brand: String?, defaultUnitId: Long?) {
        if (name.isBlank()) return
        launchMutation(
            onError = { e -> _uiState.update { it.copy(message = e.message) } },
            onSuccess = {}, // il Flow reattivo aggiorna la lista da solo
            call = {
                priceRepository.createProduct(
                    CreateProductRequest(name = name.trim(), brand = brand?.takeIf { it.isNotBlank() }, defaultUnitId = defaultUnitId)
                )
            },
        )
    }

    fun deleteProduct(product: Product) {
        launchMutation(
            onError = { e -> _uiState.update { it.copy(message = e.message) } },
            onSuccess = {}, // il Flow reattivo aggiorna la lista da solo
            call = { priceRepository.deleteProduct(product.id) },
        )
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
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }
}
