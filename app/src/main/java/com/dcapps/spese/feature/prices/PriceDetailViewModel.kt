package com.dcapps.spese.feature.prices

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dcapps.spese.core.auth.SessionManager
import com.dcapps.spese.core.data.CatalogRepository
import com.dcapps.spese.core.data.PriceRepository
import com.dcapps.spese.core.local.GUEST_OWNER_ID
import com.dcapps.spese.core.model.CreatePurchaseRequest
import com.dcapps.spese.core.model.UpdatePurchaseRequest
import com.dcapps.spese.core.model.MeasurementUnit
import com.dcapps.spese.core.model.PriceHistoryPoint
import com.dcapps.spese.core.model.Product
import com.dcapps.spese.core.model.Purchase
import com.dcapps.spese.core.model.UpdateProductRequest
import com.dcapps.spese.core.network.ApiResult
import com.dcapps.spese.core.prefs.UiPreferences
import com.dcapps.spese.core.ui.SelectionController
import com.dcapps.spese.core.ui.launchMutation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class Granularity(val wire: String, val label: String) {
    DAY("day", "Giorno"), MONTH("month", "Mese"), YEAR("year", "Anno")
}

data class PriceDetailUiState(
    val isLoading: Boolean = true,
    // Nome mostrato nel titolo durante il caricamento (arriva dall'elenco via navigazione).
    val placeholderName: String? = null,
    // Loading del solo grafico (cambio granularità): non fa sparire l'intera schermata.
    val historyLoading: Boolean = false,
    val error: String? = null,
    val product: Product? = null,
    val purchases: List<Purchase> = emptyList(),
    val history: List<PriceHistoryPoint> = emptyList(),
    val units: List<MeasurementUnit> = emptyList(),
    val granularity: Granularity = Granularity.MONTH,
    // Granularità con dati sufficienti per un andamento (≥ 2 periodi distinti, stessa soglia del
    // grafico). Solo queste mostrano il chip; se è vuoto l'intero pannello andamento resta nascosto.
    val availableGranularities: Set<Granularity> = emptySet(),
    val message: String? = null,
    val deleted: Boolean = false,
    // Mostra il badge "in attesa di sync" sulle righe (preferenza utente).
    val showPendingSync: Boolean = true,
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class PriceDetailViewModel @Inject constructor(
    private val priceRepository: PriceRepository,
    private val catalogRepository: CatalogRepository,
    private val uiPreferences: UiPreferences,
    private val session: SessionManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val productId: Long = checkNotNull(savedStateHandle["productId"])

    // Nome passato dall'elenco: usato come titolo durante il caricamento, prima che arrivi il
    // prodotto completo dal backend.
    private val placeholderName: String? =
        (savedStateHandle.get<String>("name"))?.takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow(PriceDetailUiState(placeholderName = placeholderName))
    val uiState: StateFlow<PriceDetailUiState> = _uiState.asStateFlow()

    // Granularità corrente: pilota il Flow dello storico (cambio = ri-osserva lo storico).
    private val granularityFlow = MutableStateFlow(Granularity.MONTH)

    init {
        // Prodotto + acquisti: Flow reattivi da Room (local-first).
        viewModelScope.launch {
            priceRepository.observeProduct(productId)
                .combine(priceRepository.observePurchases(productId)) { product, purchases ->
                    product to purchases
                }
                .collect { (product, purchases) ->
                    val available = availableGranularities(purchases)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            product = product?.withDefaultUnitSymbol(it.units),
                            purchases = purchases.map { p -> p.withUnitSymbol(it.units) },
                            availableGranularities = available,
                        )
                    }
                    // Se la granularità corrente non ha dati sufficienti ma un'altra sì, passa a quella
                    // (così il grafico, quando il pannello è visibile, mostra un andamento reale).
                    val current = _uiState.value.granularity
                    if (available.isNotEmpty() && current !in available) {
                        // Ordine deterministico (un Set non lo garantisce): preferisci MONTH, poi gli altri.
                        val next = listOf(Granularity.MONTH, Granularity.YEAR, Granularity.DAY)
                            .first { it in available }
                        setGranularity(next)
                    }
                }
        }
        // Storico: ri-osservato a ogni cambio di granularità.
        viewModelScope.launch {
            granularityFlow
                .flatMapLatest { g -> priceRepository.observeHistory(productId, g.wire) }
                .collect { history ->
                    _uiState.update { it.copy(history = history, historyLoading = false) }
                }
        }
        // Unità di misura: catalogo online (non local-first).
        loadUnits()
        viewModelScope.launch {
            uiPreferences.showPendingSyncBadges.collect { show ->
                _uiState.update { it.copy(showPendingSync = show) }
            }
        }
    }

    /** load() resta per compat: in local-first ricarica solo il catalogo unità. */
    fun load(showLoading: Boolean = true) = loadUnits()

    private fun loadUnits() {
        viewModelScope.launch {
            val units = catalogRepository.getMeasurementUnits()
            if (units is ApiResult.Success) {
                _uiState.update { s ->
                    s.copy(
                        units = units.data,
                        // Risolve il simbolo dell'unità di default del prodotto dal catalogo.
                        product = s.product?.withDefaultUnitSymbol(units.data),
                        // Rienumera i simboli unità sugli acquisti già caricati.
                        purchases = s.purchases.map { it.withUnitSymbol(units.data) },
                    )
                }
            }
        }
    }

    /**
     * Cambio granularità del grafico: ri-osserva SOLO lo storico (il Flow dedicato), con un loading
     * locale → la pagina non sparisce.
     */
    fun setGranularity(g: Granularity) {
        if (g == _uiState.value.granularity) return
        _uiState.update { it.copy(granularity = g, historyLoading = true) }
        granularityFlow.value = g
    }

    fun addPurchase(
        price: Double,
        quantity: Double,
        unitId: Long,
        purchasedAt: String,
        store: String?,
        notes: String?,
    ) {
        // In local-first l'acquisto è locale: in guest non c'è utente reale → owner fittizio.
        val userId = session.currentUser.value?.id ?: GUEST_OWNER_ID
        launchMutation(
            onError = { e -> _uiState.update { it.copy(message = e.message) } },
            onSuccess = {}, // il Flow reattivo aggiorna gli acquisti/storico
            call = {
                priceRepository.createPurchase(
                    CreatePurchaseRequest(
                        productId = productId,
                        userId = userId,
                        purchasedAt = purchasedAt,
                        price = price,
                        quantity = quantity,
                        unitId = unitId,
                        store = store?.takeIf { it.isNotBlank() },
                        notes = notes?.takeIf { it.isNotBlank() },
                    )
                )
            },
        )
    }

    fun updatePurchase(
        id: Long,
        price: Double,
        quantity: Double,
        unitId: Long,
        purchasedAt: String,
        store: String?,
        notes: String?,
    ) {
        launchMutation(
            onError = { e -> _uiState.update { it.copy(message = e.message) } },
            onSuccess = {},
            call = {
                priceRepository.updatePurchase(
                    id,
                    UpdatePurchaseRequest(
                        purchasedAt = purchasedAt,
                        price = price,
                        quantity = quantity,
                        unitId = unitId,
                        store = store?.takeIf { it.isNotBlank() },
                        notes = notes?.takeIf { it.isNotBlank() },
                    ),
                )
            },
        )
    }

    /** Modifica le proprietà del prodotto (nome, marca, unità predefinita). */
    fun updateProduct(name: String, brand: String?, defaultUnitId: Long?) {
        if (name.isBlank()) return
        launchMutation(
            onError = { e -> _uiState.update { it.copy(message = e.message) } },
            onSuccess = {},
            call = {
                priceRepository.updateProduct(
                    productId,
                    UpdateProductRequest(
                        name = name.trim(),
                        brand = brand?.takeIf { it.isNotBlank() },
                        defaultUnitId = defaultUnitId,
                    ),
                )
            },
        )
    }

    /** Elimina il prodotto e tutti i suoi acquisti. */
    fun deleteProduct() {
        launchMutation(
            onError = { e -> _uiState.update { it.copy(message = e.message) } },
            onSuccess = { _uiState.update { it.copy(deleted = true) } },
            call = { priceRepository.deleteProduct(productId) },
        )
    }

    fun deletePurchase(purchase: Purchase) {
        viewModelScope.launch {
            val r = priceRepository.deletePurchase(purchase.id)
            if (r is ApiResult.Error) _uiState.update { it.copy(message = r.message) }
        }
    }

    // --- Selezione multipla (issue #10) ---
    val selection = SelectionController()

    /** Elimina tutti gli acquisti selezionati in UNA chiamata bulk, poi esce dalla selezione. */
    fun deleteSelected() {
        val ids = selection.ids.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val r = priceRepository.deletePurchasesBulk(ids)
            selection.clear()
            if (r is ApiResult.Error) _uiState.update { it.copy(message = r.message) }
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }

    /**
     * Granularità con dati sufficienti per un andamento: quelle che, raggruppando gli acquisti per
     * periodo, producono ≥ 2 periodi distinti (stessa soglia del grafico in [LineChart]). Le altre
     * non mostrano il chip; se nessuna la supera il pannello andamento resta nascosto.
     */
    private fun availableGranularities(purchases: List<Purchase>): Set<Granularity> =
        Granularity.entries.filterTo(mutableSetOf()) { g ->
            purchases.mapTo(mutableSetOf()) { periodKeyOf(it.purchasedAt, g) }.size >= 2
        }
}

/** Chiave di raggruppamento del periodo, allineata a PricesLocalDataSource.periodKey. */
private fun periodKeyOf(purchasedAt: String, g: Granularity): String = when (g) {
    Granularity.DAY -> purchasedAt.take(10)   // yyyy-MM-dd
    Granularity.YEAR -> purchasedAt.take(4)   // yyyy
    Granularity.MONTH -> purchasedAt.take(7)  // yyyy-MM
}

/**
 * Local-first: gli acquisti salvati in Room non conoscono il simbolo dell'unità (solo l'id). Lo
 * risolviamo dal catalogo unità così la UI mostra il simbolo come prima.
 */
private fun Purchase.withUnitSymbol(units: List<MeasurementUnit>): Purchase {
    if (unitSymbol != null) return this
    val sym = units.firstOrNull { it.id == unitId }?.symbol ?: return this
    return copy(unitSymbol = sym)
}

/**
 * Local-first: il prodotto salvato in Room conosce solo [Product.defaultUnitId], non il simbolo
 * ([Product.defaultUnitSymbol] è null). Lo risolviamo dal catalogo unità, così cambiando l'unità di
 * default del prodotto tutte le voci "unità" (grafico, statistiche, acquisti, elenco) si aggiornano.
 * Package-internal: usato anche da [PriceListViewModel].
 */
internal fun Product.withDefaultUnitSymbol(units: List<MeasurementUnit>): Product {
    val sym = units.firstOrNull { it.id == defaultUnitId }?.symbol ?: return this
    return copy(defaultUnitSymbol = sym)
}
