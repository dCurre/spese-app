package com.dcapps.spese.core.local

import com.dcapps.spese.core.local.dao.PricesDao
import com.dcapps.spese.core.local.entity.LocalProductEntity
import com.dcapps.spese.core.local.entity.LocalPurchaseEntity
import com.dcapps.spese.core.model.PriceHistoryPoint
import com.dcapps.spese.core.model.Product
import com.dcapps.spese.core.model.Purchase
import com.dcapps.spese.core.network.ApiResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sorgente dati LOCALE (Room) local-first dei PREZZI (prodotti + acquisti). Le letture per la UI
 * sono Flow reattivi; le scritture marcano `pendingSync`/`updatedAt`; la cancellazione è un tombstone
 * (`deleted = true`), non una DELETE fisica.
 *
 * Alcuni valori che lato backend erano calcolati (purchaseCount, minPrice, storico aggregato) qui
 * vengono ricalcolati localmente dagli acquisti → la UI resta identica, ma tutto è offline.
 */
@Singleton
class PricesLocalDataSource @Inject constructor(
    private val dao: PricesDao,
) {
    private fun now() = System.currentTimeMillis()

    // --- Letture reattive (Flow) ---

    /** Tutti i prodotti con conteggio acquisti e prezzo unitario minimo (reattivi agli acquisti). */
    fun observeProducts(): Flow<List<Product>> =
        dao.observeProducts().combine(dao.observeAllPurchases()) { products, allPurchases ->
            products.map { p ->
                val mine = allPurchases.filter { it.productId == p.id && !it.deleted }
                p.toModel(
                    purchaseCount = mine.size,
                    minPrice = mine.minOfOrNull { it.unitPrice },
                )
            }
        }

    /** Un prodotto, reattivo (con conteggio acquisti e prezzo minimo). */
    fun observeProduct(id: Long): Flow<Product?> =
        dao.observeProduct(id).combine(dao.observeAllPurchases()) { productEntity, allPurchases ->
            val mine = allPurchases.filter { it.productId == id && !it.deleted }
            productEntity?.toModel(
                purchaseCount = mine.size,
                minPrice = mine.minOfOrNull { it.unitPrice },
            )
        }

    /** Gli acquisti di un prodotto, reattivi (escludono i tombstone). */
    fun observePurchases(productId: Long): Flow<List<Purchase>> =
        dao.observePurchases(productId).map { list -> list.map { it.toModel() } }

    /**
     * Storico prezzi aggregato lato client: raggruppa gli acquisti non-deleted per periodo (giorno /
     * mese / anno) e calcola media/min/max del prezzo unitario + conteggio. Sostituisce l'endpoint
     * backend che non è ancora disponibile.
     */
    fun observeHistory(productId: Long, granularity: String): Flow<List<PriceHistoryPoint>> =
        dao.observePurchases(productId).map { purchases ->
            purchases
                .groupBy { periodKey(it.purchasedAt, granularity) }
                .map { (period, group) ->
                    val prices = group.map { it.unitPrice }
                    PriceHistoryPoint(
                        period = period,
                        avgUnitPrice = if (prices.isEmpty()) 0.0 else prices.average(),
                        minUnitPrice = prices.minOrNull() ?: 0.0,
                        maxUnitPrice = prices.maxOrNull() ?: 0.0,
                        count = group.size,
                    )
                }
                .sortedBy { it.period }
        }

    // --- Scritture (marcano pendingSync) ---

    suspend fun createProduct(
        name: String,
        brand: String?,
        defaultUnitId: Long?,
    ): ApiResult<Long> = guard {
        dao.insertProduct(
            LocalProductEntity(
                name = name,
                brand = brand,
                defaultUnitId = defaultUnitId,
                pendingSync = true,
                updatedAt = now(),
            ),
        )
    }

    suspend fun updateProduct(
        id: Long,
        name: String?,
        brand: String?,
        defaultUnitId: Long?,
    ): ApiResult<Unit> = guard {
        val cur = dao.getProduct(id) ?: error("Prodotto non trovato")
        dao.updateProduct(
            cur.copy(
                name = name ?: cur.name,
                brand = brand,
                defaultUnitId = defaultUnitId,
                pendingSync = true,
                updatedAt = now(),
            ),
        )
    }

    suspend fun deleteProduct(id: Long): ApiResult<Unit> = guard { tombstoneProduct(id) }

    suspend fun deleteProducts(ids: List<Long>): ApiResult<Unit> = guard { ids.forEach { tombstoneProduct(it) } }

    suspend fun addPurchase(
        productId: Long,
        purchasedAt: String,
        price: Double,
        quantity: Double,
        unitId: Long?,
        unitSymbol: String?,
        store: String?,
        notes: String?,
    ): ApiResult<Long> = guard {
        dao.insertPurchase(
            LocalPurchaseEntity(
                productId = productId,
                purchasedAt = purchasedAt,
                price = price,
                quantity = quantity,
                unitId = unitId,
                unitSymbol = unitSymbol,
                unitPrice = unitPriceOf(price, quantity),
                store = store,
                notes = notes,
                pendingSync = true,
                updatedAt = now(),
            ),
        )
    }

    suspend fun updatePurchase(
        id: Long,
        purchasedAt: String?,
        price: Double?,
        quantity: Double?,
        unitId: Long?,
        unitSymbol: String?,
        store: String?,
        notes: String?,
    ): ApiResult<Unit> = guard {
        val cur = dao.getPurchase(id) ?: error("Acquisto non trovato")
        val newPrice = price ?: cur.price
        val newQty = quantity ?: cur.quantity
        dao.updatePurchase(
            cur.copy(
                purchasedAt = purchasedAt ?: cur.purchasedAt,
                price = newPrice,
                quantity = newQty,
                unitId = unitId ?: cur.unitId,
                unitSymbol = unitSymbol ?: cur.unitSymbol,
                unitPrice = unitPriceOf(newPrice, newQty),
                store = store,
                notes = notes,
                pendingSync = true,
                updatedAt = now(),
            ),
        )
    }

    suspend fun deletePurchase(id: Long): ApiResult<Unit> = guard { tombstonePurchase(id) }

    suspend fun deletePurchases(ids: List<Long>): ApiResult<Unit> = guard { ids.forEach { tombstonePurchase(it) } }

    // --- helper tombstone ---

    private suspend fun tombstoneProduct(id: Long) {
        val e = dao.getProduct(id) ?: return
        dao.updateProduct(e.copy(deleted = true, pendingSync = true, updatedAt = now()))
    }

    private suspend fun tombstonePurchase(id: Long) {
        val e = dao.getPurchase(id) ?: return
        dao.updatePurchase(e.copy(deleted = true, pendingSync = true, updatedAt = now()))
    }
}

private fun unitPriceOf(price: Double, quantity: Double): Double =
    if (quantity > 0.0) price / quantity else 0.0

/** Chiave di raggruppamento dello storico a partire da una data "yyyy-MM-dd". */
private fun periodKey(purchasedAt: String, granularity: String): String =
    when (granularity) {
        "day" -> purchasedAt.take(10)   // yyyy-MM-dd
        "year" -> purchasedAt.take(4)   // yyyy
        else -> purchasedAt.take(7)     // yyyy-MM (month, default)
    }

private inline fun <T> guard(block: () -> T): ApiResult<T> =
    try {
        ApiResult.Success(block())
    } catch (e: Exception) {
        ApiResult.Error(message = e.message ?: "Errore locale", cause = e)
    }

private fun LocalProductEntity.toModel(purchaseCount: Int, minPrice: Double?) = Product(
    id = id,
    name = name,
    brand = brand,
    defaultUnitId = defaultUnitId,
    defaultUnitSymbol = null,
    purchaseCount = purchaseCount,
    minPrice = minPrice,
)

private fun LocalPurchaseEntity.toModel() = Purchase(
    id = id,
    productId = productId,
    userId = GUEST_OWNER_ID,
    purchasedAt = purchasedAt,
    price = price,
    quantity = quantity,
    unitId = unitId,
    unitSymbol = unitSymbol,
    unitPrice = unitPrice,
    store = store,
    notes = notes,
)
