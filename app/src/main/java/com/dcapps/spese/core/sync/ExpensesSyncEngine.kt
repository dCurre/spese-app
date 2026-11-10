package com.dcapps.spese.core.sync

import com.dcapps.spese.core.auth.SessionManager
import com.dcapps.spese.core.bugreport.ErrorReporter
import com.dcapps.spese.core.bugreport.SyncErrorAutoReporter
import com.dcapps.spese.core.local.dao.ExpensesDao
import com.dcapps.spese.core.local.entity.LocalExpenseEntity
import com.dcapps.spese.core.local.entity.LocalExpensesListEntity
import com.dcapps.spese.core.model.Expense
import com.dcapps.spese.core.model.ExpensesBatchSyncRequest
import com.dcapps.spese.core.model.ExpensesList
import com.dcapps.spese.core.model.SyncExpenseCreate
import com.dcapps.spese.core.model.SyncExpenseUpdate
import com.dcapps.spese.core.model.SyncExpensesListCreate
import com.dcapps.spese.core.model.SyncExpensesListUpdate
import com.dcapps.spese.core.network.SpeseApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sincronizza le SPESE local-first (Room) col backend delta. SOLO per utente loggato (il guest non
 * sincronizza finché non fa l'upgrade). Un ciclo [sync] fa: PUSH (modifiche locali → backend) poi
 * PULL (delta → Room, merge last-write-wins per riga). Replica del [ChecklistSyncEngine] su 2 livelli
 * (liste spese → spese).
 *
 * Idempotenza: ogni riga porta un `clientId` (UUID) che il backend usa per deduplicare, quindi un
 * re-invio non crea duplicati. Il push è snapshot-safe: azzera `pendingSync` solo se la riga non è
 * cambiata durante l'invio.
 */
@Singleton
class ExpensesSyncEngine @Inject constructor(
    private val api: SpeseApi,
    private val dao: ExpensesDao,
    private val session: SessionManager,
    private val cursor: SyncCursor,
    private val errorReporter: ErrorReporter,
    private val syncStatus: SyncStatusReporter,
    private val autoReporter: SyncErrorAutoReporter,
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
                failed = errorReporter.runSyncStep("spese push", autoReporter) { push() } || failed
                failed = errorReporter.runSyncStep("spese pull", autoReporter) { pull(userId) } || failed
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
        val expenses = dao.pendingExpenses()
        if (lists.isEmpty() && expenses.isEmpty()) return

        // Temp-id per le righe NUOVE (remoteId == null) così i figli possono referenziarle.
        fun tempOf(prefix: String, id: Long) = "$prefix$id"

        // Create+update+delete per ciascuna entità. Le righe deleted mai sincronizzate (remoteId null)
        // non vanno inviate: si rimuovono solo in locale (regola #5).
        val listsCreate = mutableListOf<SyncExpensesListCreate>()
        val listsUpdate = mutableListOf<SyncExpensesListUpdate>()
        val listsDelete = mutableListOf<Long>()

        lists.forEach { l ->
            when {
                l.deleted && l.remoteId == null -> dao.hardDeleteList(l.id) // mai sul server
                l.deleted -> listsDelete += l.remoteId!!
                l.remoteId == null -> listsCreate += SyncExpensesListCreate(
                    tempId = tempOf("L", l.id), clientId = l.clientId, name = l.name, paid = l.paid,
                )
                else -> listsUpdate += SyncExpensesListUpdate(l.remoteId, l.name, l.paid)
            }
        }

        val expensesCreate = mutableListOf<SyncExpenseCreate>()
        val expensesUpdate = mutableListOf<SyncExpenseUpdate>()
        val expensesDelete = mutableListOf<Long>()
        expenses.forEach { e ->
            when {
                e.deleted && e.remoteId == null -> dao.hardDeleteExpense(e.id)
                e.deleted -> expensesDelete += e.remoteId!!
                e.remoteId == null -> expensesCreate += SyncExpenseCreate(
                    tempId = tempOf("E", e.id), clientId = e.clientId,
                    listId = listRemoteId(e.listId), listTempId = listTempIfNew(e.listId),
                    name = e.name, amount = BigDecimal(e.amount),
                    expenseDate = e.expenseDate, expenseTypeId = e.expenseTypeId,
                )
                else -> expensesUpdate += SyncExpenseUpdate(
                    e.remoteId, e.name, BigDecimal(e.amount), e.expenseDate, e.expenseTypeId,
                )
            }
        }

        if (listsCreate.isEmpty() && listsUpdate.isEmpty() && listsDelete.isEmpty() &&
            expensesCreate.isEmpty() && expensesUpdate.isEmpty() && expensesDelete.isEmpty()
        ) return

        val resp = api.batchSyncExpenses(
            ExpensesBatchSyncRequest(
                listsCreate = listsCreate, listsUpdate = listsUpdate, listsDelete = listsDelete,
                expensesCreate = expensesCreate, expensesUpdate = expensesUpdate, expensesDelete = expensesDelete,
            ),
        )

        // Applica id_map: salva i remoteId delle nuove righe (temp_id → id reale).
        resp.idMap.lists.forEach { (temp, remote) -> bindListRemote(temp, remote) }
        resp.idMap.expenses.forEach { (temp, remote) -> bindExpenseRemote(temp, remote) }

        // Azzera pendingSync (snapshot-safe) per le righe inviate non cancellate; hard-delete per i
        // tombstone propagati.
        clearPendingOrHardDelete(lists, expenses)
    }

    private suspend fun listRemoteId(localListId: Long): Long? = dao.getList(localListId)?.remoteId
    private suspend fun listTempIfNew(localListId: Long): String? =
        dao.getList(localListId)?.let { if (it.remoteId == null) "L${it.id}" else null }

    private suspend fun bindListRemote(temp: String, remote: Long) {
        val localId = temp.removePrefix("L").toLongOrNull() ?: return
        dao.getList(localId)?.let { dao.updateList(it.copy(remoteId = remote)) }
    }
    private suspend fun bindExpenseRemote(temp: String, remote: Long) {
        val localId = temp.removePrefix("E").toLongOrNull() ?: return
        dao.getExpense(localId)?.let { dao.updateExpense(it.copy(remoteId = remote)) }
    }

    /** Snapshot-safe: azzera pendingSync solo se updatedAt non è cambiato dopo lo snapshot. */
    private suspend fun clearPendingOrHardDelete(
        lists: List<LocalExpensesListEntity>,
        expenses: List<LocalExpenseEntity>,
    ) {
        lists.forEach { snap ->
            val cur = dao.getList(snap.id) ?: return@forEach
            if (cur.deleted) dao.hardDeleteList(cur.id)
            else if (cur.updatedAt == snap.updatedAt) dao.updateList(cur.copy(pendingSync = false))
        }
        expenses.forEach { snap ->
            val cur = dao.getExpense(snap.id) ?: return@forEach
            if (cur.deleted) dao.hardDeleteExpense(cur.id)
            else if (cur.updatedAt == snap.updatedAt) dao.updateExpense(cur.copy(pendingSync = false))
        }
    }

    // ---------------- PULL ----------------

    private suspend fun pull(userId: Long) {
        val since = cursor.get(SyncCursor.EXPENSES)
        val delta = api.getExpensesDelta(userId, since)
        delta.lists.forEach { applyServerList(it) }
        delta.expenses.forEach { applyServerExpense(it) }
        cursor.set(SyncCursor.EXPENSES, delta.serverTime)
    }

    private fun epoch(instant: Instant?): Long = instant?.toEpochMilli() ?: 0L

    /** La data della spesa arriva come Instant dal backend; localmente è una stringa "yyyy-MM-dd". */
    private fun dateString(instant: Instant?): String? =
        instant?.let { DATE_FMT.format(it.atZone(ZoneId.systemDefault()).toLocalDate()) }

    private suspend fun applyServerList(s: ExpensesList) {
        val local = s.clientId?.let { dao.listByClientId(it) } ?: dao.listByRemoteId(s.id)
        val serverUpdated = epoch(s.updatedAt)
        if (local == null) {
            if (s.deleted) return // niente da creare per una riga già cancellata
            dao.insertListRaw(
                LocalExpensesListEntity(
                    name = s.name, paid = s.paid,
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
            dao.updateList(local.copy(name = s.name, paid = s.paid, remoteId = s.id, updatedAt = serverUpdated))
        } else if (local.remoteId == null) {
            dao.updateList(local.copy(remoteId = s.id))
        }
    }

    private suspend fun applyServerExpense(s: Expense) {
        val local = s.clientId?.let { dao.expenseByClientId(it) } ?: dao.expenseByRemoteId(s.id)
        val serverUpdated = epoch(s.updatedAt)
        val localListId = dao.listIdByRemote(s.expenseListId)
        if (local == null) {
            if (s.deleted || localListId == null) return
            dao.insertExpenseRaw(
                LocalExpenseEntity(
                    listId = localListId, name = s.name, amount = s.amount.toPlainString(),
                    expenseDate = dateString(s.expenseDate), expenseTypeId = s.expenseTypeId,
                    clientId = s.clientId ?: java.util.UUID.randomUUID().toString(),
                    remoteId = s.id, pendingSync = false, deleted = false, updatedAt = serverUpdated,
                ),
            )
            return
        }
        if (s.deleted) { dao.hardDeleteExpense(local.id); return }
        if (!local.pendingSync && serverUpdated >= local.updatedAt) {
            dao.updateExpense(
                local.copy(
                    name = s.name, amount = s.amount.toPlainString(),
                    expenseDate = dateString(s.expenseDate), expenseTypeId = s.expenseTypeId,
                    remoteId = s.id, updatedAt = serverUpdated,
                ),
            )
        } else if (local.remoteId == null) {
            dao.updateExpense(local.copy(remoteId = s.id))
        }
    }

    private companion object {
        private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }
}
