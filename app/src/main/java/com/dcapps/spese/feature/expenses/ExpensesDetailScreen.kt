package com.dcapps.spese.feature.expenses

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dcapps.spese.R
import com.dcapps.spese.core.model.Expense
import com.dcapps.spese.core.model.ListType
import com.dcapps.spese.core.util.toEuro
import com.dcapps.spese.core.util.toShortDate
import com.dcapps.spese.ui.components.ConfirmDialog
import com.dcapps.spese.ui.components.ErrorSnackbarEffect
import com.dcapps.spese.ui.components.OverflowMenu
import com.dcapps.spese.ui.components.RowDivider
import com.dcapps.spese.ui.components.SortBar
import com.dcapps.spese.ui.components.SortOption
import com.dcapps.spese.ui.components.SelectableRow
import com.dcapps.spese.ui.components.SelectionTopBar
import com.dcapps.spese.ui.components.SpeseDialog
import com.dcapps.spese.ui.components.SpeseSearchField
import com.dcapps.spese.ui.components.SpeseTopAppBar
import com.dcapps.spese.core.ui.SelectionUiState
import com.dcapps.spese.ui.theme.SpeseTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesDetailScreen(
    onBack: () -> Unit,
    viewModel: ExpensesDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sel by viewModel.selection.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Expense?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var showParticipants by remember { mutableStateOf(false) }
    var showTransfer by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showMakePersonal by remember { mutableStateOf(false) }
    var showDeleteSelected by remember { mutableStateOf(false) }
    var repeating by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

    val isShared = state.list?.type == ListType.SHARED
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // Messaggio mostrato quando un guest tocca un'azione di condivisione lucchettata.
    val guestLockedMsg = stringResource(R.string.guest_share_locked)
    val onGuestLocked: () -> Unit = {
        scope.launch { snackbarHostState.showSnackbar(guestLockedMsg) }
    }

    ErrorSnackbarEffect(
        state.message, snackbarHostState, viewModel::clearMessage,
    )

    // In modalità selezione, il tasto Indietro annulla la selezione invece di uscire.
    BackHandler(enabled = sel.selectionMode) { viewModel.selection.clear() }

    // Condivisione: apre il chooser di sistema quando è disponibile un link.
    state.shareLink?.let { link ->
        ShareLinkEffect(link = link, onShared = viewModel::clearShareLink)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (sel.selectionMode) {
                SelectionTopBar(
                    count = sel.selectedIds.size,
                    onCancel = { viewModel.selection.clear() },
                    onDelete = { showDeleteSelected = true },
                )
            } else {
                SpeseTopAppBar(
                    title = state.displayName ?: stringResource(R.string.detail_list_fallback),
                    onTitleEdit = { showRename = true },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    },
                    actions = {
                        ExpensesListMenu(
                            isShared = isShared,
                            canChangeType = viewModel.isOwner,
                            isGuest = viewModel.isGuest,
                            onGuestLocked = onGuestLocked,
                            onShare = viewModel::share,
                            onParticipants = {
                                viewModel.loadAvailableUsers()
                                showParticipants = true
                            },
                            onTransfer = {
                                viewModel.loadAvailableUsers()
                                showTransfer = true
                            },
                            onMakeShared = { viewModel.setListType(true) },
                            onMakePersonal = { showMakePersonal = true },
                        )
                    },
                )
            }
        },
        floatingActionButton = {
            if (!sel.selectionMode) {
                FloatingActionButton(onClick = { editing = null; showDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.detail_new_expense))
                }
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.error != null -> Text(
                    state.error!!,
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                )
                else -> DetailContent(
                    state = state,
                    sel = sel,
                    onQueryChange = viewModel::onQueryChange,
                    onSortChange = viewModel::onSortChange,
                    onEdit = { editing = it; repeating = false; showDialog = true },
                    onRepeat = { editing = it; repeating = true; showDialog = true },
                    onLongPress = { viewModel.selection.start(it.id) },
                    onToggleSelect = { viewModel.selection.toggle(it.id) },
                )
            }
        }
    }

    if (showDialog) {
        val isPersonal = state.list?.type == ListType.PERSONAL
        val nameSuggestions = state.expenses.map { it.name }.filter { it.isNotBlank() }
        ExpenseDialog(
            expense = editing,
            participants = state.participants,
            expenseTypes = state.expenseTypes,
            isPersonal = isPersonal,
            personalOwnerId = state.list?.userId,
            nameSuggestions = nameSuggestions,
            forceCreate = repeating,
            onDismiss = { showDialog = false },
            onDelete = if (!repeating) {
                editing?.let { current -> { showDialog = false; expenseToDelete = current } }
            } else null,
            onConfirm = { name, amount, ownerId, date, typeId ->
                val current = editing
                if (current == null || repeating) {
                    viewModel.addExpense(name, amount, ownerId, date, typeId)
                } else {
                    viewModel.updateExpense(current.id, name, amount, ownerId, date, typeId, ownerId)
                }
                showDialog = false
            },
        )
    }

    expenseToDelete?.let { exp ->
        ConfirmDialog(
            title = stringResource(R.string.detail_delete_expense_title),
            message = stringResource(R.string.detail_delete_expense_message, exp.name),
            onConfirm = { viewModel.deleteExpense(exp) },
            onDismiss = { expenseToDelete = null },
        )
    }

    // Conferma eliminazione multipla (issue #10).
    if (showDeleteSelected) {
        ConfirmDialog(
            title = stringResource(R.string.expenses_delete_selected_title),
            message = stringResource(R.string.expenses_delete_selected_message, sel.selectedIds.size),
            onConfirm = { viewModel.deleteSelected() },
            onDismiss = { showDeleteSelected = false },
        )
    }

    if (showParticipants) {
        ParticipantsDialog(
            participants = state.participants,
            availableUsers = state.availableUsers,
            ownerId = state.list?.userId,
            onAddUser = { viewModel.addParticipant(it) },
            onAddGuest = { viewModel.addGuest(it) },
            onRemove = { viewModel.removeParticipant(it) },
            onDismiss = { showParticipants = false },
        )
    }

    if (showTransfer) {
        TransferOwnerDialog(
            participants = state.participants,
            ownerId = state.list?.userId,
            onConfirm = { viewModel.transferOwner(it); showTransfer = false },
            onDismiss = { showTransfer = false },
        )
    }

    if (showRename) {
        RenameListDialog(
            currentName = state.list?.name ?: "",
            onDismiss = { showRename = false },
            onConfirm = { viewModel.renameList(it); showRename = false },
        )
    }

    // Conferma passaggio a personale: DISTRUTTIVO (spese all'owner + partecipanti rimossi). (#11)
    if (showMakePersonal) {
        ConfirmDialog(
            title = stringResource(R.string.list_make_personal_title),
            message = stringResource(R.string.expenses_make_personal_message),
            confirmText = stringResource(R.string.list_make_personal),
            onConfirm = { viewModel.setListType(false) },
            onDismiss = { showMakePersonal = false },
        )
    }
}

@Composable
private fun RenameListDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(currentName) }
    SpeseDialog(
        title = stringResource(R.string.detail_rename_list),
        onDismiss = onDismiss,
        icon = Icons.Outlined.DriveFileRenameOutline,
        content = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmText = stringResource(R.string.action_save),
        onConfirm = { onConfirm(name) },
        confirmEnabled = name.isNotBlank(),
    )
}

@Composable
private fun ShareLinkEffect(link: String, onShared: () -> Unit) {
    val context = LocalContext.current
    val shareText = stringResource(R.string.detail_share_text, link)
    val chooserTitle = stringResource(R.string.detail_share_chooser)
    LaunchedEffect(link) {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(android.content.Intent.createChooser(intent, chooserTitle))
        onShared()
    }
}

@Composable
private fun ExpensesListMenu(
    isShared: Boolean,
    canChangeType: Boolean,
    isGuest: Boolean,
    onGuestLocked: () -> Unit,
    onShare: () -> Unit,
    onParticipants: () -> Unit,
    onTransfer: () -> Unit,
    onMakeShared: () -> Unit,
    onMakePersonal: () -> Unit,
) {
    // Mostra il menu se c'è almeno un'azione: condivisione (solo se condivisa) o cambio tipo (owner).
    // In guest mostriamo comunque le voci di condivisione (lucchettate) quando la lista è condivisa.
    if (!isShared && !canChangeType) return

    OverflowMenu { dismiss ->
        if (isShared) {
            LockableMenuItem(
                label = stringResource(R.string.detail_share),
                locked = isGuest,
                onClick = onShare,
                onLocked = onGuestLocked,
                dismiss = dismiss,
            )
            LockableMenuItem(
                label = stringResource(R.string.detail_participants),
                locked = isGuest,
                onClick = onParticipants,
                onLocked = onGuestLocked,
                dismiss = dismiss,
            )
            LockableMenuItem(
                label = stringResource(R.string.detail_transfer_owner),
                locked = isGuest,
                onClick = onTransfer,
                onLocked = onGuestLocked,
                dismiss = dismiss,
            )
        }
        // Cambio tipo condivisa↔personale, solo per l'owner (issue #11). In guest è lucchettato.
        if (canChangeType) {
            LockableMenuItem(
                label = stringResource(
                    if (isShared) R.string.list_make_personal else R.string.list_make_shared,
                ),
                locked = isGuest,
                onClick = { if (isShared) onMakePersonal() else onMakeShared() },
                onLocked = onGuestLocked,
                dismiss = dismiss,
            )
        }
    }
}

/**
 * Voce di menu che può essere "lucchettata" per il guest: resta visibile ma oscurata, con un'icona
 * a lucchetto; il tap NON esegue l'azione reale ma invoca [onLocked] (es. invito ad accedere).
 */
@Composable
private fun LockableMenuItem(
    label: String,
    locked: Boolean,
    onClick: () -> Unit,
    onLocked: () -> Unit,
    dismiss: () -> Unit,
) {
    if (locked) {
        val lockedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        DropdownMenuItem(
            text = { Text(label, color = lockedColor) },
            trailingIcon = {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = stringResource(R.string.cd_locked),
                    tint = lockedColor,
                )
            },
            onClick = { dismiss(); onLocked() },
        )
    } else {
        DropdownMenuItem(
            text = { Text(label) },
            onClick = { dismiss(); onClick() },
        )
    }
}

@Composable
private fun DetailContent(
    state: ExpensesDetailUiState,
    sel: SelectionUiState,
    onQueryChange: (String) -> Unit,
    onSortChange: (ExpenseSort) -> Unit,
    onEdit: (Expense) -> Unit,
    onRepeat: (Expense) -> Unit,
    onLongPress: (Expense) -> Unit,
    onToggleSelect: (Expense) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp),
    ) {
        // Totale: numero grande con i centesimi più piccoli/attenuati (come nel mock).
        item {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
                Text(
                    stringResource(R.string.detail_total).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val euro = state.total.toEuro()
                // Separa i centesimi (dopo la virgola) per mostrarli più piccoli.
                val commaIdx = euro.lastIndexOf(',')
                val centsSize = MaterialTheme.typography.headlineSmall.fontSize
                val centsColor = MaterialTheme.colorScheme.onSurfaceVariant
                Text(
                    text = buildAnnotatedString {
                        if (commaIdx > 0) {
                            append(euro.substring(0, commaIdx))
                            pushStyle(SpanStyle(fontSize = centsSize, color = centsColor))
                            append(euro.substring(commaIdx))
                            pop()
                        } else {
                            append(euro)
                        }
                    },
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
        if (state.list?.type == ListType.SHARED && state.balanceTotals.isNotEmpty()) {
            item {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    BalanceSection(
                        totals = state.balanceTotals,
                        balance = state.balanceEntries,
                        expenses = state.expenses,
                    )
                }
            }
        }

        item {
            SpeseSearchField(
                query = state.query,
                onQueryChange = onQueryChange,
                placeholder = stringResource(R.string.detail_search_expense),
            )
        }
        item {
            SortBar(
                options = listOf(
                    SortOption(ExpenseSort.DATE, stringResource(R.string.sort_date)),
                    SortOption(ExpenseSort.NAME, stringResource(R.string.sort_name)),
                    SortOption(ExpenseSort.AMOUNT, stringResource(R.string.sort_amount)),
                ),
                selectedKey = state.sort,
                ascending = state.sortAsc,
                onSelect = onSortChange,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        if (state.visibleExpenses.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.detail_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        itemsIndexed(state.visibleExpenses, key = { _, it -> it.id }) { index, expense ->
            if (index == 0) {
                RowDivider()
            }
            ExpenseRow(expense, sel, onEdit, onRepeat, onLongPress, onToggleSelect)
            HorizontalDivider(
                Modifier.padding(horizontal = 16.dp),
                color = SpeseTheme.colors.divider,
            )
        }
    }
}

/** Riga spesa in stile "ledger": badge €, nome + sottotitolo, importo colorato a destra. */
@Composable
private fun ExpenseRow(
    expense: Expense,
    sel: SelectionUiState,
    onEdit: (Expense) -> Unit,
    onRepeat: (Expense) -> Unit,
    onLongPress: (Expense) -> Unit,
    onToggleSelect: (Expense) -> Unit,
) {
    SelectableRow(
        selectionMode = sel.selectionMode,
        selected = expense.id in sel.selectedIds,
        onClick = { onEdit(expense) },
        onLongClick = { onLongPress(expense) },
        onToggle = { onToggleSelect(expense) },
        modifier = Modifier.padding(vertical = 0.dp),
    ) {
        // Badge circolare €
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text("€", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(expense.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            val owner = expense.owner?.let {
                listOfNotNull(it.name, it.surname).joinToString(" ").ifBlank { it.email ?: "" }
            } ?: ""
            val subtitle = buildString {
                if (owner.isNotBlank()) append(owner)
                expense.expenseType?.let { if (isNotEmpty()) append(" · "); append(it) }
                expense.expenseDate?.let { append(" · "); append(it.toShortDate()) }
            }
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        // Importo: colore semantico (spesa = debito/rosso).
        Text(
            expense.amount.toEuro(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = SpeseTheme.colors.debit,
        )
        if (!sel.selectionMode) {
            IconButton(onClick = { onRepeat(expense) }) {
                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.detail_repeat_expense))
            }
        }
    }
}
