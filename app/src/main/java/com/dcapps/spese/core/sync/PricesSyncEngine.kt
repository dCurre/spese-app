package com.dcapps.spese.core.sync

import com.dcapps.spese.core.auth.SessionManager
import com.dcapps.spese.core.bugreport.ErrorReporter
import com.dcapps.spese.core.bugreport.SyncErrorAutoReporter
import com.dcapps.spese.core.local.dao.PricesDao
import com.dcapps.spese.core.local.entity.LocalProductEntity
import com.dcapps.spese.core.local.entity.LocalPurchaseEntity
import com.dcapps.spese.core.model.Product
import com.dcapps.spese.core.model.ProductsBatchSyncRequest
import com.dcapps.spese.core.model.Purchase
import com.dcapps.spese.core.model.SyncProductCreate
import com.dcapps.spese.core.model.SyncProductUpdate
import com.dcapps.spese.core.model.SyncPurchaseCreate
import com.dcapps.spese.core.model.SyncPurchaseUpdate
import com.dcapps.spese.core.network.SpeseApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sincronizza i prezzi local-first (Room) col backend delta. SOLO per utente loggato (il guest non
 * sincronizza finché non fa l'upgrade). Un ciclo [sync] fa: PUSH (modifiche locali → backend) poi
 * PULL (delta → Room, merge last-write-wins per riga). Due livelli: prodotti → acquisti.
 *
 * Idempotenza: ogni riga porta un `clientId` (UUID) che il backend usa per deduplicare. Il push è
 * snapshot-safe: azzera `pendingSync` solo se la riga non è cambiata durante l'invio.
 *
 * Differenze dalle checklist: i prodotti sono GLOBALI (nessuna lista/owner padre), gli acquisti
 * portano lo `userId` del chiamante loggato.
 */
@Singleton
class PricesSyncEngine @Inject constructor(
    private val api: SpeseApi,
    private val dao: PricesDao,
    private val session: SessionManager,
    private val cursor: SyncCursor,
    private val errorReporter: ErrorReporter,
    private val syncStatus: SyncStatusReporter,
    private val autoReporter: SyncErrorAutoReporter,
) {
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob())
    private var debounceJob: Job? = null
    @Volatile private var rerunRequested = false

    /** Esegue un ciclo completo di sync. No-op in guest o senza utente. Best-effort (non lancia). */
    suspend fun sync() {
        if (session.isGuest) return
        mutex.withLock { runOneCycle() }
    }

    /** Un singolo ciclo push+pull. DEVE girare sotto [mutex]. Mai interrotto a metà da un debounce. */
    private suspend fun runOneCycle() {
        val userId = session.currentUser.value?.id ?: return
        syncStatus.onSyncStarted()
        var failed = false
        try {
            failed = errorReporter.runSyncStep("prezzi push", autoReporter) { push(userId) } || failed
            failed = errorReporter.runSyncStep("prezzi pull", autoReporter) { pull(userId) } || failed
        } finally {
            syncStatus.onSyncFinished(failed)
        }
    }

    /**
     * Richiesta di sync DEBOUNCED: il repository la chiama dopo ogni scrittura locale. Più scritture
     * ravvicinate collassano in un solo ciclo di sync (evita una chiamata di rete per tap).
     *
     * Il `cancel()` del debounce annulla SOLO l'attesa [delay], mai un ciclo già partito: il ciclo
     * vero gira in una coroutine separata ([drain]) protetta da [mutex]. Se una nuova richiesta
     * arriva mentre un ciclo è in corso, NON lo interrompe: alza [rerunRequested] così [drain] fa un
     * altro giro al termine (coda "pending re-run", niente sync cancellati a metà né duplicati).
     */
    fun requestSync() {
        if (session.isGuest) return
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(1500)
            rerunRequested = true
            scope.launch { drain() } // lancio indipendente: il prossimo cancel() del debounce non lo tocca
        }
    }

    /** Esegue cicli finché restano richieste pendenti. Un solo [drain] alla volta grazie al [mutex]. */
    private suspend fun drain() {
        if (!mutex.tryLock()) return // un drain è già attivo: raccoglierà lui il rerun richiesto
        try {
            while (rerunRequested) {
                rerunRequested = false
                runOneCycle()
            }
        } finally {
            mutex.unlock()
        }
    }

    // ---------------- PUSH ----------------

    private suspend fun push(userId: Long) {
        val products = dao.pendingProducts()
        val purchases = dao.pendingPurchases()
        if (products.isEmpty() && purchases.isEmpty()) return

        fun tempOf(prefix: String, id: Long) = "$prefix$id"

        val productsCreate = mutableListOf<SyncProductCreate>()
        val productsUpdate = mutableListOf<SyncProductUpdate>()
        val productsDelete = mutableListOf<Long>()

        products.forEach { p ->
            when {
                p.deleted && p.remoteId == null -> dao.hardDeleteProduct(p.id) // mai sul server
                p.deleted -> productsDelete += p.remoteId!!
                p.remoteId == null -> productsCreate += SyncProductCreate(
                    tempId = tempOf("P", p.id), clientId = p.clientId,
                    name = p.name, brand = p.brand, defaultUnitId = p.defaultUnitId,
                )
                else -> productsUpdate += SyncProductUpdate(p.remoteId, p.name, p.brand, p.defaultUnitId)
            }
        }

        val purchasesCreate = mutableListOf<SyncPurchaseCreate>()
        val purchasesUpdate = mutableListOf<SyncPurchaseUpdate>()
        val purchasesDelete = mutableListOf<Long>()

        purchases.forEach { u ->
            when {
                u.deleted && u.remoteId == null -> dao.hardDeletePurchase(u.id)
                u.deleted -> purchasesDelete += u.remoteId!!
                u.remoteId == null -> purchasesCreate += SyncPurchaseCreate(
                    tempId = tempOf("U", u.id), clientId = u.clientId,
                    productId = productRemoteId(u.productId), productTempId = productTempIfNew(u.productId),
                    userId = userId,
                    purchasedAt = u.purchasedAt, price = u.price, quantity = u.quantity,
                    unitId = u.unitId, store = u.store, notes = u.notes,
                )
                else -> purchasesUpdate += SyncPurchaseUpdate(
                    u.remoteId!!, u.purchasedAt, u.price, u.quantity, u.unitId, u.store, u.notes,
                )
            }
        }

        if (productsCreate.isEmpty() && productsUpdate.isEmpty() && productsDelete.isEmpty() &&
            purchasesCreate.isEmpty() && purchasesUpdate.isEmpty() && purchasesDelete.isEmpty()
        ) return

        val resp = api.batchSyncProducts(
            ProductsBatchSyncRequest(
                productsCreate = productsCreate, productsUpdate = productsUpdate, productsDelete = productsDelete,
                purchasesCreate = purchasesCreate, purchasesUpdate = purchasesUpdate, purchasesDelete = purchasesDelete,
            ),
        )

        // Applica id_map: salva i remoteId delle nuove righe (temp_id → id reale).
        resp.idMap.products.forEach { (temp, remote) -> bindProductRemote(temp, remote) }
        resp.idMap.purchases.forEach { (temp, remote) -> bindPurchaseRemote(temp, remote) }

        // Azzera pendingSync (snapshot-safe) per le righe inviate non cancellate; hard-delete per i
        // tombstone propagati.
        clearPendingOrHardDelete(products, purchases)
    }

    private suspend fun productRemoteId(localProductId: Long): Long? = dao.getProduct(localProductId)?.remoteId
    private suspend fun productTempIfNew(localProductId: Long): String? =
        dao.getProduct(localProductId)?.let { if (it.remoteId == null) "P${it.id}" else null }

    private suspend fun bindProductRemote(temp: String, remote: Long) {
        val localId = temp.removePrefix("P").toLongOrNull() ?: return
        dao.getProduct(localId)?.let { dao.updateProduct(it.copy(remoteId = remote)) }
    }
    private suspend fun bindPurchaseRemote(temp: String, remote: Long) {
        val localId = temp.removePrefix("U").toLongOrNull() ?: return
        dao.getPurchase(localId)?.let { dao.updatePurchase(it.copy(remoteId = remote)) }
    }

    /** Snapshot-safe: azzera pendingSync solo se updatedAt non è cambiato dopo lo snapshot. */
    private suspend fun clearPendingOrHardDelete(
        products: List<LocalProductEntity>,
        purchases: List<LocalPurchaseEntity>,
    ) {
        products.forEach { snap ->
            val cur = dao.getProduct(snap.id) ?: return@forEach
            if (cur.deleted) dao.hardDeleteProduct(cur.id)
            else if (cur.updatedAt == snap.updatedAt) dao.updateProduct(cur.copy(pendingSync = false))
        }
        purchases.forEach { snap ->
            val cur = dao.getPurchase(snap.id) ?: return@forEach
            if (cur.deleted) dao.hardDeletePurchase(cur.id)
            else if (cur.updatedAt == snap.updatedAt) dao.updatePurchase(cur.copy(pendingSync = false))
        }
    }

    // ---------------- PULL ----------------

    private suspend fun pull(userId: Long) {
        val since = cursor.get(SyncCursor.PRICES)
        val delta = api.getProductsDelta(userId, since)
        delta.products.forEach { applyServerProduct(it) }
        delta.purchases.forEach { applyServerPurchase(it) }
        cursor.set(SyncCursor.PRICES, delta.serverTime)
    }

    private fun epoch(instantIso: java.time.Instant?): Long = instantIso?.toEpochMilli() ?: 0L

    private fun unitPriceOf(price: Double, quantity: Double): Double =
        if (quantity > 0.0) price / quantity else 0.0

    /** Prodotti GLOBALI: nessuna lista/owner padre, si applicano direttamente. */
    private suspend fun applyServerProduct(s: Product) {
        val local = s.clientId?.let { dao.productByClientId(it) } ?: dao.productByRemoteId(s.id)
        val serverUpdated = epoch(s.updatedAt)
        if (local == null) {
            if (s.deleted) return // niente da creare per una riga già cancellata
            dao.insertProductRaw(
                LocalProductEntity(
                    name = s.name, brand = s.brand, defaultUnitId = s.defaultUnitId,
                    clientId = s.clientId ?: java.util.UUID.randomUUID().toString(),
                    remoteId = s.id, pendingSync = false, deleted = false, updatedAt = serverUpdated,
                ),
            )
            return
        }
        // Conflitto: la cancellazione vince.
        if (s.deleted) { dao.hardDeleteProduct(local.id); return }
        // LWW: applica il server solo se più recente e la riga locale non ha modifiche pending più nuove.
        if (!local.pendingSync && serverUpdated >= local.updatedAt) {
            dao.updateProduct(
                local.copy(
                    name = s.name, brand = s.brand, defaultUnitId = s.defaultUnitId,
                    remoteId = s.id, updatedAt = serverUpdated,
                ),
            )
        } else if (local.remoteId == null) {
            dao.updateProduct(local.copy(remoteId = s.id))
        }
    }

    private suspend fun applyServerPurchase(s: Purchase) {
        val local = s.clientId?.let { dao.purchaseByClientId(it) } ?: dao.purchaseByRemoteId(s.id)
        val serverUpdated = epoch(s.updatedAt)
        val localProductId = dao.productIdByRemote(s.productId)
        if (local == null) {
            if (s.deleted || localProductId == null) return
            dao.insertPurchaseRaw(
                LocalPurchaseEntity(
                    productId = localProductId, purchasedAt = s.purchasedAt,
                    price = s.price, quantity = s.quantity,
                    unitId = s.unitId, unitSymbol = s.unitSymbol,
                    unitPrice = unitPriceOf(s.price, s.quantity),
                    store = s.store, notes = s.notes,
                    clientId = s.clientId ?: java.util.UUID.randomUUID().toString(),
                    remoteId = s.id, pendingSync = false, deleted = false, updatedAt = serverUpdated,
                ),
            )
            return
        }
        if (s.deleted) { dao.hardDeletePurchase(local.id); return }
        if (!local.pendingSync && serverUpdated >= local.updatedAt) {
            dao.updatePurchase(
                local.copy(
                    purchasedAt = s.purchasedAt, price = s.price, quantity = s.quantity,
                    unitId = s.unitId, unitSymbol = s.unitSymbol,
                    unitPrice = unitPriceOf(s.price, s.quantity),
                    store = s.store, notes = s.notes, remoteId = s.id, updatedAt = serverUpdated,
                ),
            )
        } else if (local.remoteId == null) {
            dao.updatePurchase(local.copy(remoteId = s.id))
        }
    }
}
