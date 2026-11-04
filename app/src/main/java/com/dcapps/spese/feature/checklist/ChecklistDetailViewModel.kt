package com.dcapps.spese.feature.checklist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dcapps.spese.core.data.ShoppingItemRepository
import com.dcapps.spese.core.data.ShoppingRepository
import com.dcapps.spese.core.model.CreateShoppingCategoryRequest
import com.dcapps.spese.core.model.CreateShoppingItemRequest
import com.dcapps.spese.core.model.ShoppingCategory
import com.dcapps.spese.core.model.ShoppingItem
import com.dcapps.spese.core.model.ShoppingList
import com.dcapps.spese.core.model.UpdateShoppingCategoryRequest
import com.dcapps.spese.core.model.UpdateShoppingItemRequest
import com.dcapps.spese.core.network.ApiResult
import com.dcapps.spese.core.realtime.RealtimeService
import com.dcapps.spese.core.realtime.RealtimeTable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Riga appiattita dell'albero checklist, per il rendering in una lista lineare. */
sealed interface ChecklistRow {
    data class CategoryHeader(val category: ShoppingCategory, val depth: Int) : ChecklistRow
    data class Item(val item: ShoppingItem, val depth: Int) : ChecklistRow
}

data class ChecklistDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val list: ShoppingList? = null,
    val rows: List<ChecklistRow> = emptyList(),
    val categories: List<ShoppingCategory> = emptyList(),
    val inviteToken: String? = null,
    // Modalità modifica: se non null, l'utente sta editando (le modifiche sono locali
    // finché non salva con batch-save).
    val editState: ChecklistEditState? = null,
    val saving: Boolean = false,
    val message: String? = null,
) {
    val isEditing: Boolean get() = editState != null
}

@HiltViewModel
class ChecklistDetailViewModel @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val itemRepository: ShoppingItemRepository,
    private val realtimeService: RealtimeService,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val listId: Long = checkNotNull(savedStateHandle["listId"])

    private val _uiState = MutableStateFlow(ChecklistDetailUiState())
    val uiState: StateFlow<ChecklistDetailUiState> = _uiState.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            realtimeService.watch(RealtimeTable.SHOPPING_ITEMS).collect { load() }
        }
        viewModelScope.launch {
            realtimeService.watch(RealtimeTable.SHOPPING_CATEGORIES).collect { load() }
        }
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = shoppingRepository.getList(listId)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            list = result.data,
                            rows = flatten(result.data),
                            categories = flattenCategories(result.data),
                        )
                    }
                }
                is ApiResult.Error ->
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
            }
        }
    }

    /** Spunta/despunta un item in modo ottimistico, poi conferma col backend. */
    fun toggleItem(item: ShoppingItem) {
        val newChecked = !item.checked
        // Aggiornamento ottimistico della UI.
        _uiState.update { state ->
            state.copy(
                rows = state.rows.map { row ->
                    if (row is ChecklistRow.Item && row.item.id == item.id) {
                        row.copy(item = row.item.copy(checked = newChecked))
                    } else row
                }
            )
        }
        viewModelScope.launch {
            val result = itemRepository.check(item.id, newChecked)
            if (result is ApiResult.Error) {
                _uiState.update { it.copy(message = result.message) }
                load() // ripristina lo stato reale
            }
        }
    }

    fun addItem(name: String, categoryId: Long?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val result = itemRepository.create(
                CreateShoppingItemRequest(
                    shoppingListId = listId,
                    name = name.trim(),
                    categoryId = categoryId,
                )
            )
            when (result) {
                is ApiResult.Success -> load()
                is ApiResult.Error -> _uiState.update { it.copy(message = result.message) }
            }
        }
    }

    fun deleteItem(item: ShoppingItem) {
        viewModelScope.launch {
            itemRepository.delete(item.id)
            load()
        }
    }

    /** Modifica nome, quantità e categoria di un articolo. */
    fun updateItem(item: ShoppingItem, name: String, quantity: Int, categoryId: Long?) {
        viewModelScope.launch {
            itemRepository.update(
                item.id,
                UpdateShoppingItemRequest(
                    name = name.trim(),
                    quantity = quantity,
                    categoryId = categoryId,
                ),
            )
            load()
        }
    }

    fun addCategory(name: String, parentId: Long?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val result = itemRepository.createCategory(
                CreateShoppingCategoryRequest(
                    shoppingListId = listId,
                    name = name.trim(),
                    parentId = parentId,
                )
            )
            when (result) {
                is ApiResult.Success -> load()
                is ApiResult.Error -> _uiState.update { it.copy(message = result.message) }
            }
        }
    }

    fun renameCategory(category: ShoppingCategory, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            itemRepository.updateCategory(category.id, UpdateShoppingCategoryRequest(name = name.trim()))
            load()
        }
    }

    fun deleteCategory(category: ShoppingCategory) {
        viewModelScope.launch {
            itemRepository.deleteCategory(category.id)
            load()
        }
    }

    /** Spunta o despunta tutti gli articoli della lista. */
    fun checkAll(checked: Boolean) {
        viewModelScope.launch {
            itemRepository.checkAll(listId, checked)
            load()
        }
    }

    /** Genera (o recupera) il link di invito della checklist. */
    fun generateInviteToken() {
        viewModelScope.launch {
            when (val result = shoppingRepository.createInviteToken(listId)) {
                is ApiResult.Success -> _uiState.update { it.copy(inviteToken = result.data) }
                is ApiResult.Error -> _uiState.update { it.copy(message = result.message) }
            }
        }
    }

    fun clearInviteToken() = _uiState.update { it.copy(inviteToken = null) }

    // --- Modalità modifica con salvataggio in batch ---

    fun enterEditMode() {
        val list = _uiState.value.list ?: return
        _uiState.update { it.copy(editState = ChecklistEditState.from(list)) }
    }

    fun cancelEdit() = _uiState.update { it.copy(editState = null) }

    /** Applica una trasformazione allo stato di modifica locale (nessuna chiamata di rete). */
    fun editLocal(transform: (ChecklistEditState) -> ChecklistEditState) {
        _uiState.update { s -> s.editState?.let { s.copy(editState = transform(it)) } ?: s }
    }

    /** Salva tutte le modifiche accumulate in un'unica chiamata batch-save. */
    fun saveEdits() {
        val edit = _uiState.value.editState ?: return
        if (!edit.hasChanges) {
            _uiState.update { it.copy(editState = null) }
            return
        }
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            when (val result = shoppingRepository.batchSave(listId, edit.toBatchRequest())) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(saving = false, editState = null) }
                    load()
                }
                is ApiResult.Error ->
                    _uiState.update { it.copy(saving = false, message = result.message) }
            }
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }

    /** Tutte le categorie in lista piatta (per i selettori), con nome indentato per livello. */
    private fun flattenCategories(list: ShoppingList): List<ShoppingCategory> {
        val out = mutableListOf<ShoppingCategory>()
        fun walk(cats: List<ShoppingCategory>) {
            cats.sortedBy { it.sortOrder }.forEach { c ->
                out += c
                walk(c.children)
            }
        }
        walk(list.categories)
        return out
    }

    /** Appiattisce l'albero (item senza categoria, poi categorie ricorsive) in righe. */
    private fun flatten(list: ShoppingList): List<ChecklistRow> {
        val rows = mutableListOf<ChecklistRow>()
        // Item senza categoria, in cima.
        list.items.sortedBy { it.sortOrder }.forEach { rows += ChecklistRow.Item(it, 0) }
        list.categories.sortedBy { it.sortOrder }.forEach { cat -> appendCategory(cat, 0, rows) }
        return rows
    }

    private fun appendCategory(cat: ShoppingCategory, depth: Int, out: MutableList<ChecklistRow>) {
        out += ChecklistRow.CategoryHeader(cat, depth)
        cat.items.sortedBy { it.sortOrder }.forEach { out += ChecklistRow.Item(it, depth + 1) }
        cat.children.sortedBy { it.sortOrder }.forEach { child -> appendCategory(child, depth + 1, out) }
    }
}
