package com.ashleykaminski.aipackinglist

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

    // State to keep track of the item ID that is currently "active" for showing delete
    var activeItemIdForDelete by remember { mutableStateOf<Int?>(null) }

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
                        val newItem = SelectableItem(id = nextItemId++, text = newItemText)
                        rememberedItems = rememberedItems + newItem
                        val oldNewItemText = newItemText
                        newItemText = ""
                        activeItemIdForDelete = null // Hide delete icon on new item add
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
                // Add a clickable modifier to the LazyColumn to reset activeItemIdForDelete
                // when clicking outside of any item (e.g., on empty space)
                .clickable { activeItemIdForDelete = null },
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(rememberedItems, key = { it.id }) { item ->
                SelectableListItem(
                    item = item,
                    isDeleteVisible = item.id == activeItemIdForDelete,
                    onItemClick = {
                        // Toggle active state: if already active, deactivate, else activate
                        activeItemIdForDelete =
                            if (activeItemIdForDelete == item.id) null else item.id
                    },
                    onItemSelected = { newItemState ->
                        rememberedItems = rememberedItems.map {
                            if (it.id == newItemState.id) newItemState else it
                        }
                        // Optionally, you might want to hide the delete icon when checkbox is toggled
                        // activeItemIdForDelete = null
                    },
                    onDeleteItem = { itemToDelete ->
                        rememberedItems = rememberedItems - itemToDelete
                        activeItemIdForDelete = null // Hide delete icon after deletion
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
    isDeleteVisible: Boolean, // New parameter to control delete icon visibility
    onItemClick: () -> Unit,   // Callback when the item row is clicked
    onItemSelected: (SelectableItem) -> Unit,
    onDeleteItem: (SelectableItem) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() } // Make the whole row clickable
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.isSelected,
            onCheckedChange = { isChecked ->
                onItemSelected(item.copy(isSelected = isChecked))
            }
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = item.text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        // Conditionally display the IconButton
        if (isDeleteVisible) {
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { onDeleteItem(item) }) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete item",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        } else {
            // Optional: Add a spacer to maintain layout consistency when icon is hidden
            // This ensures the item text doesn't shift when the icon appears/disappears.
            // Adjust width to match IconButton's approximate width or use a fixed size.
            // For a standard IconButton, this might be around 48.dp.
            // Spacer(modifier = Modifier.width(48.dp)) // Or Icons.Filled.Delete.defaultWidth
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