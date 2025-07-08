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

// Data class to represent an item in the list
data class SelectableItem(
    val id: Int,
    val text: String,
    var isSelected: Boolean = false
)

@Composable
fun SelectableListScreen(items: List<SelectableItem>) {
    // Remember the state of the items to allow for recomposition when an item is selected
    var rememberedItems by remember { mutableStateOf(items) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(rememberedItems, key = { it.id }) { item ->
            SelectableListItem(
                item = item,
                onItemSelected = { newItemState ->
                    // Update the list with the new item state
                    rememberedItems = rememberedItems.map {
                        if (it.id == newItemState.id) newItemState else it
                    }
                }
            )
            Divider() // Optional: Adds a line between items
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
fun DefaultPreviewOfSelectableListScreen() {
    val sampleItems = listOf(
        SelectableItem(1, "Item 1"),
        SelectableItem(2, "Item 2", isSelected = true),
        SelectableItem(3, "Item 3")
    )
    MaterialTheme { // Ensure a MaterialTheme is applied for preview
        SelectableListScreen(items = sampleItems)
    }
}