package com.dcapps.spese.core.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entità Room local-first per i PREZZI (prodotti + acquisti). Stesso schema di sync di checklist e
 * spese: la UI legge/scrive sempre qui; un futuro SyncEngine allineerà col backend (endpoint delta
 * non ancora disponibili → per ora i dati restano solo locali). I prodotti NON hanno condivisione.
 *
 * Stato di sincronizzazione comune a ogni riga:
 * - [clientId]: UUID stabile per la riga, usato dal backend per dedup/idempotenza;
 * - [remoteId]: id assegnato dal backend (null finché non sincronizzata);
 * - [pendingSync]: true se la riga ha modifiche locali da inviare;
 * - [deleted]: tombstone — cancellata localmente, da propagare al backend;
 * - [updatedAt]: timestamp ultima modifica locale (per il last-write-wins).
 */
@Entity(tableName = "local_product")
data class LocalProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val brand: String? = null,
    val defaultUnitId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val clientId: String = java.util.UUID.randomUUID().toString(),
    val remoteId: Long? = null,
    val pendingSync: Boolean = true,
    val deleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "local_purchase",
    indices = [Index("productId")],
)
data class LocalPurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    // data dell'acquisto (yyyy-MM-dd) come stringa, coerente col contratto backend.
    val purchasedAt: String,
    val price: Double = 0.0,
    val quantity: Double = 0.0,
    val unitId: Long? = null,
    val unitSymbol: String? = null,
    // prezzo unitario = price/quantity (0 se quantity non valida); calcolato alla scrittura.
    val unitPrice: Double = 0.0,
    val store: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val clientId: String = java.util.UUID.randomUUID().toString(),
    val remoteId: Long? = null,
    val pendingSync: Boolean = true,
    val deleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)
