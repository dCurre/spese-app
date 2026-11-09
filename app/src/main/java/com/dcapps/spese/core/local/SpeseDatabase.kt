package com.dcapps.spese.core.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dcapps.spese.core.local.dao.ChecklistDao
import com.dcapps.spese.core.local.dao.ExpensesDao
import com.dcapps.spese.core.local.dao.PricesDao
import com.dcapps.spese.core.local.entity.LocalExpenseEntity
import com.dcapps.spese.core.local.entity.LocalExpensesListEntity
import com.dcapps.spese.core.local.entity.LocalProductEntity
import com.dcapps.spese.core.local.entity.LocalPurchaseEntity
import com.dcapps.spese.core.local.entity.LocalShoppingCategoryEntity
import com.dcapps.spese.core.local.entity.LocalShoppingItemEntity
import com.dcapps.spese.core.local.entity.LocalShoppingListEntity

/**
 * Database Room locale per la modalità GUEST (dati offline senza account). Copre le checklist
 * (Fase 1), le spese e i prezzi (Fase 2, dati local-first; sync backend non ancora disponibile).
 */
@Database(
    entities = [
        LocalShoppingListEntity::class,
        LocalShoppingCategoryEntity::class,
        LocalShoppingItemEntity::class,
        LocalExpensesListEntity::class,
        LocalExpenseEntity::class,
        LocalProductEntity::class,
        LocalPurchaseEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class SpeseDatabase : RoomDatabase() {
    abstract fun checklistDao(): ChecklistDao
    abstract fun expensesDao(): ExpensesDao
    abstract fun pricesDao(): PricesDao
}
