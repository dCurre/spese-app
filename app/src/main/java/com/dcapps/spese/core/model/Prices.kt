package com.dcapps.spese.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Prodotto dello storico prezzi. */
@Serializable
data class Product(
    val id: Long,
    val name: String = "",
    val brand: String? = null,
    @SerialName("default_unit_id") val defaultUnitId: Long? = null,
    @SerialName("default_unit_symbol") val defaultUnitSymbol: String? = null,
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
)

@Serializable
data class PurchasesResponse(val purchases: List<Purchase> = emptyList())

@Serializable
data class CreateProductRequest(
    val name: String,
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
