package com.dcapps.spese.feature.checklist

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dcapps.spese.R
import com.dcapps.spese.core.data.ShoppingItemRepository
import com.dcapps.spese.core.data.ShoppingRepository
import com.dcapps.spese.core.model.BatchCategoryCreate
import com.dcapps.spese.core.model.BatchItemCreate
import com.dcapps.spese.core.model.BatchSaveRequest
import com.dcapps.spese.core.model.CreateShoppingCategoryRequest
import com.dcapps.spese.core.model.CreateShoppingItemRequest
import com.dcapps.spese.core.model.ShoppingCategory
import com.dcapps.spese.core.model.ShoppingItem
import com.dcapps.spese.core.model.ShoppingList
import com.dcapps.spese.core.model.UpdateShoppingCategoryRequest
import com.dcapps.spese.core.model.UpdateShoppingItemRequest
import com.dcapps.spese.core.model.UpdateShoppingListRequest
import com.dcapps.spese.core.network.ApiResult
import com.dcapps.spese.core.prefs.SortPreferences
import com.dcapps.spese.core.auth.SessionManager
import com.dcapps.spese.core.realtime.RealtimeService
import com.dcapps.spese.core.realtime.RealtimeTable
import com.dcapps.spese.core.realtime.longField
import com.dcapps.spese.core.ui.SelectionController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
        // Numero di sottoelementi diretti (sottocategorie + articoli diretti): badge conteggio.
        val directCount: Int = 0,
        // true se la categoria ha articoli e sono TUTTI spuntati (ricorsivo): nome barrato.
        val completed: Boolean = false,
        // true se è l'ultimo figlio del suo gruppo: connettore albero "└─" invece di "├─".
        val isLastChild: Boolean = false,
    ) : ChecklistRow
    data class Item(
        val item: ShoppingItem,
        val depth: Int,
        val isLastChild: Boolean = false,
    ) : ChecklistRow
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
    // false per messaggi informativi (es. articolo già esistente): la snackbar non offre "Segnala".
    val messageReportable: Boolean = true,
    // Impostato dopo l'eliminazione della lista: la UI torna indietro.
    val deleted: Boolean = false,
    // Placeholder passati via nav-arg per mostrare subito titolo e stella corretti prima che
    // getList risponda (evita flicker del nome / stella con stato sbagliato).
    val placeholderName: String? = null,
    val placeholderStarred: Boolean = false,
) {
    val isEditing: Boolean get() = editState != null

    /** Nome da mostrare: quello reale se caricato, altrimenti il placeholder dal nav-arg. */
    val displayName: String? get() = list?.name ?: placeholderName?.takeIf { it.isNotBlank() }

    /** La checklist è condivisa? (issue #11) */
    val isShared: Boolean get() = list?.listType == "shared"

    /** La checklist è nei preferiti? Usa il valore reale se caricato, altrimenti il placeholder. */
    val starred: Boolean get() = list?.starred ?: placeholderStarred

    /** Tutti gli articoli dell'albero (item radice + quelli dentro le categorie, ricorsivo). */
    private fun allItems(): List<ShoppingItem> {
        val l = list ?: return emptyList()
        fun fromCats(cats: List<ShoppingCategory>): List<ShoppingItem> =
            cats.flatMap { it.items + fromCats(it.children) }
        return l.items + fromCats(l.categories)
    }

    /** C'è almeno un articolo (il toggle spunta/despunta ha senso solo allora). */
    val hasItems: Boolean get() = allItems().isNotEmpty()

    /** C'è almeno un articolo e sono tutti spuntati (per l'icona spunta/despunta tutto). */
    val allItemsChecked: Boolean
        get() = allItems().let { it.isNotEmpty() && it.all { item -> item.checked } }
}

@HiltViewModel
class ChecklistDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shoppingRepository: ShoppingRepository,
    private val itemRepository: ShoppingItemRepository,
    private val realtimeService: RealtimeService,
    private val sortPreferences: SortPreferences,
    private val session: SessionManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val listId: Long = checkNotNull(savedStateHandle["listId"])
    private val navName: String? = savedStateHandle["name"]
    private val navStarred: Boolean = savedStateHandle["starred"] ?: false

    // Ordinamento ricordato dall'ultima volta (issue #9).
    private val savedSort = sortPreferences.get(SortPreferences.CHECKLIST_ITEMS)
    private val _uiState = MutableStateFlow(
        ChecklistDetailUiState(
            placeholderName = navName,
            placeholderStarred = navStarred,
            sort = savedSort?.criterion?.let { runCatching { ChecklistItemSort.valueOf(it) }.getOrNull() }
                ?: ChecklistItemSort.ORDER,
            sortAsc = savedSort?.ascending ?: true,
        ),
    )
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

    /**
     * Aggiunge un articolo. Se un articolo con lo stesso nome esiste già (ovunque nella lista,
     * confronto case-insensitive) NON lo crea e mostra un avviso: niente duplicati per nome. (#7)
     */
    fun addItem(name: String, categoryId: Long?) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        if (itemExists(trimmed)) {
            _uiState.update {
                it.copy(
                    message = context.getString(R.string.cd_duplicate_toast, trimmed),
                    messageReportable = false,
                )
            }
            return
        }
        viewModelScope.launch {
            val result = itemRepository.create(
                CreateShoppingItemRequest(
                    shoppingListId = listId,
                    name = trimmed,
                    categoryId = categoryId,
                )
            )
            when (result) {
                is ApiResult.Success -> load()
                is ApiResult.Error -> _uiState.update { it.copy(message = result.message) }
            }
        }
    }

    /** Esiste già un articolo con questo nome (ovunque nell'albero, case-insensitive)? */
    private fun itemExists(name: String): Boolean {
        val list = _uiState.value.list ?: return false
        fun collect(cats: List<ShoppingCategory>): List<ShoppingItem> =
            cats.flatMap { it.items + collect(it.children) }
        return (list.items + collect(list.categories)).any { it.name.trim().equals(name, ignoreCase = true) }
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
            load(showLoading = false)
        }
    }

    /** Rinomina la checklist (modifica classica del nome, come nelle altre sezioni). */
    fun renameList(newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            val r = shoppingRepository.update(listId, UpdateShoppingListRequest(name = newName.trim()))
            if (r is ApiResult.Error) _uiState.update { it.copy(message = r.message) }
            load(showLoading = false)
        }
    }

    /** true se l'utente loggato è il proprietario della checklist (può cambiarne il tipo). (#11) */
    val isOwner: Boolean
        get() = _uiState.value.list?.ownerId != null &&
            _uiState.value.list?.ownerId == session.currentUser.value?.id

    /**
     * Cambia il tipo della checklist (condivisa ↔ personale). Richiede requester_id (owner).
     * Il passaggio a personale è distruttivo (rimuove i partecipanti): la conferma è a carico
     * della UI. (issue #11)
     */
    fun setListType(shared: Boolean) {
        val requesterId = session.currentUser.value?.id ?: return
        viewModelScope.launch {
            val r = shoppingRepository.update(
                listId,
                UpdateShoppingListRequest(
                    listType = if (shared) "shared" else "personal",
                    requesterId = requesterId,
                ),
            )
            if (r is ApiResult.Error) _uiState.update { it.copy(message = r.message) }
            load(showLoading = false)
        }
    }

    /** Aggiunge/toglie la checklist dai preferiti (stella). */
    fun toggleStarred() {
        val list = _uiState.value.list ?: return
        viewModelScope.launch {
            shoppingRepository.update(
                listId,
                UpdateShoppingListRequest(starred = !list.starred),
            )
            load(showLoading = false)
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
        val catsCreate = mutableListOf<BatchCategoryCreate>()
        val itemsCreate = mutableListOf<BatchItemCreate>()
        var currentCatTemp: String? = null
        // Marcatori di spunta a inizio riga.
        val checkedPrefixes = listOf("[v]", "[x]", "[X]", "✓", "☑", "[✓]")
        val uncheckedPrefixes = listOf("[ ]", "○", "☐", "-", "[]")

        lines.forEach { raw ->
            if (raw.endsWith(":")) {
                val name = raw.dropLast(1).trim()
                if (name.isNotEmpty()) {
                    val t = "t${tempId++}"
                    catsCreate += BatchCategoryCreate(tempId = t, name = name)
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
                    itemsCreate += BatchItemCreate(
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
            val req = BatchSaveRequest(
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

    // --- Selezione multipla ARTICOLI (issue #10) ---
    val selection = SelectionController()

    /** Elimina tutti gli articoli selezionati in UNA chiamata bulk, poi esce dalla selezione. */
    fun deleteSelectedItems() {
        val ids = selection.ids.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val r = itemRepository.deleteBulk(ids)
            selection.clear()
            if (r is ApiResult.Error) _uiState.update { it.copy(message = r.message) }
            load(showLoading = false)
        }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null, messageReportable = true) }

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
        // Item senza categoria, in cima (depth 0: nessuna guida albero).
        list.items.filter { matchesQuery(it, q) }.sortedWith(cmp).forEach { rows += ChecklistRow.Item(it, 0) }
        val topCats = list.categories.sortedBy { it.sortOrder }.filter { q.isBlank() || categoryHasMatch(it, q) }
        topCats.forEachIndexed { i, cat ->
            appendCategory(cat, 0, rows, q, cmp, isLast = i == topCats.lastIndex)
        }
        return rows
    }

    /** True se la categoria o un suo discendente contiene almeno un item che matcha la query. */
    private fun categoryHasMatch(cat: ShoppingCategory, q: String): Boolean =
        cat.items.any { matchesQuery(it, q) } || cat.children.any { categoryHasMatch(it, q) }

    /**
     * True se la categoria contiene almeno un articolo (diretto o nei discendenti) e TUTTI sono
     * spuntati: in tal caso il nome va barrato.
     */
    private fun categoryCompleted(cat: ShoppingCategory): Boolean {
        fun collect(c: ShoppingCategory): List<ShoppingItem> =
            c.items + c.children.flatMap { collect(it) }
        val all = collect(cat)
        return all.isNotEmpty() && all.all { it.checked }
    }

    private fun appendCategory(
        cat: ShoppingCategory,
        depth: Int,
        out: MutableList<ChecklistRow>,
        q: String,
        cmp: Comparator<ShoppingItem>,
        isLast: Boolean = false,
    ) {
        // In ricerca, salta le categorie che non contengono nulla di corrispondente.
        if (q.isNotBlank() && !categoryHasMatch(cat, q)) return

        val isCollapsed = cat.id in collapsed
        out += ChecklistRow.CategoryHeader(
            cat, depth, collapsed = isCollapsed,
            directCount = cat.items.size + cat.children.size,
            completed = categoryCompleted(cat),
            isLastChild = isLast,
        )
        // In ricerca ignoriamo il collasso (si vogliono vedere i risultati).
        if (isCollapsed && q.isBlank()) return
        // Figli del gruppo: prima gli articoli, poi le sottocategorie (filtrati). L'ultimo in
        // assoluto è "ultimo figlio" → connettore └─.
        val childItems = cat.items.filter { matchesQuery(it, q) }.sortedWith(cmp)
        val childCats = cat.children.sortedBy { it.sortOrder }.filter { q.isBlank() || categoryHasMatch(it, q) }
        childItems.forEachIndexed { i, item ->
            val last = childCats.isEmpty() && i == childItems.lastIndex
            out += ChecklistRow.Item(item, depth + 1, isLastChild = last)
        }
        childCats.forEachIndexed { i, child ->
            appendCategory(child, depth + 1, out, q, cmp, isLast = i == childCats.lastIndex)
        }
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
        // Ricorda la scelta per la prossima apertura (issue #9).
        _uiState.value.let { sortPreferences.set(SortPreferences.CHECKLIST_ITEMS, it.sort.name, it.sortAsc) }
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
