package com.dcapps.spese.core.model

import com.dcapps.spese.core.network.serialization.InstantSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

/** Articolo di una checklist. */
@Serializable
data class ShoppingItem(
    val id: Long,
    @SerialName("shopping_list_id") val shoppingListId: Long,
    @SerialName("category_id") val categoryId: Long? = null,
    val name: String = "",
    val quantity: Int? = null,
    val checked: Boolean = false,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("created_at") @Serializable(InstantSerializer::class) val createdAt: Instant? = null,
    // Campi delta sync (backend local-first).
    @SerialName("client_id") val clientId: String? = null,
    @SerialName("updated_at") @Serializable(InstantSerializer::class) val updatedAt: Instant? = null,
    val deleted: Boolean = false,
    // Solo locale: true se la riga ha modifiche non ancora inviate al backend (badge "in attesa").
    @kotlinx.serialization.Transient val pendingSync: Boolean = false,
)

/** Categoria (albero: può avere sottocategorie e item). */
@Serializable
data class ShoppingCategory(
    val id: Long,
    @SerialName("shopping_list_id") val shoppingListId: Long,
    @SerialName("parent_id") val parentId: Long? = null,
    val name: String = "",
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("created_at") @Serializable(InstantSerializer::class) val createdAt: Instant? = null,
    @SerialName("client_id") val clientId: String? = null,
    @SerialName("updated_at") @Serializable(InstantSerializer::class) val updatedAt: Instant? = null,
    val deleted: Boolean = false,
    val items: List<ShoppingItem> = emptyList(),
    val children: List<ShoppingCategory> = emptyList(),
)

/** Partecipante a una checklist. */
@Serializable
data class ShoppingListParticipant(
    @SerialName("user_id") val userId: Long,
    @SerialName("shopping_list_id") val shoppingListId: Long? = null,
    val name: String? = null,
    val surname: String? = null,
    val email: String? = null,
    @SerialName("profile_image") val profileImage: String? = null,
    @SerialName("joined_at") @Serializable(InstantSerializer::class) val joinedAt: Instant? = null,
)

/** Checklist / lista della spesa. */
@Serializable
data class ShoppingList(
    val id: Long,
    val name: String = "",
    @SerialName("owner_id") val ownerId: Long,
    @SerialName("list_type") val listType: String = "personal",
    val completed: Boolean = false,
    val starred: Boolean = false,
    @SerialName("invite_token") val inviteToken: String? = null,
    @SerialName("created_at") @Serializable(InstantSerializer::class) val createdAt: Instant? = null,
    @SerialName("items_count") val itemsCount: Int? = null,
    @SerialName("checked_count") val checkedCount: Int? = null,
    @SerialName("participants_count") val participantsCount: Int? = null,
    @SerialName("client_id") val clientId: String? = null,
    @SerialName("updated_at") @Serializable(InstantSerializer::class) val updatedAt: Instant? = null,
    val deleted: Boolean = false,
    // Solo locale: true se la lista ha modifiche non ancora inviate al backend (badge "in attesa").
    @kotlinx.serialization.Transient val pendingSync: Boolean = false,
    val participants: List<ShoppingListParticipant> = emptyList(),
    val categories: List<ShoppingCategory> = emptyList(),
    val items: List<ShoppingItem> = emptyList(),
)

@Serializable
data class ShoppingListsResponse(
    @SerialName("shopping_lists") val shoppingLists: List<ShoppingList> = emptyList(),
)

// --- Request bodies ---

@Serializable
data class CreateShoppingListRequest(
    val name: String,
    @SerialName("owner_id") val ownerId: Long,
    @SerialName("list_type") val listType: String = "personal",
    val completed: Boolean = false,
)

@Serializable
data class UpdateShoppingListRequest(
    val name: String? = null,
    val completed: Boolean? = null,
    @SerialName("list_type") val listType: String? = null,
    val starred: Boolean? = null,
    // Cambio tipo (issue #11): obbligatorio insieme a listType (il backend valida l'owner).
    @SerialName("requester_id") val requesterId: Long? = null,
)

@Serializable
data class InviteTokenResponse(@SerialName("invite_token") val inviteToken: String)

@Serializable
data class JoinRequest(@SerialName("user_id") val userId: Long)

@Serializable
data class TransferShoppingOwnershipRequest(
    @SerialName("new_owner_id") val newOwnerId: Long,
    @SerialName("old_owner_id") val oldOwnerId: Long,
)

@Serializable
data class CreateShoppingItemRequest(
    @SerialName("shopping_list_id") val shoppingListId: Long,
    val name: String,
    val quantity: Int = 1,
    val checked: Boolean = false,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("category_id") val categoryId: Long? = null,
)

@Serializable
data class UpdateShoppingItemRequest(
    val name: String? = null,
    val quantity: Int? = null,
    val checked: Boolean? = null,
    @SerialName("sort_order") val sortOrder: Int? = null,
    @SerialName("category_id") val categoryId: Long? = null,
)

@Serializable
data class CheckRequest(val checked: Boolean? = null)

@Serializable
data class CheckResponse(val message: String? = null, val checked: Boolean)

@Serializable
data class CheckAllRequest(
    @SerialName("shopping_list_id") val shoppingListId: Long,
    val checked: Boolean = true,
    @SerialName("uncategorized_only") val uncategorizedOnly: Boolean = false,
)

@Serializable
data class ReorderRequest(
    @SerialName("shopping_list_id") val shoppingListId: Long,
    @SerialName("ordered_ids") val orderedIds: List<Long>,
)

@Serializable
data class BulkItemUpdate(
    val id: Long,
    val name: String? = null,
    val quantity: Int? = null,
    val checked: Boolean? = null,
)

@Serializable
data class BulkItemsRequest(
    val updates: List<BulkItemUpdate> = emptyList(),
    @SerialName("delete_ids") val deleteIds: List<Long> = emptyList(),
)

@Serializable
data class CreateShoppingCategoryRequest(
    @SerialName("shopping_list_id") val shoppingListId: Long,
    val name: String,
    @SerialName("parent_id") val parentId: Long? = null,
    @SerialName("sort_order") val sortOrder: Int = 0,
)

@Serializable
data class UpdateShoppingCategoryRequest(
    val name: String? = null,
    @SerialName("sort_order") val sortOrder: Int? = null,
    @SerialName("parent_id") val parentId: Long? = null,
)

// --- Batch save (modifica massiva dell'albero) ---

@Serializable
data class BatchCategoryUpdate(val id: Long, val name: String? = null)

@Serializable
data class BatchCategoryCreate(
    @SerialName("temp_id") val tempId: String,
    val name: String,
    // parent_id può essere null, un id numerico (Long) o un temp_id (String):
    // lo serializziamo come stringa quando riferisce un temp_id. Qui lo esponiamo come
    // JsonElement per supportare entrambe le forme senza perdere informazione.
    @SerialName("parent_id") val parentId: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 0,
)

@Serializable
data class BatchItemUpdate(
    val id: Long,
    val name: String? = null,
    val quantity: Int? = null,
    val checked: Boolean? = null,
    @SerialName("category_id") val categoryId: Long? = null,
)

@Serializable
data class BatchItemCreate(
    val name: String,
    val quantity: Int = 1,
    val checked: Boolean = false,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("category_id") val categoryId: Long? = null,
    @SerialName("category_temp_id") val categoryTempId: String? = null,
)

@Serializable
data class BatchSaveRequest(
    @SerialName("categories_update") val categoriesUpdate: List<BatchCategoryUpdate> = emptyList(),
    @SerialName("categories_delete") val categoriesDelete: List<Long> = emptyList(),
    @SerialName("categories_create") val categoriesCreate: List<BatchCategoryCreate> = emptyList(),
    @SerialName("items_update") val itemsUpdate: List<BatchItemUpdate> = emptyList(),
    @SerialName("items_delete") val itemsDelete: List<Long> = emptyList(),
    @SerialName("items_create") val itemsCreate: List<BatchItemCreate> = emptyList(),
)

// --- Delta sync (local-first) ---

/** Risposta di GET /api/shopping-lists/delta: entità modificate dopo `since` (incluse deleted). */
@Serializable
data class DeltaResponse(
    @SerialName("server_time") val serverTime: String,
    val lists: List<ShoppingList> = emptyList(),
    val categories: List<ShoppingCategory> = emptyList(),
    val items: List<ShoppingItem> = emptyList(),
)

// --- Push: batch-sync (create/update/delete misti cross-lista in una transazione) ---

/** Blocco "create lista" per il batch-sync: temp_id referenzia la nuova lista dai figli. */
@Serializable
data class SyncListCreate(
    @SerialName("temp_id") val tempId: String,
    @SerialName("client_id") val clientId: String,
    val name: String,
    @SerialName("list_type") val listType: String = "personal",
    val completed: Boolean = false,
    val starred: Boolean = false,
)

@Serializable
data class SyncListUpdate(
    val id: Long,
    val name: String? = null,
    val completed: Boolean? = null,
    val starred: Boolean? = null,
)

@Serializable
data class SyncCategoryCreate(
    @SerialName("temp_id") val tempId: String,
    @SerialName("client_id") val clientId: String,
    // list_id o list_temp_id (uno dei due): la lista può essere nuova (temp) o esistente.
    @SerialName("list_id") val listId: Long? = null,
    @SerialName("list_temp_id") val listTempId: String? = null,
    @SerialName("parent_id") val parentId: Long? = null,
    @SerialName("parent_temp_id") val parentTempId: String? = null,
    val name: String,
    @SerialName("sort_order") val sortOrder: Int = 0,
)

@Serializable
data class SyncCategoryUpdate(
    val id: Long,
    val name: String? = null,
    @SerialName("parent_id") val parentId: Long? = null,
    @SerialName("sort_order") val sortOrder: Int? = null,
)

@Serializable
data class SyncItemCreate(
    @SerialName("temp_id") val tempId: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("list_id") val listId: Long? = null,
    @SerialName("list_temp_id") val listTempId: String? = null,
    @SerialName("category_id") val categoryId: Long? = null,
    @SerialName("category_temp_id") val categoryTempId: String? = null,
    val name: String,
    val quantity: Int = 1,
    val checked: Boolean = false,
    @SerialName("sort_order") val sortOrder: Int = 0,
)

@Serializable
data class SyncItemUpdate(
    val id: Long,
    val name: String? = null,
    val quantity: Int? = null,
    val checked: Boolean? = null,
    @SerialName("category_id") val categoryId: Long? = null,
)

@Serializable
data class BatchSyncRequest(
    @SerialName("lists_create") val listsCreate: List<SyncListCreate> = emptyList(),
    @SerialName("lists_update") val listsUpdate: List<SyncListUpdate> = emptyList(),
    @SerialName("lists_delete") val listsDelete: List<Long> = emptyList(),
    @SerialName("categories_create") val categoriesCreate: List<SyncCategoryCreate> = emptyList(),
    @SerialName("categories_update") val categoriesUpdate: List<SyncCategoryUpdate> = emptyList(),
    @SerialName("categories_delete") val categoriesDelete: List<Long> = emptyList(),
    @SerialName("items_create") val itemsCreate: List<SyncItemCreate> = emptyList(),
    @SerialName("items_update") val itemsUpdate: List<SyncItemUpdate> = emptyList(),
    @SerialName("items_delete") val itemsDelete: List<Long> = emptyList(),
)

/** Mappa temp_id → id reale per ciascuna entità, restituita dal batch-sync. */
@Serializable
data class SyncIdMap(
    val lists: Map<String, Long> = emptyMap(),
    val categories: Map<String, Long> = emptyMap(),
    val items: Map<String, Long> = emptyMap(),
)

@Serializable
data class BatchSyncResponse(
    @SerialName("server_time") val serverTime: String,
    @SerialName("id_map") val idMap: SyncIdMap = SyncIdMap(),
)
