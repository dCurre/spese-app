package com.dcapps.spese.core.data

import com.dcapps.spese.core.model.BulkDeleteRequest
import com.dcapps.spese.core.model.CreateProductRequest
import com.dcapps.spese.core.model.CreatePurchaseRequest
import com.dcapps.spese.core.model.MeasurementUnit
import com.dcapps.spese.core.model.PriceHistoryResponse
import com.dcapps.spese.core.model.Product
import com.dcapps.spese.core.model.Purchase
import com.dcapps.spese.core.model.UpdateProductRequest
import com.dcapps.spese.core.model.UpdatePurchaseRequest
import com.dcapps.spese.core.network.ApiResult
import com.dcapps.spese.core.network.SpeseApi
import com.dcapps.spese.core.network.apiCall
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Accesso ai dati dello storico prezzi (prodotti, acquisti, serie prezzi). */
@Singleton
class PriceRepository @Inject constructor(
    private val api: SpeseApi,
    private val json: Json,
) {
    suspend fun getProducts(): ApiResult<List<Product>> =
        apiCall(json) { api.getProducts().products }

    suspend fun getProduct(id: Long): ApiResult<Product> =
        apiCall(json) { api.getProduct(id) }

    suspend fun createProduct(body: CreateProductRequest): ApiResult<Long?> =
        apiCall(json) { api.createProduct(body).id }

    suspend fun updateProduct(
        id: Long,
        body: UpdateProductRequest,
    ): ApiResult<Unit> = apiCall(json) { api.updateProduct(id, body); Unit }

    suspend fun deleteProduct(id: Long): ApiResult<Unit> =
        apiCall(json) { api.deleteProduct(id); Unit }

    /** Cancellazione massiva di prodotti in una sola chiamata. */
    suspend fun deleteProductsBulk(ids: List<Long>): ApiResult<Unit> =
        apiCall(json) { api.deleteProductsBulk(BulkDeleteRequest(ids)); Unit }

    suspend fun getPurchases(productId: Long): ApiResult<List<Purchase>> =
        apiCall(json) { api.getPurchases(productId).purchases }

    suspend fun createPurchase(body: CreatePurchaseRequest): ApiResult<Long?> =
        apiCall(json) { api.createPurchase(body).id }

    suspend fun updatePurchase(id: Long, body: UpdatePurchaseRequest): ApiResult<Unit> =
        apiCall(json) { api.updatePurchase(id, body); Unit }

    suspend fun deletePurchase(id: Long): ApiResult<Unit> =
        apiCall(json) { api.deletePurchase(id); Unit }

    /** Cancellazione massiva di acquisti in una sola chiamata. */
    suspend fun deletePurchasesBulk(ids: List<Long>): ApiResult<Unit> =
        apiCall(json) { api.deletePurchasesBulk(BulkDeleteRequest(ids)); Unit }

    suspend fun getPriceHistory(
        productId: Long,
        granularity: String = "month",
    ): ApiResult<PriceHistoryResponse> =
        apiCall(json) { api.getPriceHistory(productId, granularity, null, null) }
}
