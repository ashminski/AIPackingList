package com.ashleykaminski.aipackinglist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

// Data class to represent an item in the list
data class SelectableItem(
    val id: Int,
    val text: String,
    var isSelected: Boolean = false // For the checkbox
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectableListScreen(initialItems: List<SelectableItem>) {
    var rememberedItems by remember { mutableStateOf(initialItems) }
    var newItemText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var nextItemId by remember { mutableStateOf(initialItems.maxOfOrNull { it.id }?.plus(1) ?: 1) }

    var activeItemIdForDelete by remember { mutableStateOf<Int?>(null) }

    // Derived state for sorted items
    val sortedItems by remember(rememberedItems) {
        derivedStateOf {
            rememberedItems.sortedWith(compareBy { it.isSelected })
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text("Shopping List") })
        },
        bottomBar = {
            NewItemInputRow(
                newItemText = newItemText,
                onNewItemTextChange = { newItemText = it },
                onAddItemClick = {
                    if (newItemText.isNotBlank()) {
                        val newItem = SelectableItem(id = nextItemId++, text = newItemText) // isSelected is false by default
                        val oldNewItemText = newItemText

                        // Add the new item and then re-sort the entire list
                        // to ensure it's placed correctly (after existing unchecked, before any checked).
                        rememberedItems = (rememberedItems + newItem)
                            .sortedWith(compareBy { it.isSelected }) // Re-sort here

                        newItemText = ""
                        activeItemIdForDelete = null
                        scope.launch {
                            snackbarHostState.showSnackbar("Item added: ${oldNewItemText}")
                        }
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar("Please enter item text")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .clickable { activeItemIdForDelete = null },
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(sortedItems, key = { it.id }) { item ->
                SelectableListItem(
                    item = item,
                    isCurrentlyActiveForDelete = item.id == activeItemIdForDelete,
                    onItemClick = {
                        activeItemIdForDelete = if (activeItemIdForDelete == item.id) null else item.id
                    },
                    onItemSelected = { updatedItem ->
                        rememberedItems = rememberedItems.map {
                            if (it.id == updatedItem.id) updatedItem else it
                        }
                        // If an item is checked, it can't be active for delete in the same action
                        // but if it's unchecked, it might still be the active one.
                        // For simplicity, we can just clear it here.
                        // Or, if an item is unchecked, and it was the active one, keep it active:
                        // if (updatedItem.isSelected) activeItemIdForDelete = null
                    },
                    onDeleteItem = { itemToDelete ->
                        rememberedItems = rememberedItems - itemToDelete
                        activeItemIdForDelete = null
                        scope.launch {
                            snackbarHostState.showSnackbar("Item deleted: ${itemToDelete.text}")
                        }
                    }
                )
                Divider()
            }
        }
    }
}

@Composable
fun NewItemInputRow(
    newItemText: String,
    onNewItemTextChange: (String) -> Unit,
    onAddItemClick: () -> Unit
) {
    Surface(shadowElevation = 4.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newItemText,
                onValueChange = onNewItemTextChange,
                label = { Text("New item") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onAddItemClick) {
                Text("Add")
            }
        }
    }
}


@Composable
fun SelectableListItem(
    item: SelectableItem,
    isCurrentlyActiveForDelete: Boolean,
    onItemClick: () -> Unit,
    onItemSelected: (SelectableItem) -> Unit,
    onDeleteItem: (SelectableItem) -> Unit
) {
    val itemTextColor = if (item.isSelected) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    } else {
        LocalContentColor.current
    }

    val textDecoration = if (item.isSelected) {
        TextDecoration.LineThrough
    } else {
        null
    }

    val itemBackgroundColor = when {
        isCurrentlyActiveForDelete -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(itemBackgroundColor)
            .clickable { onItemClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.isSelected,
            onCheckedChange = { isChecked ->
                onItemSelected(item.copy(isSelected = isChecked))
            },
            colors = CheckboxDefaults.colors(
                checkedColor = if (item.isSelected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                checkmarkColor = MaterialTheme.colorScheme.surface
            )
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = item.text,
            style = MaterialTheme.typography.bodyLarge,
            color = itemTextColor,
            textDecoration = textDecoration,
            modifier = Modifier.weight(1f)
        )

        if (isCurrentlyActiveForDelete) { // Show delete if item is active (regardless of checked state)
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { onDeleteItem(item) }) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete item",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        } else {
            Spacer(modifier = Modifier.width(48.dp)) // Approx. width of an IconButton
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreviewOfSelectableListScreenWithInput() {
    val sampleItems = listOf(
        SelectableItem(1, "Apples"),
        SelectableItem(2, "Bananas", isSelected = true),
        SelectableItem(3, "Milk")
    )
    MaterialTheme {
        SelectableListScreen(initialItems = sampleItems)
    }
}