package com.dcapps.spese.feature.checklist

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistDetailScreen(
    onBack: () -> Unit,
    viewModel: ChecklistDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var newItem by remember { mutableStateOf("") }

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
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = newItem,
                    onValueChange = { newItem = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Aggiungi articolo") },
                    singleLine = true,
                )
                IconButton(
                    onClick = {
                        viewModel.addItem(newItem, null)
                        newItem = ""
                    },
                    enabled = newItem.isNotBlank(),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Aggiungi")
                }
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.error != null -> Text(state.error!!, Modifier.align(Alignment.Center).padding(32.dp))
                state.rows.isEmpty() -> Text(
                    "Lista vuota. Aggiungi un articolo qui sotto.",
                    Modifier.align(Alignment.Center).padding(32.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(state.rows, key = { rowKey(it) }) { row ->
                        when (row) {
                            is ChecklistRow.CategoryHeader -> CategoryHeaderRow(row)
                            is ChecklistRow.Item -> ItemRow(
                                row = row,
                                onToggle = { viewModel.toggleItem(row.item) },
                                onDelete = { viewModel.deleteItem(row.item) },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun rowKey(row: ChecklistRow): String = when (row) {
    is ChecklistRow.CategoryHeader -> "c${row.category.id}"
    is ChecklistRow.Item -> "i${row.item.id}"
}

@Composable
private fun CategoryHeaderRow(row: ChecklistRow.CategoryHeader) {
    Text(
        text = row.category.name,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (16 + row.depth * 16).dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
    )
    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun ItemRow(
    row: ChecklistRow.Item,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    val item = row.item
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (8 + row.depth * 16).dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Checkbox(checked = item.checked, onCheckedChange = { onToggle() })
        Text(
            text = buildString {
                append(item.name)
                val q = item.quantity ?: 1
                if (q > 1) append("  ×$q")
            },
            style = MaterialTheme.typography.bodyLarge,
            textDecoration = if (item.checked) TextDecoration.LineThrough else null,
            color = if (item.checked) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Elimina", modifier = Modifier.padding(0.dp))
        }
    }
}
