package com.dcapps.spese.core.model

import com.dcapps.spese.core.network.serialization.InstantSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

/** Prodotto dello storico prezzi. */
@Serializable
data class Product(
    val id: Long,
    val name: String = "",
    val brand: String? = null,
    @SerialName("default_unit_id") val defaultUnitId: Long? = null,
    @SerialName("default_unit_symbol") val defaultUnitSymbol: String? = null,
    // Numero di acquisti registrati per il prodotto (badge in lista).
    @SerialName("purchase_count") val purchaseCount: Int = 0,
    // Prezzo unitario minimo di sempre (null se nessun acquisto).
    @SerialName("min_price") val minPrice: Double? = null,
    // Campi delta sync (backend local-first).
    @SerialName("client_id") val clientId: String? = null,
    @SerialName("updated_at") @Serializable(InstantSerializer::class) val updatedAt: Instant? = null,
    val deleted: Boolean = false,
)

@Serializable
data class ProductsResponse(val products: List<Product> = emptyList())

/** Acquisto registrato. Qui price/quantity/unit_price sono numeri (float), non stringhe. */
@Serializable
data class Purchase(
    val id: Long,
    @SerialName("product_id") val productId: Long,
    @SerialName("user_id") val userId: Long,
    @SerialName("purchased_at") val purchasedAt: String,
    val price: Double = 0.0,
    val quantity: Double = 0.0,
    @SerialName("unit_id") val unitId: Long? = null,
    @SerialName("unit_symbol") val unitSymbol: String? = null,
    @SerialName("unit_price") val unitPrice: Double = 0.0,
    val store: String? = null,
    val notes: String? = null,
    // Campi delta sync (backend local-first).
    @SerialName("client_id") val clientId: String? = null,
    @SerialName("updated_at") @Serializable(InstantSerializer::class) val updatedAt: Instant? = null,
    val deleted: Boolean = false,
)

@Serializable
data class PurchasesResponse(val purchases: List<Purchase> = emptyList())

@Serializable
data class CreateProductRequest(
    val name: String,
    val brand: String? = null,
    @SerialName("default_unit_id") val defaultUnitId: Long? = null,
)

/** Body modifica prodotto (parziale). */
@Serializable
data class UpdateProductRequest(
    val name: String? = null,
    val brand: String? = null,
    @SerialName("default_unit_id") val defaultUnitId: Long? = null,
)

@Serializable
data class CreatePurchaseRequest(
    @SerialName("product_id") val productId: Long,
    @SerialName("user_id") val userId: Long,
    @SerialName("purchased_at") val purchasedAt: String,
    val price: Double,
    val quantity: Double,
    @SerialName("unit_id") val unitId: Long,
    val store: String? = null,
    val notes: String? = null,
)

@Serializable
data class UpdatePurchaseRequest(
    @SerialName("purchased_at") val purchasedAt: String? = null,
    val price: Double? = null,
    val quantity: Double? = null,
    @SerialName("unit_id") val unitId: Long? = null,
    val store: String? = null,
    val notes: String? = null,
)

@Serializable
data class PriceHistoryPoint(
    val period: String,
    @SerialName("avg_unit_price") val avgUnitPrice: Double = 0.0,
    @SerialName("min_unit_price") val minUnitPrice: Double = 0.0,
    @SerialName("max_unit_price") val maxUnitPrice: Double = 0.0,
    val count: Int = 0,
)

@Serializable
data class PriceHistoryResponse(
    @SerialName("product_id") val productId: Long,
    @SerialName("product_name") val productName: String = "",
    @SerialName("unit_symbol") val unitSymbol: String? = null,
    val data: List<PriceHistoryPoint> = emptyList(),
)

// --- Delta sync (local-first) ---

/** Risposta di GET /api/products/delta: prodotti (globali) e acquisti (del chiamante) dopo `since`. */
@Serializable
data class ProductsDeltaResponse(
    @SerialName("server_time") val serverTime: String,
    val products: List<Product> = emptyList(),
    val purchases: List<Purchase> = emptyList(),
)

/** Blocco "create prodotto" per il batch-sync: temp_id referenzia il nuovo prodotto dagli acquisti. */
@Serializable
data class SyncProductCreate(
    @SerialName("temp_id") val tempId: String,
    @SerialName("client_id") val clientId: String,
    val name: String,
    val brand: String? = null,
    @SerialName("default_unit_id") val defaultUnitId: Long? = null,
)

@Serializable
data class SyncProductUpdate(
    val id: Long,
    val name: String? = null,
    val brand: String? = null,
    @SerialName("default_unit_id") val defaultUnitId: Long? = null,
)

@Serializable
data class SyncPurchaseCreate(
    @SerialName("temp_id") val tempId: String,
    @SerialName("client_id") val clientId: String,
    // product_id o product_temp_id (uno dei due): il prodotto può essere nuovo (temp) o esistente.
    @SerialName("product_id") val productId: Long? = null,
    @SerialName("product_temp_id") val productTempId: String? = null,
    @SerialName("user_id") val userId: Long,
    @SerialName("purchased_at") val purchasedAt: String,
    val price: Double,
    val quantity: Double,
    @SerialName("unit_id") val unitId: Long? = null,
    val store: String? = null,
    val notes: String? = null,
)

@Serializable
data class SyncPurchaseUpdate(
    val id: Long,
    @SerialName("purchased_at") val purchasedAt: String? = null,
    val price: Double? = null,
    val quantity: Double? = null,
    @SerialName("unit_id") val unitId: Long? = null,
    val store: String? = null,
    val notes: String? = null,
)

@Serializable
data class ProductsBatchSyncRequest(
    @SerialName("products_create") val productsCreate: List<SyncProductCreate> = emptyList(),
    @SerialName("products_update") val productsUpdate: List<SyncProductUpdate> = emptyList(),
    @SerialName("products_delete") val productsDelete: List<Long> = emptyList(),
    @SerialName("purchases_create") val purchasesCreate: List<SyncPurchaseCreate> = emptyList(),
    @SerialName("purchases_update") val purchasesUpdate: List<SyncPurchaseUpdate> = emptyList(),
    @SerialName("purchases_delete") val purchasesDelete: List<Long> = emptyList(),
)

/** Mappa temp_id → id reale per ciascuna entità, restituita dal batch-sync prezzi. */
@Serializable
data class ProductsSyncIdMap(
    val products: Map<String, Long> = emptyMap(),
    val purchases: Map<String, Long> = emptyMap(),
)

@Serializable
data class ProductsBatchSyncResponse(
    @SerialName("server_time") val serverTime: String,
    @SerialName("id_map") val idMap: ProductsSyncIdMap = ProductsSyncIdMap(),
)
