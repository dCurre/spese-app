package com.dcapps.spese.core.model

import com.dcapps.spese.core.network.serialization.BigDecimalSerializer
import com.dcapps.spese.core.network.serialization.InstantSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.Instant

/** Tipo di lista spese. */
enum class ListType(val wire: String) {
    SHARED("shared"),
    PERSONAL("personal");

    companion object {
        fun from(value: String?): ListType =
            entries.firstOrNull { it.wire == value } ?: SHARED
    }
}

/** Lista spese (condivisa o personale). */
@Serializable
data class ExpensesList(
    val id: Long,
    val name: String = "",
    @SerialName("user_id") val userId: Long,
    val paid: Boolean = false,
    @SerialName("created_at") @Serializable(InstantSerializer::class) val createdAt: Instant? = null,
    @SerialName("list_type") val listType: String = "shared",
    @SerialName("max_participants") val maxParticipants: Int? = null,
    @SerialName("participants_count") val participantsCount: Int? = null,
    @SerialName("expenses_count") val expensesCount: Int? = null,
    val participants: List<ExpensesListParticipant> = emptyList(),
    // Campi delta sync (backend local-first).
    @SerialName("client_id") val clientId: String? = null,
    @SerialName("updated_at") @Serializable(InstantSerializer::class) val updatedAt: Instant? = null,
    val deleted: Boolean = false,
    // Solo locale: true se la riga ha modifiche non ancora inviate al backend (badge "in attesa").
    @kotlinx.serialization.Transient val pendingSync: Boolean = false,
) {
    val type: ListType get() = ListType.from(listType)
}

@Serializable
data class ExpensesListsResponse(
    @SerialName("expenses_lists") val expensesLists: List<ExpensesList> = emptyList(),
)

/** Partecipante a una lista spese (può essere un ospite senza account). */
@Serializable
data class ExpensesListParticipant(
    @SerialName("user_id") val userId: Long,
    @SerialName("expenses_list_id") val expensesListId: Long? = null,
    @SerialName("joined_at") @Serializable(InstantSerializer::class) val joinedAt: Instant? = null,
    val name: String? = null,
    val surname: String? = null,
    val email: String? = null,
    @SerialName("profile_image") val profileImage: String? = null,
    @SerialName("is_guest") val isGuest: Boolean = false,
)

@Serializable
data class ParticipantsResponse(
    val participants: List<ExpensesListParticipant> = emptyList(),
)

/** Owner "ridotto" annidato dentro una spesa. */
@Serializable
data class ExpenseOwner(
    val id: Long,
    val name: String? = null,
    val surname: String? = null,
    val email: String? = null,
    @SerialName("profile_image") val profileImage: String? = null,
    @SerialName("paid_list_shown") val paidListShown: Boolean? = null,
)

/** Spesa singola. */
@Serializable
data class Expense(
    val id: Long,
    val name: String = "",
    @Serializable(BigDecimalSerializer::class) val amount: BigDecimal = BigDecimal.ZERO,
    @SerialName("created_at") @Serializable(InstantSerializer::class) val createdAt: Instant? = null,
    val owner: ExpenseOwner? = null,
    @SerialName("updated_at") @Serializable(InstantSerializer::class) val updatedAt: Instant? = null,
    @SerialName("modified_by") val modifiedBy: Long? = null,
    @SerialName("expense_list_id") val expenseListId: Long,
    @SerialName("expense_date") @Serializable(InstantSerializer::class) val expenseDate: Instant? = null,
    @SerialName("expense_type_id") val expenseTypeId: Long? = null,
    @SerialName("expense_type") val expenseType: String? = null,
    // Campi delta sync (backend local-first).
    @SerialName("client_id") val clientId: String? = null,
    val deleted: Boolean = false,
    // Solo locale: true se la riga ha modifiche non ancora inviate al backend (badge "in attesa").
    @kotlinx.serialization.Transient val pendingSync: Boolean = false,
)

@Serializable
data class ExpensesResponse(val expenses: List<Expense> = emptyList())

/** Body creazione spesa. amount come stringa (coerente col backend). */
@Serializable
data class CreateExpenseRequest(
    val name: String,
    @Serializable(BigDecimalSerializer::class) val amount: BigDecimal,
    @SerialName("expense_owner_user_id") val expenseOwnerUserId: Long,
    @SerialName("expense_list_id") val expenseListId: Long,
    @SerialName("expense_date") val expenseDate: String,
    @SerialName("expense_type_id") val expenseTypeId: Long? = null,
)

/** Body modifica spesa (parziale). */
@Serializable
data class UpdateExpenseRequest(
    val name: String? = null,
    @Serializable(BigDecimalSerializer::class) val amount: BigDecimal? = null,
    @SerialName("expense_date") val expenseDate: String? = null,
    @SerialName("expense_owner_user_id") val expenseOwnerUserId: Long? = null,
    @SerialName("modified_by") val modifiedBy: Long? = null,
    @SerialName("expense_type_id") val expenseTypeId: Long? = null,
)

/** Body creazione lista spese. */
@Serializable
data class CreateExpensesListRequest(
    val name: String,
    @SerialName("user_id") val userId: Long,
    val paid: Boolean = false,
    @SerialName("list_type") val listType: String = "shared",
)

/** Body modifica lista (parziale). */
@Serializable
data class UpdateExpensesListRequest(
    val name: String? = null,
    val paid: Boolean? = null,
    // Cambio tipo (issue #11): "shared" | "personal". Se presente, requesterId è OBBLIGATORIO
    // (il backend valida che sia l'owner). Passaggio a "personal" è distruttivo.
    @SerialName("list_type") val listType: String? = null,
    @SerialName("requester_id") val requesterId: Long? = null,
)

@Serializable
data class TransferOwnerRequest(
    @SerialName("new_owner_id") val newOwnerId: Long,
    @SerialName("current_owner_id") val currentOwnerId: Long,
)

@Serializable
data class AddParticipantRequest(@SerialName("user_id") val userId: Long)

// --- Saldo / Balance ---

/** Riga "chi deve a chi". toPay > 0 = buyer deve a receiver. */
@Serializable
data class BalanceEntry(
    val buyer: String,
    val receiver: String,
    @SerialName("toPay") val toPay: Double,
)

/** Totale pagato per persona (chiave = "nome cognome"). */
@Serializable
data class BalanceTotal(
    val name: String,
    val amount: Double,
)

@Serializable
data class BalanceResponse(
    val balance: List<BalanceEntry> = emptyList(),
    val totals: List<BalanceTotal> = emptyList(),
)

/** Risposta di GET /users/by-email/{email}/expenses-lists */
@Serializable
data class UserWithExpensesListsResponse(
    val user: User,
    @SerialName("expenses_lists") val expensesLists: List<ExpensesList> = emptyList(),
)

// --- Delta sync (local-first) ---

/** Risposta di GET /api/expenses-lists/delta: entità modificate dopo `since` (incluse deleted). */
@Serializable
data class ExpensesDeltaResponse(
    @SerialName("server_time") val serverTime: String,
    val lists: List<ExpensesList> = emptyList(),
    val expenses: List<Expense> = emptyList(),
)

/** Blocco "create lista spese" per il batch-sync: temp_id referenzia la nuova lista dalle spese. */
@Serializable
data class SyncExpensesListCreate(
    @SerialName("temp_id") val tempId: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("user_id") val userId: Long,
    val name: String,
    @SerialName("list_type") val listType: String = "personal",
    val paid: Boolean = false,
)

@Serializable
data class SyncExpensesListUpdate(
    val id: Long,
    val name: String? = null,
    val paid: Boolean? = null,
)

@Serializable
data class SyncExpenseCreate(
    @SerialName("temp_id") val tempId: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("expense_owner_user_id") val expenseOwnerUserId: Long,
    // list_id o list_temp_id (uno dei due): la lista può essere nuova (temp) o esistente.
    @SerialName("expense_list_id") val listId: Long? = null,
    @SerialName("list_temp_id") val listTempId: String? = null,
    val name: String,
    @Serializable(BigDecimalSerializer::class) val amount: BigDecimal,
    @SerialName("expense_date") val expenseDate: String? = null,
    @SerialName("expense_type_id") val expenseTypeId: Long? = null,
)

@Serializable
data class SyncExpenseUpdate(
    val id: Long,
    val name: String? = null,
    @Serializable(BigDecimalSerializer::class) val amount: BigDecimal? = null,
    @SerialName("expense_date") val expenseDate: String? = null,
    @SerialName("expense_type_id") val expenseTypeId: Long? = null,
)

@Serializable
data class ExpensesBatchSyncRequest(
    @SerialName("lists_create") val listsCreate: List<SyncExpensesListCreate> = emptyList(),
    @SerialName("lists_update") val listsUpdate: List<SyncExpensesListUpdate> = emptyList(),
    @SerialName("lists_delete") val listsDelete: List<Long> = emptyList(),
    @SerialName("expenses_create") val expensesCreate: List<SyncExpenseCreate> = emptyList(),
    @SerialName("expenses_update") val expensesUpdate: List<SyncExpenseUpdate> = emptyList(),
    @SerialName("expenses_delete") val expensesDelete: List<Long> = emptyList(),
)

/** Mappa temp_id → id reale per ciascuna entità, restituita dal batch-sync spese. */
@Serializable
data class ExpensesSyncIdMap(
    val lists: Map<String, Long> = emptyMap(),
    val expenses: Map<String, Long> = emptyMap(),
)

@Serializable
data class ExpensesBatchSyncResponse(
    @SerialName("server_time") val serverTime: String,
    @SerialName("id_map") val idMap: ExpensesSyncIdMap = ExpensesSyncIdMap(),
)
