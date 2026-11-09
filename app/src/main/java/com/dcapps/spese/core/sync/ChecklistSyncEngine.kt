package com.dcapps.spese.core.sync

import com.dcapps.spese.core.auth.SessionManager
import com.dcapps.spese.core.bugreport.ErrorReporter
import com.dcapps.spese.core.local.dao.ChecklistDao
import com.dcapps.spese.core.local.entity.LocalShoppingCategoryEntity
import com.dcapps.spese.core.local.entity.LocalShoppingItemEntity
import com.dcapps.spese.core.local.entity.LocalShoppingListEntity
import com.dcapps.spese.core.model.BatchSyncRequest
import com.dcapps.spese.core.model.ShoppingCategory
import com.dcapps.spese.core.model.ShoppingItem
import com.dcapps.spese.core.model.ShoppingList
import com.dcapps.spese.core.model.SyncCategoryCreate
import com.dcapps.spese.core.model.SyncCategoryUpdate
import com.dcapps.spese.core.model.SyncItemCreate
import com.dcapps.spese.core.model.SyncItemUpdate
import com.dcapps.spese.core.model.SyncListCreate
import com.dcapps.spese.core.model.SyncListUpdate
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
 * Sincronizza le checklist local-first (Room) col backend delta. SOLO per utente loggato (il guest
 * non sincronizza finché non fa l'upgrade). Un ciclo [sync] fa: PUSH (modifiche locali → backend) poi
 * PULL (delta → Room, merge last-write-wins per riga). Vedi regole nel design §10/§11.
 *
 * Idempotenza: ogni riga porta un `clientId` (UUID) che il backend usa per deduplicare, quindi un
 * re-invio non crea duplicati. Il push è snapshot-safe: azzera `pendingSync` solo se la riga non è
 * cambiata durante l'invio.
 */
@Singleton
class ChecklistSyncEngine @Inject constructor(
    private val api: SpeseApi,
    private val dao: ChecklistDao,
    private val session: SessionManager,
    private val cursor: SyncCursor,
    private val errorReporter: ErrorReporter,
    private val syncStatus: SyncStatusReporter,
) {
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob())
    private var debounceJob: Job? = null

    /** Esegue un ciclo completo di sync. No-op in guest o senza utente. Best-effort (non lancia). */
    suspend fun sync() {
        if (session.isGuest) return
        val userId = session.currentUser.value?.id ?: return
        syncStatus.onSyncStarted()
        var failed = false
        try {
            mutex.withLock {
                failed = errorReporter.runSyncStep("checklist push") { push() } || failed
                failed = errorReporter.runSyncStep("checklist pull") { pull(userId) } || failed
            }
        } finally {
            syncStatus.onSyncFinished(failed)
        }
    }

    /**
     * Richiesta di sync DEBOUNCED: i repository la chiamano dopo ogni scrittura locale. Più scritture
     * ravvicinate collassano in un solo ciclo di sync (evita una chiamata di rete per tap).
     */
    fun requestSync() {
        if (session.isGuest) return
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(1500)
            sync()
        }
    }

    // ---------------- PUSH ----------------

    private suspend fun push() {
        val lists = dao.pendingLists()
        val cats = dao.pendingCategories()
        val items = dao.pendingItems()
        if (lists.isEmpty() && cats.isEmpty() && items.isEmpty()) return

        // Temp-id per le righe NUOVE (remoteId == null) così i figli possono referenziarle.
        fun tempOf(prefix: String, id: Long) = "$prefix$id"

        // Create+update+delete per ciascuna entità. Le righe deleted mai sincronizzate (remoteId null)
        // non vanno inviate: si rimuovono solo in locale (regola #5).
        val listsCreate = mutableListOf<SyncListCreate>()
        val listsUpdate = mutableListOf<SyncListUpdate>()
        val listsDelete = mutableListOf<Long>()
        val localDeleteOnly = mutableListOf<() -> Unit>() // azioni di hard-delete locale differite

        lists.forEach { l ->
            when {
                l.deleted && l.remoteId == null -> dao.hardDeleteList(l.id) // mai sul server
                l.deleted -> listsDelete += l.remoteId!!
                l.remoteId == null -> listsCreate += SyncListCreate(
                    tempId = tempOf("L", l.id), clientId = l.clientId, name = l.name,
                    completed = l.completed, starred = l.starred,
                )
                else -> listsUpdate += SyncListUpdate(l.remoteId, l.name, l.completed, l.starred)
            }
        }

        val catsCreate = mutableListOf<SyncCategoryCreate>()
        val catsUpdate = mutableListOf<SyncCategoryUpdate>()
        val catsDelete = mutableListOf<Long>()
        cats.forEach { c ->
            when {
                c.deleted && c.remoteId == null -> dao.hardDeleteCategory(c.id)
                c.deleted -> catsDelete += c.remoteId!!
                c.remoteId == null -> catsCreate += SyncCategoryCreate(
                    tempId = tempOf("C", c.id), clientId = c.clientId,
                    listId = listRemoteId(c.listId), listTempId = listTempIfNew(c.listId),
                    parentId = c.parentId?.let { listCatRemoteId(it) },
                    parentTempId = c.parentId?.let { catTempIfNew(it) },
                    name = c.name, sortOrder = c.sortOrder,
                )
                else -> catsUpdate += SyncCategoryUpdate(c.remoteId, c.name, c.parentId?.let { catRemote(it) }, c.sortOrder)
            }
        }

        val itemsCreate = mutableListOf<SyncItemCreate>()
        val itemsUpdate = mutableListOf<SyncItemUpdate>()
        val itemsDelete = mutableListOf<Long>()
        items.forEach { i ->
            when {
                i.deleted && i.remoteId == null -> dao.hardDeleteItem(i.id)
                i.deleted -> itemsDelete += i.remoteId!!
                i.remoteId == null -> itemsCreate += SyncItemCreate(
                    tempId = tempOf("I", i.id), clientId = i.clientId,
                    listId = listRemoteId(i.listId), listTempId = listTempIfNew(i.listId),
                    categoryId = i.categoryId?.let { catRemote(it) },
                    categoryTempId = i.categoryId?.let { catTempIfNew(it) },
                    name = i.name, quantity = i.quantity, checked = i.checked, sortOrder = i.sortOrder,
                )
                else -> itemsUpdate += SyncItemUpdate(i.remoteId, i.name, i.quantity, i.checked, i.categoryId?.let { catRemote(it) })
            }
        }

        if (listsCreate.isEmpty() && listsUpdate.isEmpty() && listsDelete.isEmpty() &&
            catsCreate.isEmpty() && catsUpdate.isEmpty() && catsDelete.isEmpty() &&
            itemsCreate.isEmpty() && itemsUpdate.isEmpty() && itemsDelete.isEmpty()
        ) return

        val resp = api.batchSyncShopping(
            BatchSyncRequest(
                listsCreate = listsCreate, listsUpdate = listsUpdate, listsDelete = listsDelete,
                categoriesCreate = catsCreate, categoriesUpdate = catsUpdate, categoriesDelete = catsDelete,
                itemsCreate = itemsCreate, itemsUpdate = itemsUpdate, itemsDelete = itemsDelete,
            ),
        )

        // Applica id_map: salva i remoteId delle nuove righe (temp_id → id reale).
        resp.idMap.lists.forEach { (temp, remote) -> bindListRemote(temp, remote) }
        resp.idMap.categories.forEach { (temp, remote) -> bindCategoryRemote(temp, remote) }
        resp.idMap.items.forEach { (temp, remote) -> bindItemRemote(temp, remote) }

        // Azzera pendingSync (snapshot-safe) per le righe inviate non cancellate; hard-delete per i
        // tombstone propagati.
        clearPendingOrHardDelete(lists, cats, items)
    }

    private suspend fun listRemoteId(localListId: Long): Long? = dao.getList(localListId)?.remoteId
    private suspend fun listTempIfNew(localListId: Long): String? =
        dao.getList(localListId)?.let { if (it.remoteId == null) "L${it.id}" else null }
    private suspend fun catRemote(localCatId: Long): Long? = dao.getCategoryLocal(localCatId)?.remoteId
    private suspend fun catTempIfNew(localCatId: Long): String? =
        dao.getCategoryLocal(localCatId)?.let { if (it.remoteId == null) "C${it.id}" else null }
    private suspend fun listCatRemoteId(localCatId: Long): Long? = catRemote(localCatId)

    private suspend fun bindListRemote(temp: String, remote: Long) {
        val localId = temp.removePrefix("L").toLongOrNull() ?: return
        dao.getList(localId)?.let { dao.updateList(it.copy(remoteId = remote)) }
    }
    private suspend fun bindCategoryRemote(temp: String, remote: Long) {
        val localId = temp.removePrefix("C").toLongOrNull() ?: return
        dao.getCategoryLocal(localId)?.let { dao.updateCategory(it.copy(remoteId = remote)) }
    }
    private suspend fun bindItemRemote(temp: String, remote: Long) {
        val localId = temp.removePrefix("I").toLongOrNull() ?: return
        dao.getItem(localId)?.let { dao.updateItem(it.copy(remoteId = remote)) }
    }

    /** Snapshot-safe: azzera pendingSync solo se updatedAt non è cambiato dopo lo snapshot. */
    private suspend fun clearPendingOrHardDelete(
        lists: List<LocalShoppingListEntity>,
        cats: List<LocalShoppingCategoryEntity>,
        items: List<LocalShoppingItemEntity>,
    ) {
        lists.forEach { snap ->
            val cur = dao.getList(snap.id) ?: return@forEach
            if (cur.deleted) dao.hardDeleteList(cur.id)
            else if (cur.updatedAt == snap.updatedAt) dao.updateList(cur.copy(pendingSync = false))
        }
        cats.forEach { snap ->
            val cur = dao.getCategoryLocal(snap.id) ?: return@forEach
            if (cur.deleted) dao.hardDeleteCategory(cur.id)
            else if (cur.updatedAt == snap.updatedAt) dao.updateCategory(cur.copy(pendingSync = false))
        }
        items.forEach { snap ->
            val cur = dao.getItem(snap.id) ?: return@forEach
            if (cur.deleted) dao.hardDeleteItem(cur.id)
            else if (cur.updatedAt == snap.updatedAt) dao.updateItem(cur.copy(pendingSync = false))
        }
    }

    // ---------------- PULL ----------------

    private suspend fun pull(userId: Long) {
        val since = cursor.get(SyncCursor.CHECKLIST)
        val delta = api.getShoppingDelta(userId, since)
        delta.lists.forEach { applyServerList(it) }
        delta.categories.forEach { applyServerCategory(it) }
        delta.items.forEach { applyServerItem(it) }
        cursor.set(SyncCursor.CHECKLIST, delta.serverTime)
    }

    private fun epoch(instantIso: java.time.Instant?): Long = instantIso?.toEpochMilli() ?: 0L

    private suspend fun applyServerList(s: ShoppingList) {
        val local = s.clientId?.let { dao.listByClientId(it) } ?: dao.listByRemoteId(s.id)
        val serverUpdated = epoch(s.updatedAt)
        if (local == null) {
            if (s.deleted) return // niente da creare per una riga già cancellata
            dao.insertListRaw(
                LocalShoppingListEntity(
                    name = s.name, completed = s.completed, starred = s.starred,
                    clientId = s.clientId ?: java.util.UUID.randomUUID().toString(),
                    remoteId = s.id, pendingSync = false, deleted = false, updatedAt = serverUpdated,
                ),
            )
            return
        }
        // Conflitto: la cancellazione vince (regola #6).
        if (s.deleted) { dao.hardDeleteList(local.id); return }
        // LWW: applica il server solo se più recente e la riga locale non ha modifiche pending più nuove.
        if (!local.pendingSync && serverUpdated >= local.updatedAt) {
            dao.updateList(local.copy(name = s.name, completed = s.completed, starred = s.starred, remoteId = s.id, updatedAt = serverUpdated))
        } else if (local.remoteId == null) {
            dao.updateList(local.copy(remoteId = s.id))
        }
    }

    private suspend fun applyServerCategory(s: ShoppingCategory) {
        val local = s.clientId?.let { dao.categoryByClientId(it) } ?: dao.categoryByRemoteId(s.id)
        val serverUpdated = epoch(s.updatedAt)
        val localListId = dao.listIdByRemote(s.shoppingListId)
        if (local == null) {
            if (s.deleted || localListId == null) return
            dao.insertCategoryRaw(
                LocalShoppingCategoryEntity(
                    listId = localListId, parentId = s.parentId?.let { dao.categoryIdByRemote(it) },
                    name = s.name, sortOrder = s.sortOrder,
                    clientId = s.clientId ?: java.util.UUID.randomUUID().toString(),
                    remoteId = s.id, pendingSync = false, deleted = false, updatedAt = serverUpdated,
                ),
            )
            return
        }
        if (s.deleted) { dao.hardDeleteCategory(local.id); return }
        if (!local.pendingSync && serverUpdated >= local.updatedAt) {
            dao.updateCategory(local.copy(name = s.name, sortOrder = s.sortOrder, remoteId = s.id, updatedAt = serverUpdated))
        } else if (local.remoteId == null) {
            dao.updateCategory(local.copy(remoteId = s.id))
        }
    }

    private suspend fun applyServerItem(s: ShoppingItem) {
        val local = s.clientId?.let { dao.itemByClientId(it) } ?: dao.itemByRemoteId(s.id)
        val serverUpdated = epoch(s.updatedAt)
        val localListId = dao.listIdByRemote(s.shoppingListId)
        if (local == null) {
            if (s.deleted || localListId == null) return
            dao.insertItemRaw(
                LocalShoppingItemEntity(
                    listId = localListId, categoryId = s.categoryId?.let { dao.categoryIdByRemote(it) },
                    name = s.name, quantity = s.quantity ?: 1, checked = s.checked, sortOrder = s.sortOrder,
                    clientId = s.clientId ?: java.util.UUID.randomUUID().toString(),
                    remoteId = s.id, pendingSync = false, deleted = false, updatedAt = serverUpdated,
                ),
            )
            return
        }
        if (s.deleted) { dao.hardDeleteItem(local.id); return }
        if (!local.pendingSync && serverUpdated >= local.updatedAt) {
            dao.updateItem(
                local.copy(
                    name = s.name, quantity = s.quantity ?: 1, checked = s.checked,
                    categoryId = s.categoryId?.let { dao.categoryIdByRemote(it) }, remoteId = s.id, updatedAt = serverUpdated,
                ),
            )
        } else if (local.remoteId == null) {
            dao.updateItem(local.copy(remoteId = s.id))
        }
    }
}
