package com.dcapps.spese.feature.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
            TopAppBar(
                title = { Text(state.list?.name ?: "Lista") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    ExpensesListMenu(
                        isShared = isShared,
                        onRename = { showRename = true },
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
            FloatingActionButton(onClick = { editing = null; showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Nuova spesa")
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
                    onEdit = { editing = it; showDialog = true },
                )
            }
        }
    }

    if (showDialog) {
        val isPersonal = state.list?.type == com.dcapps.spese.core.model.ListType.PERSONAL
        ExpenseDialog(
            expense = editing,
            participants = state.participants,
            expenseTypes = state.expenseTypes,
            isPersonal = isPersonal,
            personalOwnerId = state.list?.userId,
            onDismiss = { showDialog = false },
            onDelete = editing?.let { current ->
                { showDialog = false; expenseToDelete = current }
            },
            onConfirm = { name, amount, ownerId, date, typeId ->
                val current = editing
                if (current == null) {
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
            title = "Elimina spesa",
            message = "Vuoi eliminare \"${exp.name}\"?",
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
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rinomina lista") },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name) },
            ) { Text("Salva") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Annulla") }
        },
    )
}

@Composable
private fun ShareLinkEffect(link: String, onShared: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(link) {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, "Unisciti alla mia lista spese: $link")
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Condividi lista"))
        onShared()
    }
}

@Composable
private fun ExpensesListMenu(
    isShared: Boolean,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onParticipants: () -> Unit,
    onTransfer: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Default.MoreVert, contentDescription = "Altre azioni")
    }
    androidx.compose.material3.DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
    ) {
        androidx.compose.material3.DropdownMenuItem(
            text = { Text("Rinomina lista") },
            onClick = { expanded = false; onRename() },
        )
        if (isShared) {
            androidx.compose.material3.DropdownMenuItem(
                text = { Text("Condividi") },
                onClick = { expanded = false; onShare() },
            )
            androidx.compose.material3.DropdownMenuItem(
                text = { Text("Partecipanti") },
                onClick = { expanded = false; onParticipants() },
            )
            androidx.compose.material3.DropdownMenuItem(
                text = { Text("Trasferisci proprietà") },
                onClick = { expanded = false; onTransfer() },
            )
        }
    }
}

@Composable
private fun DetailContent(
    state: ExpensesDetailUiState,
    onQueryChange: (String) -> Unit,
    onEdit: (Expense) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Totale", style = MaterialTheme.typography.labelMedium)
                    Text(
                        state.total.toEuro(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        if (state.list?.type == com.dcapps.spese.core.model.ListType.SHARED && state.balanceTotals.isNotEmpty()) {
            item {
                BalanceSection(
                    totals = state.balanceTotals,
                    balance = state.balanceEntries,
                    expenses = state.expenses,
                )
            }
        }

        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Cerca spesa") },
                singleLine = true,
            )
        }

        if (state.visibleExpenses.isEmpty()) {
            item {
                Text(
                    "Nessuna spesa. Aggiungine una con il +.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }

        items(state.visibleExpenses, key = { it.id }) { expense ->
            ExpenseCard(expense, onEdit)
        }
    }
}

@Composable
private fun ExpenseCard(
    expense: Expense,
    onEdit: (Expense) -> Unit,
) {
    Card(onClick = { onEdit(expense) }, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(expense.name, style = MaterialTheme.typography.titleMedium)
                val owner = expense.owner?.let {
                    listOfNotNull(it.name, it.surname).joinToString(" ").ifBlank { it.email ?: "" }
                } ?: ""
                val subtitle = buildString {
                    if (owner.isNotBlank()) append(owner)
                    expense.expenseType?.let { if (isNotEmpty()) append(" · "); append(it) }
                    expense.expenseDate?.let { append(" · "); append(it.toShortDate()) }
                }
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                expense.amount.toEuro(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
