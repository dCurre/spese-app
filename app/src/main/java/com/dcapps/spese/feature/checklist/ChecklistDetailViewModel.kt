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
import com.dcapps.spese.core.realtime.longField
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Riga appiattita dell'albero checklist, per il rendering in una lista lineare. */
sealed interface ChecklistRow {
    data class CategoryHeader(
        val category: ShoppingCategory,
        val depth: Int,
        val collapsed: Boolean = false,
    ) : ChecklistRow
    data class Item(val item: ShoppingItem, val depth: Int) : ChecklistRow
}

/** Criterio di ordinamento degli item dentro ogni categoria. */
enum class ChecklistItemSort { ORDER, NAME, CHECKED }

data class ChecklistDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val list: ShoppingList? = null,
    val rows: List<ChecklistRow> = emptyList(),
    val categories: List<ShoppingCategory> = emptyList(),
    // Ricerca e ordinamento (applicati mantenendo la struttura ad albero).
    val query: String = "",
    val sort: ChecklistItemSort = ChecklistItemSort.ORDER,
    val sortAsc: Boolean = true,
    // true se TUTTE le categorie sono collassate: guida l'icona toggle comprimi/espandi.
    val allCollapsed: Boolean = false,
    // true se esiste almeno una categoria (il toggle ha senso solo allora).
    val hasCategories: Boolean = false,
    val inviteToken: String? = null,
    // Modalità modifica: se non null, l'utente sta editando (le modifiche sono locali
    // finché non salva con batch-save).
    val editState: ChecklistEditState? = null,
    val saving: Boolean = false,
    val message: String? = null,
    // Impostato dopo l'eliminazione della lista: la UI torna indietro.
    val deleted: Boolean = false,
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

    /** Id delle categorie collassate (contenuti nascosti). */
    private val collapsed = mutableSetOf<Long>()

    init {
        load(showLoading = true)
        viewModelScope.launch {
            realtimeService.watch(RealtimeTable.SHOPPING_ITEMS).collect { action ->
                if (action.longField("shopping_list_id") == listId) load()
            }
        }
        viewModelScope.launch {
            realtimeService.watch(RealtimeTable.SHOPPING_CATEGORIES).collect { action ->
                if (action.longField("shopping_list_id") == listId) load()
            }
        }
    }

    /**
     * Ricarica la lista. [showLoading] = true mostra lo spinner a tutta pagina (solo primo
     * caricamento / retry); i reload dopo una mutazione o da realtime usano false ("reload
     * silenzioso": il contenuto resta a schermo, niente lampeggio né doppio refresh).
     */
    fun load(showLoading: Boolean = false) {
        if (showLoading) _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = shoppingRepository.getList(listId)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            list = result.data,
                            rows = flatten(result.data),
                            categories = flattenCategories(result.data),
                            allCollapsed = computeAllCollapsed(result.data),
                            hasCategories = result.data.categories.isNotEmpty(),
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

    /** Modifica nome, quantità e categoria di un articolo (category_id inviato esplicito). */
    fun updateItem(item: ShoppingItem, name: String, quantity: Int, categoryId: Long?) {
        viewModelScope.launch {
            val r = itemRepository.updateWithCategory(item.id, name.trim(), quantity, categoryId)
            if (r is ApiResult.Error) _uiState.update { it.copy(message = r.message) }
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

    /** Modifica nome e categoria padre. parentId null = categoria principale (radice). */
    fun updateCategory(category: ShoppingCategory, name: String, parentId: Long?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val r = itemRepository.updateCategoryWithParent(category.id, name.trim(), parentId)
            if (r is ApiResult.Error) _uiState.update { it.copy(message = r.message) }
            load()
        }
    }

    /** Tutti i discendenti (ricorsivi) di una categoria, per impedire cicli nello spostamento. */
    fun descendantIds(categoryId: Long): Set<Long> {
        val list = _uiState.value.list ?: return emptySet()
        fun find(cats: List<ShoppingCategory>): ShoppingCategory? {
            cats.forEach { c -> if (c.id == categoryId) return c; find(c.children)?.let { return it } }
            return null
        }
        val target = find(list.categories) ?: return emptySet()
        fun collect(c: ShoppingCategory): List<Long> =
            listOf(c.id) + c.children.flatMap { collect(it) }
        return collect(target).toSet()
    }

    fun deleteCategory(category: ShoppingCategory) {
        viewModelScope.launch {
            itemRepository.deleteCategory(category.id)
            load()
        }
    }

    /** Elimina l'intera checklist; al successo imposta [deleted] così la UI torna indietro. */
    fun deleteList() {
        viewModelScope.launch {
            when (val result = shoppingRepository.delete(listId)) {
                is ApiResult.Success -> _uiState.update { it.copy(deleted = true) }
                is ApiResult.Error -> _uiState.update { it.copy(message = result.message) }
            }
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

    /** Testo esportabile della checklist: categorie come intestazioni, item come righe "- ...". */
    fun buildExportText(): String {
        val list = _uiState.value.list ?: return ""
        val sb = StringBuilder()
        sb.appendLine(list.name)
        list.items.sortedBy { it.sortOrder }.forEach { i ->
            sb.appendLine("- ${i.name}${if ((i.quantity ?: 1) > 1) " x${i.quantity}" else ""}")
        }
        fun walk(cats: List<ShoppingCategory>, depth: Int) {
            cats.sortedBy { it.sortOrder }.forEach { c ->
                sb.appendLine("${"  ".repeat(depth)}${c.name}:")
                c.items.sortedBy { it.sortOrder }.forEach { i ->
                    sb.appendLine("${"  ".repeat(depth + 1)}- ${i.name}${if ((i.quantity ?: 1) > 1) " x${i.quantity}" else ""}")
                }
                walk(c.children, depth + 1)
            }
        }
        walk(list.categories, 0)
        return sb.toString().trimEnd()
    }

    /**
     * Importa articoli da testo. Riconosce:
     *  - riga che termina con ":" → categoria (gli articoli seguenti le vengono assegnati);
     *  - marcatori di spunta: Samsung Notes `[v]`/`[x]`/`[X]` (spuntato), `[ ]` (non spuntato);
     *    export app `✓` (spuntato), `○` (non spuntato); trattino `-` o testo semplice;
     *  - quantità opzionale in coda come " x3" o " ×3".
     * Salva tutto in un'unica chiamata batch-save.
     */
    fun importFromText(text: String) {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return
        var tempId = 1L
        val catsCreate = mutableListOf<com.dcapps.spese.core.model.BatchCategoryCreate>()
        val itemsCreate = mutableListOf<com.dcapps.spese.core.model.BatchItemCreate>()
        var currentCatTemp: String? = null
        // Marcatori di spunta a inizio riga.
        val checkedPrefixes = listOf("[v]", "[x]", "[X]", "✓", "☑", "[✓]")
        val uncheckedPrefixes = listOf("[ ]", "○", "☐", "-", "[]")

        lines.forEach { raw ->
            if (raw.endsWith(":")) {
                val name = raw.dropLast(1).trim()
                if (name.isNotEmpty()) {
                    val t = "t${tempId++}"
                    catsCreate += com.dcapps.spese.core.model.BatchCategoryCreate(tempId = t, name = name)
                    currentCatTemp = t
                }
            } else {
                var checked = false
                var body = raw
                checkedPrefixes.firstOrNull { body.startsWith(it) }?.let {
                    checked = true; body = body.removePrefix(it).trim()
                }
                if (!checked) {
                    uncheckedPrefixes.firstOrNull { body.startsWith(it) }?.let {
                        body = body.removePrefix(it).trim()
                    }
                }
                // Quantità opzionale in coda: "Mele x3" / "Mele ×3".
                var quantity = 1
                Regex("\\s[x×](\\d+)$").find(body)?.let { m ->
                    quantity = m.groupValues[1].toIntOrNull()?.coerceAtLeast(1) ?: 1
                    body = body.removeRange(m.range).trim()
                }
                val name = body.trim()
                if (name.isNotEmpty()) {
                    itemsCreate += com.dcapps.spese.core.model.BatchItemCreate(
                        name = name,
                        quantity = quantity,
                        checked = checked,
                        categoryTempId = currentCatTemp,
                    )
                }
            }
        }
        if (catsCreate.isEmpty() && itemsCreate.isEmpty()) return
        viewModelScope.launch {
            val req = com.dcapps.spese.core.model.BatchSaveRequest(
                categoriesCreate = catsCreate,
                itemsCreate = itemsCreate,
            )
            when (val r = shoppingRepository.batchSave(listId, req)) {
                is ApiResult.Success -> load()
                is ApiResult.Error -> _uiState.update { it.copy(message = r.message) }
            }
        }
    }

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

    /** Comparator per gli item dentro una categoria, secondo [sort]/[sortAsc]. */
    private fun itemComparator(): Comparator<ShoppingItem> {
        val base: Comparator<ShoppingItem> = when (_uiState.value.sort) {
            ChecklistItemSort.ORDER -> compareBy { it.sortOrder }
            ChecklistItemSort.NAME -> compareBy { it.name.lowercase() }
            // Spuntati per ultimi (false prima di true); a parità, ordine originale.
            ChecklistItemSort.CHECKED -> compareBy<ShoppingItem> { it.checked }.thenBy { it.sortOrder }
        }
        return if (_uiState.value.sortAsc) base else base.reversed()
    }

    private fun matchesQuery(item: ShoppingItem, q: String): Boolean =
        q.isBlank() || item.name.contains(q.trim(), ignoreCase = true)

    /**
     * Appiattisce l'albero in righe applicando ricerca e ordinamento:
     *  - gli item sono ordinati dentro ciascuna categoria secondo [itemComparator];
     *  - con una query attiva, si mostrano solo gli item corrispondenti e le categorie che
     *    (anche nei discendenti) ne contengono almeno uno; le altre categorie spariscono.
     */
    private fun flatten(list: ShoppingList): List<ChecklistRow> {
        val q = _uiState.value.query
        val cmp = itemComparator()
        val rows = mutableListOf<ChecklistRow>()
        // Item senza categoria, in cima.
        list.items.filter { matchesQuery(it, q) }.sortedWith(cmp).forEach { rows += ChecklistRow.Item(it, 0) }
        list.categories.sortedBy { it.sortOrder }.forEach { cat -> appendCategory(cat, 0, rows, q, cmp) }
        return rows
    }

    /** True se la categoria o un suo discendente contiene almeno un item che matcha la query. */
    private fun categoryHasMatch(cat: ShoppingCategory, q: String): Boolean =
        cat.items.any { matchesQuery(it, q) } || cat.children.any { categoryHasMatch(it, q) }

    private fun appendCategory(
        cat: ShoppingCategory,
        depth: Int,
        out: MutableList<ChecklistRow>,
        q: String,
        cmp: Comparator<ShoppingItem>,
    ) {
        // In ricerca, salta le categorie che non contengono nulla di corrispondente.
        if (q.isNotBlank() && !categoryHasMatch(cat, q)) return

        val isCollapsed = cat.id in collapsed
        out += ChecklistRow.CategoryHeader(cat, depth, collapsed = isCollapsed)
        // In ricerca ignoriamo il collasso (si vogliono vedere i risultati).
        if (isCollapsed && q.isBlank()) return
        cat.items.filter { matchesQuery(it, q) }.sortedWith(cmp).forEach { out += ChecklistRow.Item(it, depth + 1) }
        cat.children.sortedBy { it.sortOrder }.forEach { child -> appendCategory(child, depth + 1, out, q, cmp) }
    }

    fun onQueryChange(q: String) {
        _uiState.update { it.copy(query = q) }
        _uiState.value.list?.let { list -> _uiState.update { it.copy(rows = flatten(list)) } }
    }

    fun onSortChange(sort: ChecklistItemSort) {
        _uiState.update { s ->
            // Ritoccando il criterio già attivo si inverte la direzione.
            val asc = if (s.sort == sort) !s.sortAsc else true
            s.copy(sort = sort, sortAsc = asc)
        }
        _uiState.value.list?.let { list -> _uiState.update { it.copy(rows = flatten(list)) } }
    }

    /** Collassa/espande una categoria (solo stato locale, ricalcola le righe). */
    fun toggleCategoryCollapsed(categoryId: Long) {
        if (categoryId in collapsed) collapsed.remove(categoryId) else collapsed.add(categoryId)
        val list = _uiState.value.list ?: return
        _uiState.update { it.copy(rows = flatten(list), allCollapsed = computeAllCollapsed(list)) }
    }

    /** Collassa o espande tutte le categorie. */
    fun setAllCollapsed(collapseAll: Boolean) {
        val list = _uiState.value.list ?: return
        collapsed.clear()
        if (collapseAll) collapsed.addAll(allCategoryIds(list.categories))
        _uiState.update { it.copy(rows = flatten(list), allCollapsed = computeAllCollapsed(list)) }
    }

    /** Toggle unico comprimi/espandi tutto: se tutto è già collassato espande, altrimenti collassa. */
    fun toggleCollapseAll() = setAllCollapsed(!_uiState.value.allCollapsed)

    private fun allCategoryIds(cats: List<ShoppingCategory>): List<Long> =
        cats.flatMap { listOf(it.id) + allCategoryIds(it.children) }

    /** true se tutte le categorie esistenti sono attualmente collassate. */
    private fun computeAllCollapsed(list: ShoppingList): Boolean {
        val all = allCategoryIds(list.categories)
        return all.isNotEmpty() && collapsed.containsAll(all)
    }
}
