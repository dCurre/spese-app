package com.dcapps.spese.core.local

import com.dcapps.spese.core.local.dao.ExpensesDao
import com.dcapps.spese.core.local.entity.LocalExpenseEntity
import com.dcapps.spese.core.local.entity.LocalExpensesListEntity
import com.dcapps.spese.core.model.Expense
import com.dcapps.spese.core.model.ExpensesList
import com.dcapps.spese.core.network.ApiResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sorgente dati LOCALE (Room) local-first delle SPESE (liste spese + spese). Le letture per la UI
 * sono Flow reattivi; le scritture marcano `pendingSync`/`updatedAt`; la cancellazione è un tombstone
 * (`deleted = true`), non una DELETE fisica. NIENTE partecipanti: le liste local-first sono sempre
 * personali, con owner fittizio [GUEST_OWNER_ID].
 */
@Singleton
class ExpensesLocalDataSource @Inject constructor(
    private val dao: ExpensesDao,
) {
    private fun now() = System.currentTimeMillis()

    // --- Letture reattive (Flow) ---

    /** Tutte le liste spese con conteggio spese (si rinfresca quando una spesa cambia). */
    fun observeLists(): Flow<List<ExpensesList>> =
        dao.observeLists().combine(dao.observeAllExpenses()) { lists, allExpenses ->
            lists.map { e ->
                val count = allExpenses.count { it.listId == e.id && !it.deleted }
                e.toModel(expensesCount = count)
            }
        }

    /** Una lista spese, reattiva (senza partecipanti: local-first è sempre personale). */
    fun observeList(id: Long): Flow<ExpensesList?> =
        dao.observeList(id).combine(dao.observeAllExpenses()) { listEntity, allExpenses ->
            listEntity?.toModel(expensesCount = allExpenses.count { it.listId == id && !it.deleted })
        }

    /** Le spese di una lista, reattive (escludono i tombstone). */
    fun observeExpenses(listId: Long): Flow<List<Expense>> =
        dao.observeExpenses(listId).map { list -> list.map { it.toModel() } }

    // --- Scritture (marcano pendingSync) ---

    suspend fun createList(
        name: String,
        listType: String = "personal",
        ownerId: Long = GUEST_OWNER_ID,
    ): ApiResult<Long> = guard {
        dao.insertList(
            LocalExpensesListEntity(
                name = name, listType = listType, ownerId = ownerId, pendingSync = true, updatedAt = now(),
            ),
        )
    }

    suspend fun updateList(id: Long, name: String?, paid: Boolean?): ApiResult<Unit> = guard {
        val e = dao.getList(id) ?: error("Lista non trovata")
        dao.updateList(
            e.copy(
                name = name ?: e.name,
                paid = paid ?: e.paid,
                pendingSync = true,
                updatedAt = now(),
            ),
        )
    }

    suspend fun deleteList(id: Long): ApiResult<Unit> = guard { tombstoneList(id) }

    suspend fun deleteLists(ids: List<Long>): ApiResult<Unit> = guard { ids.forEach { tombstoneList(it) } }

    suspend fun addExpense(
        listId: Long,
        name: String,
        amount: BigDecimal,
        expenseDate: String?,
        expenseTypeId: Long?,
    ): ApiResult<Long> = guard {
        dao.insertExpense(
            LocalExpenseEntity(
                listId = listId,
                name = name,
                amount = amount.toPlainString(),
                expenseDate = expenseDate,
                expenseTypeId = expenseTypeId,
                pendingSync = true,
                updatedAt = now(),
            ),
        )
    }

    suspend fun updateExpense(
        id: Long,
        name: String?,
        amount: BigDecimal?,
        expenseDate: String?,
        expenseTypeId: Long?,
    ): ApiResult<Unit> = guard {
        val cur = dao.getExpense(id) ?: error("Spesa non trovata")
        dao.updateExpense(
            cur.copy(
                name = name ?: cur.name,
                amount = amount?.toPlainString() ?: cur.amount,
                expenseDate = expenseDate ?: cur.expenseDate,
                expenseTypeId = expenseTypeId ?: cur.expenseTypeId,
                pendingSync = true,
                updatedAt = now(),
            ),
        )
    }

    /**
     * remoteId (id assegnato dal backend) di una lista spese dato il suo id LOCALE di Room.
     * Serve alle operazioni ONLINE dirette (cambio tipo, transfer owner, partecipanti, balance): il
     * backend conosce solo il remoteId, non l'id locale, che coincidono solo per caso. null se la
     * lista non è ancora stata sincronizzata (remoteId non ancora assegnato) o non esiste.
     */
    suspend fun remoteListId(localId: Long): Long? = dao.listRemoteId(localId)

    suspend fun deleteExpense(id: Long): ApiResult<Unit> = guard { tombstoneExpense(id) }

    suspend fun deleteExpenses(ids: List<Long>): ApiResult<Unit> = guard { ids.forEach { tombstoneExpense(it) } }

    // --- helper tombstone ---

    private suspend fun tombstoneList(id: Long) {
        val e = dao.getList(id) ?: return
        // Mai salita al server: non c'è cancellazione da propagare → hard-delete (niente tombstone
        // inutile in Room, utile soprattutto in guest che non sincronizza mai). Le sue spese non
        // sono mai salite neanche loro → si rimuovono con la lista (niente righe orfane in Room).
        if (e.remoteId == null) {
            dao.hardDeleteExpensesOfList(id)
            dao.hardDeleteList(id)
        } else {
            dao.updateList(e.copy(deleted = true, pendingSync = true, updatedAt = now()))
        }
    }

    private suspend fun tombstoneExpense(id: Long) {
        val e = dao.getExpense(id) ?: return
        if (e.remoteId == null) dao.hardDeleteExpense(id)
        else dao.updateExpense(e.copy(deleted = true, pendingSync = true, updatedAt = now()))
    }
}

private inline fun <T> guard(block: () -> T): ApiResult<T> =
    try {
        ApiResult.Success(block())
    } catch (e: Exception) {
        ApiResult.Error(message = e.message ?: "Errore locale", cause = e)
    }

private fun LocalExpensesListEntity.toModel(expensesCount: Int) = ExpensesList(
    id = id,
    name = name,
    userId = ownerId,
    paid = paid,
    createdAt = Instant.ofEpochMilli(createdAt),
    listType = listType,
    participantsCount = 1,
    expensesCount = expensesCount,
    participants = emptyList(),
    pendingSync = pendingSync,
)

private fun LocalExpenseEntity.toModel() = Expense(
    id = id,
    name = name,
    amount = runCatching { BigDecimal(amount) }.getOrDefault(BigDecimal.ZERO),
    createdAt = Instant.ofEpochMilli(createdAt),
    expenseListId = listId,
    expenseDate = expenseDate?.let { parseDate(it) },
    expenseTypeId = expenseTypeId,
    pendingSync = pendingSync,
)

/** Converte una data semplice "yyyy-MM-dd" in Instant (mezzanotte locale). */
private fun parseDate(value: String): Instant? =
    runCatching { LocalDate.parse(value).atStartOfDay(ZoneId.systemDefault()).toInstant() }.getOrNull()
