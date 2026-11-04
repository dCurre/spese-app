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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.list?.name ?: "Lista") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
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
        ExpenseDialog(
            expense = editing,
            participants = state.participants,
            expenseTypes = state.expenseTypes,
            onDismiss = { showDialog = false },
            onDelete = editing?.let { current ->
                { viewModel.deleteExpense(current); showDialog = false }
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
