package com.dcapps.spese.core.realtime

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** Tabelle osservabili in realtime (schema PostgreSQL `spese`). */
enum class RealtimeTable(val tableName: String) {
    EXPENSES("expenses"),
    EXPENSES_LISTS("expenses_lists"),
    EXPENSES_LIST_PARTICIPANTS("expenses_list_participants"),
    SHOPPING_LISTS("shopping_lists"),
    SHOPPING_ITEMS("shopping_items"),
    SHOPPING_CATEGORIES("shopping_categories"),
    SHOPPING_LIST_PARTICIPANTS("shopping_list_participants"),
}

/**
 * Espone un [Flow] per tabella che emette ad ogni cambiamento (insert/update/delete).
 * I canali Supabase sono condivisi tra più collector con ref-counting: il canale si apre al
 * primo collector e si chiude quando non ce ne sono più (come il RealtimeService della webapp).
 */
@Singleton
class RealtimeService @Inject constructor(
    private val supabase: SupabaseClient,
) {
    private val scope = CoroutineScope(SupervisorJob())
    private val schema = "spese"

    private class Entry(
        val flow: MutableSharedFlow<PostgresAction>,
        val close: suspend () -> Unit,
        var refs: Int = 0,
    )

    private val entries = mutableMapOf<RealtimeTable, Entry>()
    private val mutex = Mutex()

    /** Osserva i cambiamenti su [table]. Il canale si chiude quando l'ultimo collector smette. */
    fun watch(table: RealtimeTable): Flow<PostgresAction> = flow {
        val entry = acquire(table)
        try {
            emitAll(entry.flow)
        } finally {
            release(table)
        }
    }

    private suspend fun acquire(table: RealtimeTable): Entry = mutex.withLock {
        entries[table]?.let { it.refs++; return it }

        val channel = supabase.channel("$schema:${table.tableName}")
        val shared = MutableSharedFlow<PostgresAction>(extraBufferCapacity = 16)
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = schema) {
            this.table = table.tableName
        }
        val job = scope.launch {
            changes.collect { shared.emit(it) }
        }
        channel.subscribe()

        val entry = Entry(
            flow = shared,
            close = {
                job.cancel()
                runCatching { supabase.realtime.removeChannel(channel) }
            },
            refs = 1,
        )
        entries[table] = entry
        entry
    }

    private suspend fun release(table: RealtimeTable) = mutex.withLock {
        val entry = entries[table] ?: return@withLock
        entry.refs--
        if (entry.refs <= 0) {
            entry.close()
            entries.remove(table)
        }
    }
}
