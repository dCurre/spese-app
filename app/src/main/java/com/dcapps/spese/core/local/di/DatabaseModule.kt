package com.dcapps.spese.core.local.di

import android.content.Context
import androidx.room.Room
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

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SpeseDatabase =
        Room.databaseBuilder(context, SpeseDatabase::class.java, "spese_guest.db")
            // In sviluppo: ricrea il DB al cambio schema (nessun dato in produzione da migrare).
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideChecklistDao(db: SpeseDatabase): ChecklistDao = db.checklistDao()

    @Provides
    fun provideExpensesDao(db: SpeseDatabase): ExpensesDao = db.expensesDao()

    @Provides
    fun providePricesDao(db: SpeseDatabase): PricesDao = db.pricesDao()
}
