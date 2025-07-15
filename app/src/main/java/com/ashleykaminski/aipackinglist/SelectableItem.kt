package com.ashleykaminski.aipackinglist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import kotlinx.serialization.Serializable

// Data class to represent an item in the list
@Serializable
data class SelectableItem(
    val id: Int,
    val text: String,
    var isSelected: Boolean = false // For the checkbox
)

@OptIn(ExperimentalMaterial3Api::class,  ExperimentalLayoutApi::class) // Need ExperimentalLayoutApi for WindowInsets)
@Composable
fun SelectableListScreen(
    packingList: PackingList, // Now takes a full PackingList
    onUpdateItems: (List<SelectableItem>) -> Unit, // Callback to update items in the parent
    generateItemId: () -> Int, // Function to get next item ID
    onNavigateBack: () -> Unit
) {
    // Initialize rememberedItems from the items within the passed packingList
    var rememberedItems by remember(packingList.items) { mutableStateOf(packingList.items) }
    var newItemText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // The generateItemId function is now passed in, so we don't need to manage nextItemId locally here.
    // We'll call generateItemId() when we need a new ID for an item.

    var activeItemIdForDelete by remember { mutableStateOf<Int?>(null) }

    // Derived state for sorted items
    val sortedItems by remember(rememberedItems) {
        derivedStateOf {
            rememberedItems.sortedWith(compareBy { it.isSelected })
        }
    }

    // When items are added or updated, call onUpdateItems to propagate changes
    // This effect ensures the parent's state (in PackingListApp) is updated
    // whenever rememberedItems changes locally.
    LaunchedEffect(rememberedItems) {
        if (rememberedItems != packingList.items) { // Only update if there's an actual change
            onUpdateItems(rememberedItems)
        }
    }

    Scaffold(
        // Modifier.fillMaxSize() on Scaffold is usually good.
        // Let's ensure the Scaffold itself is not consuming IME insets meant for specific children.
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(packingList.name) }, // Use the packing list's name
                navigationIcon = { // Add a back button
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate back")
                    }
                }
            )
        },
        bottomBar = {
            NewItemInputRow(
                newItemText = newItemText,
                onNewItemTextChange = { newItemText = it },
                onAddItemClick = {
                    if (newItemText.isNotBlank()) {
                        // Use the passed-in generateItemId function
                        val newItem = SelectableItem(id = generateItemId(), text = newItemText)
                        val oldNewItemText = newItemText

                        rememberedItems = (rememberedItems + newItem)
                            .sortedWith(compareBy { it.isSelected })

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
                },
                // Apply IME padding *directly to the BottomAppBar content*
                // This will push the content of NewItemInputRow up when the keyboard appears.
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding() // ADD THIS
            )
        }
    ) { scaffoldPaddingValues -> // These are from Scaffold (for system bars, top/bottom bars)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                // 1. Apply the padding from Scaffold. This accounts for system bars,
                //    TopAppBar, and the space reserved for the BottomAppBar.
                .padding(scaffoldPaddingValues)
                // 2. IMPORTANT: Do NOT apply .imePadding() here on the LazyColumn directly
                //    if the TextField is in the BottomAppBar. If the TextField were *inside*
                //    the LazyColumn, then imePadding() here would be correct for that case.
                //    Since the TextField is in the bottomBar, the bottomBar itself needs to react.
                .padding(horizontal = 16.dp) // Your specific horizontal padding
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null // Optional: remove ripple if you only want to clear focus
                ) {
                    activeItemIdForDelete = null
                    // Consider also clearing keyboard focus here if needed
                    // LocalSoftwareKeyboardController.current?.hide()
                },
            contentPadding = PaddingValues(vertical = 16.dp) // Inner content padding
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
    onAddItemClick: () -> Unit,
    modifier: Modifier = Modifier // Add the modifier parameter here

) {
    Surface(
        modifier = modifier, // Apply the passed-in modifier to the Surface
        shadowElevation = 4.dp) {
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