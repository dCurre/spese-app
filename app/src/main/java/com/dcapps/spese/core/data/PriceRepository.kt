package com.dcapps.spese.core.data

import com.dcapps.spese.core.local.PricesLocalDataSource
import com.dcapps.spese.core.model.CreateProductRequest
import com.dcapps.spese.core.model.CreatePurchaseRequest
import com.dcapps.spese.core.model.PriceHistoryPoint
import com.dcapps.spese.core.model.PriceHistoryResponse
import com.dcapps.spese.core.model.Product
import com.dcapps.spese.core.model.Purchase
import com.dcapps.spese.core.model.UpdateProductRequest
import com.dcapps.spese.core.model.UpdatePurchaseRequest
import com.dcapps.spese.core.network.ApiResult
import com.dcapps.spese.core.network.mapTo
import com.dcapps.spese.core.sync.PricesSyncEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Prezzi LOCAL-FIRST: i DATI (prodotti + acquisti) vivono sempre su Room, la UI è istantanea e
 * reattiva. Conteggi, prezzo minimo e storico aggregato sono ricalcolati localmente dagli acquisti.
 * Dopo ogni scrittura locale si richiede un sync col backend ([afterWrite] → [PricesSyncEngine]);
 * in guest il sync è no-op (i dati restano solo locali finché non si fa l'upgrade). I prodotti non
 * hanno condivisione, quindi nessuna operazione online dedicata.
 *
 * Espone sia letture reattive ([observeProducts]/[observeProduct]/[observePurchases]/[observeHistory],
 * Flow) sia operazioni one-shot compatibili coi ViewModel attuali.
 */
@Singleton
class PriceRepository @Inject constructor(
    private val local: PricesLocalDataSource,
    private val syncEngine: PricesSyncEngine,
) {
    // --- Letture reattive (local-first) ---
    fun observeProducts(): Flow<List<Product>> = local.observeProducts()
    fun observeProduct(id: Long): Flow<Product?> = local.observeProduct(id)
    fun observePurchases(productId: Long): Flow<List<Purchase>> = local.observePurchases(productId)
    fun observeHistory(productId: Long, granularity: String): Flow<List<PriceHistoryPoint>> =
        local.observeHistory(productId, granularity)

    // --- Letture one-shot (compat VM: prima emissione del Flow locale) ---
    suspend fun getProducts(): ApiResult<List<Product>> =
        ApiResult.Success(local.observeProducts().first())

    suspend fun getProduct(id: Long): ApiResult<Product> {
        val p = local.observeProduct(id).first()
            ?: return ApiResult.Error(message = "Prodotto non trovato")
        return ApiResult.Success(p)
    }

    suspend fun getPurchases(productId: Long): ApiResult<List<Purchase>> =
        ApiResult.Success(local.observePurchases(productId).first())

    suspend fun getPriceHistory(
        productId: Long,
        granularity: String = "month",
    ): ApiResult<PriceHistoryResponse> =
        ApiResult.Success(
            PriceHistoryResponse(
                productId = productId,
                data = local.observeHistory(productId, granularity).first(),
            ),
        )

    // --- Scritture DATI (sempre Room) ---
    suspend fun createProduct(body: CreateProductRequest): ApiResult<Long?> =
        afterWrite(
            local.createProduct(
                name = body.name,
                brand = body.brand,
                defaultUnitId = body.defaultUnitId,
            ).mapTo<Long, Long?>(null),
        )

    suspend fun updateProduct(id: Long, body: UpdateProductRequest): ApiResult<Unit> =
        afterWrite(
            local.updateProduct(
                id = id,
                name = body.name,
                brand = body.brand,
                defaultUnitId = body.defaultUnitId,
            ),
        )

    suspend fun deleteProduct(id: Long): ApiResult<Unit> = afterWrite(local.deleteProduct(id))

    suspend fun deleteProductsBulk(ids: List<Long>): ApiResult<Unit> = afterWrite(local.deleteProducts(ids))

    suspend fun createPurchase(body: CreatePurchaseRequest): ApiResult<Long?> =
        afterWrite(
            local.addPurchase(
                productId = body.productId,
                purchasedAt = body.purchasedAt,
                price = body.price,
                quantity = body.quantity,
                unitId = body.unitId,
                unitSymbol = null,
                store = body.store,
                notes = body.notes,
            ).mapTo<Long, Long?>(null),
        )

    suspend fun updatePurchase(id: Long, body: UpdatePurchaseRequest): ApiResult<Unit> =
        afterWrite(
            local.updatePurchase(
                id = id,
                purchasedAt = body.purchasedAt,
                price = body.price,
                quantity = body.quantity,
                unitId = body.unitId,
                unitSymbol = null,
                store = body.store,
                notes = body.notes,
            ),
        )

    suspend fun deletePurchase(id: Long): ApiResult<Unit> = afterWrite(local.deletePurchase(id))

    suspend fun deletePurchasesBulk(ids: List<Long>): ApiResult<Unit> = afterWrite(local.deletePurchases(ids))

    /**
     * Dopo una scrittura locale riuscita richiede un sync (debounced). In guest è no-op: i dati
     * prezzi restano solo locali finché non si fa l'upgrade a un account.
     */
    private fun <T> afterWrite(result: ApiResult<T>): ApiResult<T> {
        if (result is ApiResult.Success) syncEngine.requestSync()
        return result
    }
}
