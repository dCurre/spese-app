package com.dcapps.spese.core.sync

import com.dcapps.spese.core.local.dao.ChecklistDao
import com.dcapps.spese.core.local.dao.ExpensesDao
import com.dcapps.spese.core.local.dao.PricesDao
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestisce l'upgrade GUEST → account: cosa fare dei dati creati in locale (checklist, spese, prezzi)
 * quando l'ospite fa login/registrazione.
 *
 * Due strategie (scelta dall'utente, una per tutto):
 * - [merge]: i dati guest diventano dell'account. Azzeriamo i remoteId e marchiamo tutto pendingSync,
 *   poi un ciclo di sync li RICREA sul backend come righe nuove. L'idempotenza via `clientId` evita
 *   doppioni se il sync viene ripetuto. Riusa interamente la macchina di sync — nessun endpoint nuovo.
 * - [discardLocal]: i dati guest locali vengono scartati; il successivo pull ripopola Room con i soli
 *   dati dell'account.
 *
 * In entrambi i casi, dopo la preparazione locale, è il chiamante a lanciare il sync (che non è più
 * no-op perché a quel punto la sessione è loggata).
 */
@Singleton
class GuestMigration @Inject constructor(
    private val checklistDao: ChecklistDao,
    private val expensesDao: ExpensesDao,
    private val pricesDao: PricesDao,
) {
    /** True se esistono dati guest reali (non tombstone) in almeno una sezione → serve la scelta. */
    suspend fun hasLocalData(): Boolean =
        checklistDao.countLocal() > 0 || expensesDao.countLocal() > 0 || pricesDao.countLocal() > 0

    /**
     * "Unisci": prepara i dati guest a essere ricreati sotto l'account. NON fa rete: azzera remoteId e
     * marca pendingSync su tutte le sezioni. Il sync successivo (a sessione loggata) li invia come nuovi.
     */
    suspend fun merge() {
        checklistDao.resetListsForMigration()
        checklistDao.resetCategoriesForMigration()
        checklistDao.resetItemsForMigration()
        expensesDao.resetListsForMigration()
        expensesDao.resetExpensesForMigration()
        pricesDao.resetProductsForMigration()
        pricesDao.resetPurchasesForMigration()
    }

    /** "Solo online": scarta tutti i dati guest locali. Il pull successivo ripopola dall'account. */
    suspend fun discardLocal() {
        checklistDao.wipeItems()
        checklistDao.wipeCategories()
        checklistDao.wipeLists()
        expensesDao.wipeExpenses()
        expensesDao.wipeLists()
        pricesDao.wipePurchases()
        pricesDao.wipeProducts()
    }
}
