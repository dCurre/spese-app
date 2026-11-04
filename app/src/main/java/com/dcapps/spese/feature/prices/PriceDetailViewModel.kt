package com.dcapps.spese.feature.prices

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dcapps.spese.core.auth.SessionManager
import com.dcapps.spese.core.data.CatalogRepository
import com.dcapps.spese.core.data.PriceRepository
import com.dcapps.spese.core.model.CreatePurchaseRequest
import com.dcapps.spese.core.model.UpdatePurchaseRequest
import com.dcapps.spese.core.model.MeasurementUnit
import com.dcapps.spese.core.model.PriceHistoryPoint
import com.dcapps.spese.core.model.Product
import com.dcapps.spese.core.model.Purchase
import com.dcapps.spese.core.network.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class Granularity(val wire: String, val label: String) {
    DAY("day", "Giorno"), MONTH("month", "Mese"), YEAR("year", "Anno")
}

data class PriceDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val product: Product? = null,
    val purchases: List<Purchase> = emptyList(),
    val history: List<PriceHistoryPoint> = emptyList(),
    val units: List<MeasurementUnit> = emptyList(),
    val granularity: Granularity = Granularity.MONTH,
    val message: String? = null,
    val deleted: Boolean = false,
)

@HiltViewModel
class PriceDetailViewModel @Inject constructor(
    private val priceRepository: PriceRepository,
    private val catalogRepository: CatalogRepository,
    private val session: SessionManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val productId: Long = checkNotNull(savedStateHandle["productId"])

    private val _uiState = MutableStateFlow(PriceDetailUiState())
    val uiState: StateFlow<PriceDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val gran = _uiState.value.granularity.wire
            val productD = async { priceRepository.getProduct(productId) }
            val purchasesD = async { priceRepository.getPurchases(productId) }
            val historyD = async { priceRepository.getPriceHistory(productId, gran) }

            val product = productD.await()
            if (product is ApiResult.Error) {
                _uiState.update { it.copy(isLoading = false, error = product.message) }
                return@launch
            }
            val unitsD = async { catalogRepository.getMeasurementUnits() }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    product = (product as ApiResult.Success).data,
                    purchases = (purchasesD.await() as? ApiResult.Success)?.data ?: emptyList(),
                    history = (historyD.await() as? ApiResult.Success)?.data?.data ?: emptyList(),
                    units = (unitsD.await() as? ApiResult.Success)?.data ?: it.units,
                )
            }
        }
    }

    fun setGranularity(g: Granularity) {
        _uiState.update { it.copy(granularity = g) }
        load()
    }

    fun addPurchase(
        price: Double,
        quantity: Double,
        unitId: Long,
        purchasedAt: String,
        store: String?,
        notes: String?,
    ) {
        val userId = session.currentUser.value?.id ?: return
        viewModelScope.launch {
            val result = priceRepository.createPurchase(
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
            when (result) {
                is ApiResult.Success -> load()
                is ApiResult.Error -> _uiState.update { it.copy(message = result.message) }
            }
        }
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
        viewModelScope.launch {
            val result = priceRepository.updatePurchase(
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
            when (result) {
                is ApiResult.Success -> load()
                is ApiResult.Error -> _uiState.update { it.copy(message = result.message) }
            }
        }
    }

    /** Modifica le proprietà del prodotto (nome, marca, unità predefinita). */
    fun updateProduct(name: String, brand: String?, defaultUnitId: Long?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val result = priceRepository.updateProduct(
                productId,
                com.dcapps.spese.core.model.UpdateProductRequest(
                    name = name.trim(),
                    brand = brand?.takeIf { it.isNotBlank() },
                    defaultUnitId = defaultUnitId,
                ),
            )
            when (result) {
                is ApiResult.Success -> load()
                is ApiResult.Error -> _uiState.update { it.copy(message = result.message) }
            }
        }
    }

    /** Elimina il prodotto e tutti i suoi acquisti. */
    fun deleteProduct() {
        viewModelScope.launch {
            when (val result = priceRepository.deleteProduct(productId)) {
                is ApiResult.Success -> _uiState.update { it.copy(deleted = true) }
                is ApiResult.Error -> _uiState.update { it.copy(message = result.message) }
            }
        }
    }

    fun deletePurchase(purchase: Purchase) {
        viewModelScope.launch {
            priceRepository.deletePurchase(purchase.id)
            load()
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }
}
