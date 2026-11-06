package com.dcapps.spese.feature.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dcapps.spese.R
import com.dcapps.spese.core.model.UserRole
import com.dcapps.spese.ui.components.ConfirmDialog
import com.dcapps.spese.ui.components.ErrorSnackbarEffect
import com.dcapps.spese.ui.components.ReadOnlyPickerField
import com.dcapps.spese.ui.components.SpeseDialog
import com.dcapps.spese.ui.components.SpeseTopAppBar
import com.dcapps.spese.ui.theme.SpeseTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onBack: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    ErrorSnackbarEffect(
        state.message, snackbarHostState, viewModel::clearMessage,
        reportable = state.messageReportable,
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SpeseTopAppBar(
                title = stringResource(R.string.profile_admin),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                !state.isAdmin -> Text(
                    stringResource(R.string.admin_no_access),
                    Modifier.align(Alignment.Center).padding(32.dp),
                )
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                else -> Column(Modifier.fillMaxSize()) {
                    ScrollableTabRow(selectedTabIndex = tab, edgePadding = 0.dp) {
                        Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.admin_tab_expense_types)) })
                        Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.admin_tab_units)) })
                        Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text(stringResource(R.string.admin_tab_roles)) })
                        Tab(selected = tab == 3, onClick = { tab = 3 }, text = { Text(stringResource(R.string.admin_tab_users)) })
                    }
                    when (tab) {
                        0 -> NameSection(
                            items = state.expenseTypes.map { it.id to it.name },
                            protectedIds = state.expenseTypes.filter { it.protected }.map { it.id }.toSet(),
                            onAdd = viewModel::addExpenseType,
                            onUpdate = viewModel::updateExpenseType,
                            onDelete = viewModel::deleteExpenseType,
                        )
                        1 -> UnitSection(viewModel, state)
                        2 -> NameSection(
                            items = state.roles.map { it.id to it.name },
                            protectedIds = state.roles.filter { it.protected }.map { it.id }.toSet(),
                            onAdd = viewModel::addRole,
                            onUpdate = viewModel::updateRole,
                            onDelete = viewModel::deleteRole,
                        )
                        3 -> UsersSection(viewModel, state)
                    }
                }
            }
        }
    }
}

/** Sezione generica per elementi con solo "nome" (tipi spesa, ruoli). */
@Composable
private fun NameSection(
    items: List<Pair<Long, String>>,
    protectedIds: Set<Long>,
    onAdd: (String) -> Unit,
    onUpdate: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var editing by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Pair<Long, String>?>(null) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            TextButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(stringResource(R.string.action_add), Modifier.padding(start = 4.dp))
            }
        }
        items(items, key = { it.first }) { (id, name) ->
            val isProtected = id in protectedIds
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    if (!isProtected) {
                        IconButton(onClick = { editing = id to name }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit))
                        }
                        IconButton(onClick = { toDelete = id to name }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete), tint = SpeseTheme.colors.danger)
                        }
                    } else {
                        Text(stringResource(R.string.admin_protected), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(end = 16.dp))
                    }
                }
            }
        }
    }

    if (showAdd) {
        NameDialog(title = stringResource(R.string.action_add), initial = "", onDismiss = { showAdd = false }, onConfirm = {
            onAdd(it); showAdd = false
        })
    }
    editing?.let { (id, name) ->
        NameDialog(title = stringResource(R.string.action_edit), initial = name, onDismiss = { editing = null }, onConfirm = {
            onUpdate(id, it); editing = null
        })
    }
    toDelete?.let { (id, name) ->
        ConfirmDialog(
            title = stringResource(R.string.action_delete),
            message = stringResource(R.string.admin_delete_message, name),
            onConfirm = { onDelete(id) },
            onDismiss = { toDelete = null },
        )
    }
}

@Composable
private fun NameDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    SpeseDialog(
        title = title,
        onDismiss = onDismiss,
        icon = Icons.Outlined.Category,
        content = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(stringResource(R.string.name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmText = stringResource(R.string.action_save),
        onConfirm = { onConfirm(value.trim()) },
        confirmEnabled = value.isNotBlank(),
    )
}

@Composable
private fun UnitSection(viewModel: AdminViewModel, state: AdminUiState) {
    var showAdd by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            TextButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(stringResource(R.string.action_add), Modifier.padding(start = 4.dp))
            }
        }
        items(state.units, key = { it.id }) { u ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                        Text("${u.name} (${u.symbol})", style = MaterialTheme.typography.bodyLarge)
                        Text(u.category, style = MaterialTheme.typography.bodySmall)
                    }
                    if (!u.protected) {
                        IconButton(onClick = { viewModel.deleteUnit(u.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete), tint = SpeseTheme.colors.danger)
                        }
                    } else {
                        Text(stringResource(R.string.admin_protected), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(end = 16.dp))
                    }
                }
            }
        }
    }

    if (showAdd) {
        UnitDialog(
            onDismiss = { showAdd = false },
            onConfirm = { name, symbol, category ->
                viewModel.addUnit(name, symbol, category); showAdd = false
            },
        )
    }
}

@Composable
private fun UnitDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, symbol: String, category: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var symbol by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("peso") }

    SpeseDialog(
        title = stringResource(R.string.admin_new_unit),
        onDismiss = onDismiss,
        icon = Icons.Outlined.Straighten,
        content = {
            OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(symbol, { symbol = it }, label = { Text(stringResource(R.string.admin_unit_symbol)) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            OutlinedTextField(category, { category = it }, label = { Text(stringResource(R.string.admin_unit_category)) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        },
        confirmText = stringResource(R.string.action_create),
        onConfirm = { onConfirm(name.trim(), symbol.trim(), category.trim()) },
        confirmEnabled = name.isNotBlank() && symbol.isNotBlank() && category.isNotBlank(),
    )
}

/** Sezione gestione utenti: lista con cambio ruolo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UsersSection(viewModel: AdminViewModel, state: AdminUiState) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (state.users.isEmpty()) {
            item { Text(stringResource(R.string.admin_no_users), style = MaterialTheme.typography.bodyMedium) }
        }
        items(state.users, key = { it.id }) { user ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(user.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(user.email, style = MaterialTheme.typography.bodySmall)
                    RolePicker(
                        roles = state.roles,
                        currentRoleName = user.role,
                        onSelect = { roleId -> viewModel.changeUserRole(user.id, roleId) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RolePicker(
    roles: List<UserRole>,
    currentRoleName: String?,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Il ruolo corrente è noto per nome: risalgo all'id per la selezione.
    val selectedId = roles.firstOrNull { it.name == currentRoleName }?.id
    ReadOnlyPickerField(
        label = stringResource(R.string.admin_role),
        options = roles.map { it.id to it.name },
        selectedId = selectedId,
        onSelect = onSelect,
        modifier = modifier,
    )
}
