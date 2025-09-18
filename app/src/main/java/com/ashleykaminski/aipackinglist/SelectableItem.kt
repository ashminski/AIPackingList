package com.ashleykaminski.aipackinglist

import android.util.Log
import androidx.compose.animation.core.copy
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
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
    packingList: PackingList, // Now contains the name
    onUpdateItems: (List<SelectableItem>) -> Unit,
    generateItemId: () -> Int,
    onNavigateBack: () -> Unit,
    onRenameListTitle: (newName: String) -> Unit // New handler
) {
    // Initialize rememberedItems from the items within the passed packingList
    var rememberedItems by remember(packingList.items) { mutableStateOf(packingList.items) }
    var newItemText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 1. Create and remember the LazyListState
    val listState = rememberLazyListState()

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

    var isEditingTitle by rememberSaveable { mutableStateOf(false) }
    var editableTitle by rememberSaveable(packingList.name) { mutableStateOf(packingList.name) }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(packingList.name) {
        if (!isEditingTitle) {
            editableTitle = packingList.name
        }
    }

    Scaffold(
        // Modifier.fillMaxSize() on Scaffold is usually good.
        // Let's ensure the Scaffold itself is not consuming IME insets meant for specific children.
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    if (isEditingTitle) {
                        OutlinedTextField(
                            value = editableTitle,
                            onValueChange = { editableTitle = it },
                            label = { Text("List Name") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (editableTitle.isNotBlank()) {
                                    onRenameListTitle(editableTitle.trim())
                                }
                                isEditingTitle = false
                                keyboardController?.hide()
                            }),
                            modifier = Modifier.fillMaxWidth() // Changed from .weight(1f)
                        )
                    } else {
                        Text(packingList.name) // Display current list name
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEditingTitle) {
                        IconButton(onClick = {
                            if (editableTitle.isNotBlank()) {
                                onRenameListTitle(editableTitle.trim())
                            }
                            isEditingTitle = false
                            keyboardController?.hide()
                        }) {
                            Icon(Icons.Filled.Done, contentDescription = "Save title")
                        }
                        IconButton(onClick = {
                            isEditingTitle = false
                            editableTitle = packingList.name // Reset
                            keyboardController?.hide()
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel edit title")
                        }
                    } else {
                        IconButton(onClick = {
                            editableTitle = packingList.name // Initialize for editing
                            isEditingTitle = true
                        }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit list title")
                        }
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
                        val newItemId = generateItemId()
                        val newItemToAdd = SelectableItem(id = newItemId, text = newItemText) // Renamed to avoid confusion
                        val oldNewItemText = newItemText

                        // Create the next version of the items list
                        val nextRememberedItems = rememberedItems + newItemToAdd
                        rememberedItems = nextRememberedItems // Update the state

                        newItemText = ""
                        activeItemIdForDelete = null

                        scope.launch {
                            snackbarHostState.showSnackbar("Item added: ${oldNewItemText}")
                        }

                        scope.launch {
                            // Now, sort THIS 'nextRememberedItems' list to find the index
                            // This guarantees you're looking at the list that includes the new item.
                            val itemsAfterAddAndSorted = nextRememberedItems.sortedWith(compareBy { it.isSelected })
                            val newIndex = itemsAfterAddAndSorted.indexOfFirst { it.id == newItemToAdd.id }

                            Log.d("ScrollDebug", "Attempting to scroll. New item ID: ${newItemToAdd.id}, Text: ${newItemToAdd.text}")
                            Log.d("ScrollDebug", "Items after add and sort count: ${itemsAfterAddAndSorted.size}")
                            Log.d("ScrollDebug", "Found index for new item: $newIndex")

                            if (newIndex != -1) {
                                listState.animateScrollToItem(index = newIndex)
                                Log.d("ScrollDebug", "animateScrollToItem called for index $newIndex")
                            } else {
                                // This block should ideally not be hit now if IDs are unique and item is added.
                                Log.e("ScrollDebug", "CRITICAL: New item ID ${newItemToAdd.id} NOT FOUND after manual sort. This shouldn't happen.")
                                if (itemsAfterAddAndSorted.isNotEmpty()) {
                                    // Fallback to end of the *newly sorted* list
                                    listState.animateScrollToItem(index = itemsAfterAddAndSorted.size - 1)
                                }
                            }
                        }
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar("Please enter item text")
                        }
                    }
                },
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding()
            )
        }
    ) { scaffoldPaddingValues -> // These are from Scaffold (for system bars, top/bottom bars)
        LazyColumn(
            state = listState, // 2. Pass the state to LazyColumn
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