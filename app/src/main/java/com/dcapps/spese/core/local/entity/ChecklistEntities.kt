package com.dcapps.spese.core.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entità Room local-first per le checklist (valgono per TUTTI gli utenti: la UI legge/scrive sempre
 * qui, il SyncEngine allinea col backend). Sono "piatte" (Room non annida): l'albero
 * categorie/articoli viene ricomposto nel data source.
 *
 * Stato di sincronizzazione comune a ogni riga:
 * - [remoteId]: id assegnato dal backend (null finché non sincronizzata);
 * - [pendingSync]: true se la riga ha modifiche locali da inviare;
 * - [deleted]: tombstone — cancellata localmente, la cancellazione va propagata al backend;
 * - [updatedAt]: timestamp ultima modifica locale (per il last-write-wins).
 */
@Entity(tableName = "local_shopping_list")
data class LocalShoppingListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val completed: Boolean = false,
    val starred: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    // clientId: UUID stabile per la riga, usato dal backend per dedup/idempotenza.
    val clientId: String = java.util.UUID.randomUUID().toString(),
    val remoteId: Long? = null,
    val pendingSync: Boolean = true,
    val deleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "local_shopping_category",
    indices = [Index("listId"), Index("parentId")],
)
data class LocalShoppingCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val parentId: Long? = null,
    val name: String,
    val sortOrder: Int = 0,
    val clientId: String = java.util.UUID.randomUUID().toString(),
    val remoteId: Long? = null,
    val pendingSync: Boolean = true,
    val deleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "local_shopping_item",
    indices = [Index("listId"), Index("categoryId")],
)
data class LocalShoppingItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val categoryId: Long? = null,
    val name: String,
    val quantity: Int = 1,
    val checked: Boolean = false,
    val sortOrder: Int = 0,
    val clientId: String = java.util.UUID.randomUUID().toString(),
    val remoteId: Long? = null,
    val pendingSync: Boolean = true,
    val deleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)
