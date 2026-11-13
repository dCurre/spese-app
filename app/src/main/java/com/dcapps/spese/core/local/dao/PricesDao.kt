package com.dcapps.spese.core.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.dcapps.spese.core.local.entity.LocalProductEntity
import com.dcapps.spese.core.local.entity.LocalPurchaseEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO Room per i PREZZI local-first. Le letture per la UI escludono i tombstone (deleted=1) e sono
 * esposte come Flow (reattive). I metodi one-shot/sync ricalcano lo schema di ExpensesDao.
 */
@Dao
interface PricesDao {

    // --- Prodotti (letture reattive, escludono i cancellati) ---
    @Query("SELECT * FROM local_product WHERE deleted = 0 ORDER BY createdAt DESC")
    fun observeProducts(): Flow<List<LocalProductEntity>>

    @Query("SELECT * FROM local_product WHERE id = :id AND deleted = 0")
    fun observeProduct(id: Long): Flow<LocalProductEntity?>

    @Query("SELECT * FROM local_product WHERE deleted = 0 ORDER BY createdAt DESC")
    suspend fun getProducts(): List<LocalProductEntity>

    @Query("SELECT * FROM local_product WHERE id = :id")
    suspend fun getProduct(id: Long): LocalProductEntity?

    @Insert
    suspend fun insertProduct(product: LocalProductEntity): Long

    @Update
    suspend fun updateProduct(product: LocalProductEntity)

    // --- Acquisti ---
    @Query("SELECT * FROM local_purchase WHERE productId = :productId AND deleted = 0 ORDER BY purchasedAt DESC, createdAt DESC")
    fun observePurchases(productId: Long): Flow<List<LocalPurchaseEntity>>

    @Query("SELECT * FROM local_purchase WHERE productId = :productId AND deleted = 0 ORDER BY purchasedAt DESC, createdAt DESC")
    suspend fun getPurchases(productId: Long): List<LocalPurchaseEntity>

    @Query("SELECT * FROM local_purchase WHERE id = :id")
    suspend fun getPurchase(id: Long): LocalPurchaseEntity?

    @Insert
    suspend fun insertPurchase(purchase: LocalPurchaseEntity): Long

    @Update
    suspend fun updatePurchase(purchase: LocalPurchaseEntity)

    /** Segnale reattivo: emette a ogni cambiamento sugli acquisti (per conteggi/min/storico in lista). */
    @Query("SELECT * FROM local_purchase")
    fun observeAllPurchases(): Flow<List<LocalPurchaseEntity>>

    @Query("SELECT COUNT(*) FROM local_purchase WHERE productId = :productId AND deleted = 0")
    suspend fun countPurchases(productId: Long): Int

    // --- Sync (snapshot one-shot di ciò che è da inviare / applicare) ---

    @Query("SELECT * FROM local_product WHERE pendingSync = 1")
    suspend fun pendingProducts(): List<LocalProductEntity>

    @Query("SELECT * FROM local_purchase WHERE pendingSync = 1")
    suspend fun pendingPurchases(): List<LocalPurchaseEntity>

    @Query("SELECT * FROM local_product WHERE remoteId = :remoteId LIMIT 1")
    suspend fun productByRemoteId(remoteId: Long): LocalProductEntity?

    @Query("SELECT * FROM local_product WHERE clientId = :clientId LIMIT 1")
    suspend fun productByClientId(clientId: String): LocalProductEntity?

    @Query("SELECT * FROM local_purchase WHERE remoteId = :remoteId LIMIT 1")
    suspend fun purchaseByRemoteId(remoteId: Long): LocalPurchaseEntity?

    @Query("SELECT * FROM local_purchase WHERE clientId = :clientId LIMIT 1")
    suspend fun purchaseByClientId(clientId: String): LocalPurchaseEntity?

    @Query("SELECT id FROM local_product WHERE remoteId = :remoteId LIMIT 1")
    suspend fun productIdByRemote(remoteId: Long): Long?

    @Insert
    suspend fun insertProductRaw(product: LocalProductEntity): Long

    @Insert
    suspend fun insertPurchaseRaw(purchase: LocalPurchaseEntity): Long

    // Rimozione fisica dei tombstone già propagati al backend.
    @Query("DELETE FROM local_product WHERE id = :id")
    suspend fun hardDeleteProduct(id: Long)

    @Query("DELETE FROM local_purchase WHERE id = :id")
    suspend fun hardDeletePurchase(id: Long)

    // --- Migrazione guest → account (upgrade) ---

    /** Numero di righe locali reali (non tombstone): decide se c'è qualcosa da migrare. */
    @Query("SELECT COUNT(*) FROM local_product WHERE deleted = 0")
    suspend fun countLocal(): Int

    /** "Unisci": azzera i remoteId e marca pendingSync → il SyncEngine ricrea le righe sotto l'account. */
    @Query("UPDATE local_product SET remoteId = NULL, pendingSync = 1 WHERE deleted = 0")
    suspend fun resetProductsForMigration()

    @Query("UPDATE local_purchase SET remoteId = NULL, pendingSync = 1 WHERE deleted = 0")
    suspend fun resetPurchasesForMigration()

    /** Righe con modifiche locali non ancora sincronizzate (per il controllo sync-then-wipe al logout). */
    @Query("SELECT COUNT(*) FROM local_product WHERE pendingSync = 1")
    suspend fun pendingCount(): Int

    @Query("SELECT COUNT(*) FROM local_purchase WHERE pendingSync = 1")
    suspend fun pendingPurchasesCount(): Int

    /** "Solo online": scarta tutti i dati guest locali. */
    @Query("DELETE FROM local_product")
    suspend fun wipeProducts()

    @Query("DELETE FROM local_purchase")
    suspend fun wipePurchases()
}
