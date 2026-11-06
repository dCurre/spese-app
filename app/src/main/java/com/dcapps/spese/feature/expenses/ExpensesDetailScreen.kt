package com.dcapps.spese.feature.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dcapps.spese.R
import com.dcapps.spese.core.model.Expense
import com.dcapps.spese.core.util.toEuro
import com.dcapps.spese.core.util.toShortDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesDetailScreen(
    onBack: () -> Unit,
    viewModel: ExpensesDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Expense?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var showParticipants by remember { mutableStateOf(false) }
    var showTransfer by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var repeating by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

    val isShared = state.list?.type == com.dcapps.spese.core.model.ListType.SHARED
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    com.dcapps.spese.ui.components.ErrorSnackbarEffect(
        state.message, snackbarHostState, viewModel::clearMessage,
    )

    // Condivisione: apre il chooser di sistema quando è disponibile un link.
    state.shareLink?.let { link ->
        ShareLinkEffect(link = link, onShared = viewModel::clearShareLink)
    }

    Scaffold(
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
        topBar = {
            com.dcapps.spese.ui.components.SpeseTopAppBar(
                title = state.list?.name ?: stringResource(R.string.detail_list_fallback),
                onTitleEdit = { showRename = true },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    ExpensesListMenu(
                        isShared = isShared,
                        onShare = viewModel::share,
                        onParticipants = {
                            viewModel.loadAvailableUsers()
                            showParticipants = true
                        },
                        onTransfer = {
                            viewModel.loadAvailableUsers()
                            showTransfer = true
                        },
                    )
                },
            )
        },
        floatingActionButton = {
            SmallFloatingActionButton(onClick = { editing = null; showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.detail_new_expense))
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
                    onQueryChange = viewModel::onQueryChange,
                    onSortChange = viewModel::onSortChange,
                    onEdit = { editing = it; repeating = false; showDialog = true },
                    onRepeat = { editing = it; repeating = true; showDialog = true },
                )
            }
        }
    }

    if (showDialog) {
        val isPersonal = state.list?.type == com.dcapps.spese.core.model.ListType.PERSONAL
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
        com.dcapps.spese.ui.components.ConfirmDialog(
            title = stringResource(R.string.detail_delete_expense_title),
            message = stringResource(R.string.detail_delete_expense_message, exp.name),
            onConfirm = { viewModel.deleteExpense(exp) },
            onDismiss = { expenseToDelete = null },
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
}

@Composable
private fun RenameListDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(currentName) }
    com.dcapps.spese.ui.components.SpeseDialog(
        title = stringResource(R.string.detail_rename_list),
        onDismiss = onDismiss,
        content = {
            androidx.compose.material3.OutlinedTextField(
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val shareText = stringResource(R.string.detail_share_text, link)
    val chooserTitle = stringResource(R.string.detail_share_chooser)
    androidx.compose.runtime.LaunchedEffect(link) {
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
    onShare: () -> Unit,
    onParticipants: () -> Unit,
    onTransfer: () -> Unit,
) {
    // Il menu "⋮" contiene solo azioni di condivisione: se la lista non è condivisa non c'è
    // nulla da mostrare (la rinomina è passata alla matita accanto al titolo).
    if (!isShared) return

    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.detail_more_actions))
    }
    androidx.compose.material3.DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
    ) {
        androidx.compose.material3.DropdownMenuItem(
            text = { Text(stringResource(R.string.detail_share)) },
            onClick = { expanded = false; onShare() },
        )
        androidx.compose.material3.DropdownMenuItem(
            text = { Text(stringResource(R.string.detail_participants)) },
            onClick = { expanded = false; onParticipants() },
        )
        androidx.compose.material3.DropdownMenuItem(
            text = { Text(stringResource(R.string.detail_transfer_owner)) },
            onClick = { expanded = false; onTransfer() },
        )
    }
}

@Composable
private fun DetailContent(
    state: ExpensesDetailUiState,
    onQueryChange: (String) -> Unit,
    onSortChange: (ExpenseSort) -> Unit,
    onEdit: (Expense) -> Unit,
    onRepeat: (Expense) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 88.dp),
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
                    text = androidx.compose.ui.text.buildAnnotatedString {
                        if (commaIdx > 0) {
                            append(euro.substring(0, commaIdx))
                            pushStyle(androidx.compose.ui.text.SpanStyle(fontSize = centsSize, color = centsColor))
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
        if (state.list?.type == com.dcapps.spese.core.model.ListType.SHARED && state.balanceTotals.isNotEmpty()) {
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
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text(stringResource(R.string.detail_search_expense)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                singleLine = true,
            )
        }
        item {
            com.dcapps.spese.ui.components.SortBar(
                options = listOf(
                    com.dcapps.spese.ui.components.SortOption(ExpenseSort.DATE, stringResource(R.string.sort_date)),
                    com.dcapps.spese.ui.components.SortOption(ExpenseSort.NAME, stringResource(R.string.sort_name)),
                    com.dcapps.spese.ui.components.SortOption(ExpenseSort.AMOUNT, stringResource(R.string.sort_amount)),
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
                androidx.compose.material3.HorizontalDivider(
                    Modifier.padding(horizontal = 16.dp),
                    color = com.dcapps.spese.ui.theme.SpeseTheme.colors.divider,
                )
            }
            ExpenseRow(expense, onEdit, onRepeat)
            androidx.compose.material3.HorizontalDivider(
                Modifier.padding(horizontal = 16.dp),
                color = com.dcapps.spese.ui.theme.SpeseTheme.colors.divider,
            )
        }
    }
}

/** Riga spesa in stile "ledger": badge €, nome + sottotitolo, importo colorato a destra. */
@Composable
private fun ExpenseRow(
    expense: Expense,
    onEdit: (Expense) -> Unit,
    onRepeat: (Expense) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit(expense) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Badge circolare €
        Box(
            Modifier
                .size(36.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
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
            color = com.dcapps.spese.ui.theme.SpeseTheme.colors.debit,
        )
        IconButton(onClick = { onRepeat(expense) }) {
            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.detail_repeat_expense))
        }
    }
}
