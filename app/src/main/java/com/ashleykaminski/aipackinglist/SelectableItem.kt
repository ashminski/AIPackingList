package com.ashleykaminski.aipackinglist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch // Required for Snackbar

// Data class to represent an item in the list
data class SelectableItem(
    val id: Int,
    val text: String,
    var isSelected: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class) // For Scaffold and SnackbarHost
@Composable
fun SelectableListScreen(initialItems: List<SelectableItem>) {
    // Remember the state of the items to allow for recomposition
    var rememberedItems by remember { mutableStateOf(initialItems) }
    // State for the text input field
    var newItemText by remember { mutableStateOf("") }
    // State for managing Snackbar visibility
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Counter to generate unique IDs for new items
    var nextItemId by remember { mutableStateOf(initialItems.maxOfOrNull { it.id }?.plus(1) ?: 1) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text("Shopping List") }) // Optional: Add a title
        },
        bottomBar = {
            NewItemInputRow(
                newItemText = newItemText,
                onNewItemTextChange = { newItemText = it },
                onAddItemClick = {
                    if (newItemText.isNotBlank()) {
                        val newItem = SelectableItem(id = nextItemId++, text = newItemText)
                        rememberedItems = rememberedItems + newItem // Add to the list
                        newItemText = "" // Clear the input field
                        scope.launch {
                            snackbarHostState.showSnackbar("Item added: ${newItem.text}")
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
                .padding(paddingValues) // Apply padding from Scaffold
                .padding(horizontal = 16.dp), // Additional horizontal padding for list items
            contentPadding = PaddingValues(vertical = 16.dp) // Padding for top and bottom of the list
        ) {
            items(rememberedItems, key = { it.id }) { item ->
                SelectableListItem(
                    item = item,
                    onItemSelected = { newItemState ->
                        rememberedItems = rememberedItems.map {
                            if (it.id == newItemState.id) newItemState else it
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
    Surface(shadowElevation = 4.dp) { // Add some elevation to the input row
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
    onItemSelected: (SelectableItem) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
            style = MaterialTheme.typography.bodyLarge
        )
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