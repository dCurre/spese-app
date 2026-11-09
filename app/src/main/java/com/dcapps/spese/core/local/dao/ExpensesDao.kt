package com.dcapps.spese.core.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.dcapps.spese.core.local.entity.LocalExpenseEntity
import com.dcapps.spese.core.local.entity.LocalExpensesListEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO Room per le SPESE local-first. Le letture per la UI escludono i tombstone (deleted=1) e sono
 * esposte come Flow (reattive). I metodi one-shot/sync ricalcano lo schema del ChecklistDao.
 */
@Dao
interface ExpensesDao {

    // --- Liste spese (letture reattive, escludono i cancellati) ---
    @Query("SELECT * FROM local_expenses_list WHERE deleted = 0 ORDER BY createdAt DESC")
    fun observeLists(): Flow<List<LocalExpensesListEntity>>

    @Query("SELECT * FROM local_expenses_list WHERE id = :id AND deleted = 0")
    fun observeList(id: Long): Flow<LocalExpensesListEntity?>

    @Query("SELECT * FROM local_expenses_list WHERE id = :id")
    suspend fun getList(id: Long): LocalExpensesListEntity?

    @Insert
    suspend fun insertList(list: LocalExpensesListEntity): Long

    @Update
    suspend fun updateList(list: LocalExpensesListEntity)

    // --- Spese ---
    @Query("SELECT * FROM local_expense WHERE listId = :listId AND deleted = 0 ORDER BY createdAt DESC")
    fun observeExpenses(listId: Long): Flow<List<LocalExpenseEntity>>

    @Query("SELECT * FROM local_expense WHERE listId = :listId AND deleted = 0 ORDER BY createdAt DESC")
    suspend fun getExpenses(listId: Long): List<LocalExpenseEntity>

    @Query("SELECT * FROM local_expense WHERE id = :id")
    suspend fun getExpense(id: Long): LocalExpenseEntity?

    @Insert
    suspend fun insertExpense(expense: LocalExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: LocalExpenseEntity)

    // --- Conteggi (escludono i cancellati) ---
    @Query("SELECT COUNT(*) FROM local_expense WHERE listId = :listId AND deleted = 0")
    suspend fun countExpenses(listId: Long): Int

    /** Segnale reattivo: emette a ogni cambiamento sulle spese (per rinfrescare i conteggi in lista). */
    @Query("SELECT * FROM local_expense")
    fun observeAllExpenses(): Flow<List<LocalExpenseEntity>>

    // --- Sync (snapshot one-shot di ciò che è da inviare / applicare) ---

    @Query("SELECT * FROM local_expenses_list WHERE pendingSync = 1")
    suspend fun pendingLists(): List<LocalExpensesListEntity>

    @Query("SELECT * FROM local_expense WHERE pendingSync = 1")
    suspend fun pendingExpenses(): List<LocalExpenseEntity>

    @Query("SELECT * FROM local_expenses_list WHERE remoteId = :remoteId LIMIT 1")
    suspend fun listByRemoteId(remoteId: Long): LocalExpensesListEntity?

    @Query("SELECT * FROM local_expenses_list WHERE clientId = :clientId LIMIT 1")
    suspend fun listByClientId(clientId: String): LocalExpensesListEntity?

    @Query("SELECT * FROM local_expense WHERE remoteId = :remoteId LIMIT 1")
    suspend fun expenseByRemoteId(remoteId: Long): LocalExpenseEntity?

    @Query("SELECT * FROM local_expense WHERE clientId = :clientId LIMIT 1")
    suspend fun expenseByClientId(clientId: String): LocalExpenseEntity?

    @Query("SELECT id FROM local_expenses_list WHERE remoteId = :remoteId LIMIT 1")
    suspend fun listIdByRemote(remoteId: Long): Long?

    @Insert
    suspend fun insertListRaw(list: LocalExpensesListEntity): Long

    @Insert
    suspend fun insertExpenseRaw(expense: LocalExpenseEntity): Long

    // Rimozione fisica dei tombstone già propagati al backend.
    @Query("DELETE FROM local_expenses_list WHERE id = :id")
    suspend fun hardDeleteList(id: Long)

    @Query("DELETE FROM local_expense WHERE id = :id")
    suspend fun hardDeleteExpense(id: Long)

    // --- Migrazione guest → account (upgrade) ---

    /** Numero di righe locali reali (non tombstone): decide se c'è qualcosa da migrare. */
    @Query("SELECT COUNT(*) FROM local_expenses_list WHERE deleted = 0")
    suspend fun countLocal(): Int

    /** "Unisci": azzera i remoteId e marca pendingSync → il SyncEngine ricrea le righe sotto l'account. */
    @Query("UPDATE local_expenses_list SET remoteId = NULL, pendingSync = 1 WHERE deleted = 0")
    suspend fun resetListsForMigration()

    @Query("UPDATE local_expense SET remoteId = NULL, pendingSync = 1 WHERE deleted = 0")
    suspend fun resetExpensesForMigration()

    /** "Solo online": scarta tutti i dati guest locali. */
    @Query("DELETE FROM local_expenses_list")
    suspend fun wipeLists()

    @Query("DELETE FROM local_expense")
    suspend fun wipeExpenses()
}
