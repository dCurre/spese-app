package com.dcapps.spese.core.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entità Room local-first per le SPESE (liste spese + spese). Stesso schema di sync delle checklist:
 * la UI legge/scrive sempre qui; un futuro SyncEngine allineerà col backend (endpoint delta non
 * ancora disponibili → per ora i dati restano solo locali). NIENTE partecipanti: in local-first
 * (guest) le liste sono sempre personali, senza condivisione.
 *
 * Stato di sincronizzazione comune a ogni riga:
 * - [clientId]: UUID stabile per la riga, usato dal backend per dedup/idempotenza;
 * - [remoteId]: id assegnato dal backend (null finché non sincronizzata);
 * - [pendingSync]: true se la riga ha modifiche locali da inviare;
 * - [deleted]: tombstone — cancellata localmente, da propagare al backend;
 * - [updatedAt]: timestamp ultima modifica locale (per il last-write-wins).
 */
@Entity(tableName = "local_expenses_list")
data class LocalExpensesListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val paid: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val clientId: String = java.util.UUID.randomUUID().toString(),
    val remoteId: Long? = null,
    val pendingSync: Boolean = true,
    val deleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "local_expense",
    indices = [Index("listId")],
)
data class LocalExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val name: String,
    // amount come stringa decimale (es. "50.00") per evitare errori di arrotondamento binario.
    val amount: String = "0",
    // data della spesa (yyyy-MM-dd) come stringa, coerente col contratto backend.
    val expenseDate: String? = null,
    val expenseTypeId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val clientId: String = java.util.UUID.randomUUID().toString(),
    val remoteId: Long? = null,
    val pendingSync: Boolean = true,
    val deleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)
