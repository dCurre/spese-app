package com.dcapps.spese.core.local.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dcapps.spese.core.local.SpeseDatabase
import com.dcapps.spese.core.local.dao.ChecklistDao
import com.dcapps.spese.core.local.dao.ExpensesDao
import com.dcapps.spese.core.local.dao.PricesDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * v4 → v5: aggiunge la colonna `listType` alle liste (spese + checklist) SENZA ricreare il DB,
     * così i dati local-first dell'utente NON vengono persi all'aggiornamento. Default 'personal'
     * coerente con l'entity. (Prima si usava fallbackToDestructiveMigration, che azzerava il DB.)
     */
    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE local_expenses_list ADD COLUMN listType TEXT NOT NULL DEFAULT 'personal'")
            db.execSQL("ALTER TABLE local_shopping_list ADD COLUMN listType TEXT NOT NULL DEFAULT 'personal'")
        }
    }

    /**
     * v5 → v6: aggiunge `ownerId` alle liste spese (default -1 = GUEST_OWNER_ID). Serve a riconoscere
     * il proprietario reale e mostrare il cambio tipo condivisa/personale solo a lui. Il pull dal
     * server scrive l'owner vero; le righe esistenti partono guest (-1) e si correggono al primo sync.
     */
    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE local_expenses_list ADD COLUMN ownerId INTEGER NOT NULL DEFAULT -1")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SpeseDatabase =
        Room.databaseBuilder(context, SpeseDatabase::class.java, "spese_guest.db")
            .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
            // Rete di sicurezza SOLO per schemi vecchi non coperti da una migration esplicita: i cambi
            // di schema futuri devono aggiungere una Migration qui, NON affidarsi al wipe distruttivo.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideChecklistDao(db: SpeseDatabase): ChecklistDao = db.checklistDao()

    @Provides
    fun provideExpensesDao(db: SpeseDatabase): ExpensesDao = db.expensesDao()

    @Provides
    fun providePricesDao(db: SpeseDatabase): PricesDao = db.pricesDao()
}
