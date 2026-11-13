package com.dcapps.spese.core.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.dcapps.spese.core.local.entity.LocalShoppingCategoryEntity
import com.dcapps.spese.core.local.entity.LocalShoppingItemEntity
import com.dcapps.spese.core.local.entity.LocalShoppingListEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO Room per le checklist local-first. Le letture per la UI escludono i tombstone (deleted=1) e
 * sono esposte come Flow (reattive). L'albero viene ricomposto nel data source dai risultati piatti.
 */
@Dao
interface ChecklistDao {

    // --- Liste (letture reattive, escludono i cancellati) ---
    @Query("SELECT * FROM local_shopping_list WHERE deleted = 0 ORDER BY createdAt DESC")
    fun observeLists(): Flow<List<LocalShoppingListEntity>>

    @Query("SELECT * FROM local_shopping_list WHERE id = :id AND deleted = 0")
    fun observeList(id: Long): Flow<LocalShoppingListEntity?>

    @Query("SELECT * FROM local_shopping_list WHERE id = :id")
    suspend fun getList(id: Long): LocalShoppingListEntity?

    @Insert
    suspend fun insertList(list: LocalShoppingListEntity): Long

    @Update
    suspend fun updateList(list: LocalShoppingListEntity)

    // --- Categorie ---
    @Query("SELECT * FROM local_shopping_category WHERE listId = :listId AND deleted = 0 ORDER BY sortOrder")
    fun observeCategories(listId: Long): Flow<List<LocalShoppingCategoryEntity>>

    @Query("SELECT * FROM local_shopping_category WHERE listId = :listId AND deleted = 0 ORDER BY sortOrder")
    suspend fun getCategories(listId: Long): List<LocalShoppingCategoryEntity>

    @Query("SELECT listId FROM local_shopping_category WHERE id = :id")
    suspend fun listIdOfCategory(id: Long): Long?

    @Insert
    suspend fun insertCategory(category: LocalShoppingCategoryEntity): Long

    @Update
    suspend fun updateCategory(category: LocalShoppingCategoryEntity)

    // --- Articoli ---
    @Query("SELECT * FROM local_shopping_item WHERE listId = :listId AND deleted = 0 ORDER BY sortOrder")
    fun observeItems(listId: Long): Flow<List<LocalShoppingItemEntity>>

    @Query("SELECT * FROM local_shopping_item WHERE listId = :listId AND deleted = 0 ORDER BY sortOrder")
    suspend fun getItems(listId: Long): List<LocalShoppingItemEntity>

    @Query("SELECT * FROM local_shopping_item WHERE id = :id")
    suspend fun getItem(id: Long): LocalShoppingItemEntity?

    @Insert
    suspend fun insertItem(item: LocalShoppingItemEntity): Long

    @Update
    suspend fun updateItem(item: LocalShoppingItemEntity)

    // --- Conteggi (escludono i cancellati) ---
    @Query("SELECT COUNT(*) FROM local_shopping_item WHERE listId = :listId AND deleted = 0")
    suspend fun countItems(listId: Long): Int

    @Query("SELECT COUNT(*) FROM local_shopping_item WHERE listId = :listId AND deleted = 0 AND checked = 1")
    suspend fun countChecked(listId: Long): Int

    /** Segnale reattivo: emette a ogni cambiamento sugli item (per rinfrescare i conteggi in lista). */
    @Query("SELECT * FROM local_shopping_item")
    fun observeAllItems(): Flow<List<LocalShoppingItemEntity>>

    // --- Sync (snapshot one-shot di ciò che è da inviare / applicare) ---

    @Query("SELECT * FROM local_shopping_list WHERE pendingSync = 1")
    suspend fun pendingLists(): List<LocalShoppingListEntity>

    @Query("SELECT * FROM local_shopping_category WHERE pendingSync = 1")
    suspend fun pendingCategories(): List<LocalShoppingCategoryEntity>

    @Query("SELECT * FROM local_shopping_item WHERE pendingSync = 1")
    suspend fun pendingItems(): List<LocalShoppingItemEntity>

    @Query("SELECT * FROM local_shopping_list WHERE remoteId = :remoteId LIMIT 1")
    suspend fun listByRemoteId(remoteId: Long): LocalShoppingListEntity?

    @Query("SELECT * FROM local_shopping_list WHERE clientId = :clientId LIMIT 1")
    suspend fun listByClientId(clientId: String): LocalShoppingListEntity?

    @Query("SELECT * FROM local_shopping_category WHERE remoteId = :remoteId LIMIT 1")
    suspend fun categoryByRemoteId(remoteId: Long): LocalShoppingCategoryEntity?

    @Query("SELECT * FROM local_shopping_category WHERE clientId = :clientId LIMIT 1")
    suspend fun categoryByClientId(clientId: String): LocalShoppingCategoryEntity?

    @Query("SELECT * FROM local_shopping_item WHERE remoteId = :remoteId LIMIT 1")
    suspend fun itemByRemoteId(remoteId: Long): LocalShoppingItemEntity?

    @Query("SELECT * FROM local_shopping_item WHERE clientId = :clientId LIMIT 1")
    suspend fun itemByClientId(clientId: String): LocalShoppingItemEntity?

    @Query("SELECT * FROM local_shopping_category WHERE id = :id")
    suspend fun getCategoryLocal(id: Long): LocalShoppingCategoryEntity?

    @Query("SELECT id FROM local_shopping_list WHERE remoteId = :remoteId LIMIT 1")
    suspend fun listIdByRemote(remoteId: Long): Long?

    @Query("SELECT id FROM local_shopping_category WHERE remoteId = :remoteId LIMIT 1")
    suspend fun categoryIdByRemote(remoteId: Long): Long?

    @Insert
    suspend fun insertListRaw(list: LocalShoppingListEntity): Long

    @Insert
    suspend fun insertCategoryRaw(category: LocalShoppingCategoryEntity): Long

    @Insert
    suspend fun insertItemRaw(item: LocalShoppingItemEntity): Long

    // Rimozione fisica dei tombstone già propagati al backend.
    @Query("DELETE FROM local_shopping_list WHERE id = :id")
    suspend fun hardDeleteList(id: Long)

    @Query("DELETE FROM local_shopping_category WHERE id = :id")
    suspend fun hardDeleteCategory(id: Long)

    @Query("DELETE FROM local_shopping_item WHERE id = :id")
    suspend fun hardDeleteItem(id: Long)

    // --- Migrazione guest → account (upgrade) ---

    /** Numero di righe locali reali (non tombstone): decide se c'è qualcosa da migrare. */
    @Query("SELECT COUNT(*) FROM local_shopping_list WHERE deleted = 0")
    suspend fun countLocal(): Int

    /**
     * "Unisci": prepara i dati guest a diventare dell'account. Azzera i remoteId (erano assegnati da un
     * eventuale sync precedente ma vanno ri-creati sotto il nuovo owner) e marca tutto pendingSync, così
     * il SyncEngine li ricrea come righe nuove (idempotenti via clientId). I tombstone restano fuori.
     */
    @Query("UPDATE local_shopping_list SET remoteId = NULL, pendingSync = 1 WHERE deleted = 0")
    suspend fun resetListsForMigration()

    @Query("UPDATE local_shopping_category SET remoteId = NULL, pendingSync = 1 WHERE deleted = 0")
    suspend fun resetCategoriesForMigration()

    @Query("UPDATE local_shopping_item SET remoteId = NULL, pendingSync = 1 WHERE deleted = 0")
    suspend fun resetItemsForMigration()

    /** Righe con modifiche locali non ancora sincronizzate (per il controllo sync-then-wipe al logout). */
    @Query("SELECT COUNT(*) FROM local_shopping_list WHERE pendingSync = 1")
    suspend fun pendingCount(): Int

    @Query("SELECT COUNT(*) FROM local_shopping_category WHERE pendingSync = 1")
    suspend fun pendingCategoriesCount(): Int

    @Query("SELECT COUNT(*) FROM local_shopping_item WHERE pendingSync = 1")
    suspend fun pendingItemsCount(): Int

    /** "Solo online": scarta tutti i dati guest locali (poi il pull ripopola dall'account). */
    @Query("DELETE FROM local_shopping_list")
    suspend fun wipeLists()

    @Query("DELETE FROM local_shopping_category")
    suspend fun wipeCategories()

    @Query("DELETE FROM local_shopping_item")
    suspend fun wipeItems()
}
