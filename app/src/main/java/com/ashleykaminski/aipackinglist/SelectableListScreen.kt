package com.ashleykaminski.aipackinglist

import android.util.Log
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SelectableListScreen(
    packingList: PackingList,
    onUpdateItems: (List<SelectableItem>) -> Unit,
    generateItemId: () -> Int,
    onNavigateBack: () -> Unit,
    onRenameListTitle: (newName: String) -> Unit,
    onDeleteList: () -> Unit
) {
    var rememberedItems by remember(packingList.items) { mutableStateOf(packingList.items) }
    var newItemText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val listState = rememberLazyListState()

    var activeItemIdForDelete by remember { mutableStateOf<Int?>(null) }

    val isGrouped = rememberedItems.any { it.topicName != null }

    // Flat sorted items (used when not grouped)
    val sortedItems by remember(rememberedItems) {
        derivedStateOf {
            rememberedItems.sortedWith(compareBy { it.isSelected })
        }
    }

    // Grouped sections (used when grouped)
    val sections by remember(rememberedItems) {
        derivedStateOf {
            val topicMap = linkedMapOf<String, MutableList<SelectableItem>>()
            val myItems = mutableListOf<SelectableItem>()
            for (item in rememberedItems) {
                if (item.topicName != null) topicMap.getOrPut(item.topicName) { mutableListOf() }.add(item)
                else myItems.add(item)
            }
            val result = topicMap.map { (name, sectionItems) ->
                name to sectionItems.sortedWith(compareBy { it.isSelected })
            }.toMutableList()
            if (myItems.isNotEmpty())
                result.add("My Items" to myItems.sortedWith(compareBy { it.isSelected }))
            result.toList()
        }
    }

    var collapsedSections by rememberSaveable { mutableStateOf(emptySet<String>()) }

    LaunchedEffect(rememberedItems) {
        if (rememberedItems != packingList.items) {
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
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(packingList.name)
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
                            editableTitle = packingList.name
                            keyboardController?.hide()
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel edit title")
                        }
                    } else {
                        IconButton(onClick = {
                            editableTitle = packingList.name
                            isEditingTitle = true
                        }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit list title")
                        }
                        IconButton(onClick = onDeleteList) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete list")
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
                        val newItemToAdd = SelectableItem(id = newItemId, text = newItemText)
                        val oldNewItemText = newItemText

                        val nextRememberedItems = rememberedItems + newItemToAdd
                        rememberedItems = nextRememberedItems

                        newItemText = ""
                        activeItemIdForDelete = null

                        scope.launch {
                            snackbarHostState.showSnackbar("Item added: ${oldNewItemText}")
                        }

                        scope.launch {
                            val itemsAfterAddAndSorted = nextRememberedItems.sortedWith(compareBy { it.isSelected })
                            val newIndex = itemsAfterAddAndSorted.indexOfFirst { it.id == newItemToAdd.id }

                            Log.d("ScrollDebug", "Attempting to scroll. New item ID: ${newItemToAdd.id}, Text: ${newItemToAdd.text}")
                            Log.d("ScrollDebug", "Items after add and sort count: ${itemsAfterAddAndSorted.size}")
                            Log.d("ScrollDebug", "Found index for new item: $newIndex")

                            if (newIndex != -1) {
                                listState.animateScrollToItem(index = newIndex)
                                Log.d("ScrollDebug", "animateScrollToItem called for index $newIndex")
                            } else {
                                Log.e("ScrollDebug", "CRITICAL: New item ID ${newItemToAdd.id} NOT FOUND after manual sort. This shouldn't happen.")
                                if (itemsAfterAddAndSorted.isNotEmpty()) {
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
    ) { scaffoldPaddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPaddingValues)
                .padding(horizontal = 16.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    activeItemIdForDelete = null
                },
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            if (isGrouped) {
                sections.forEach { (sectionName, sectionItems) ->
                    val isExpanded = sectionName !in collapsedSections
                    val checkedCount = sectionItems.count { it.isSelected }
                    item(key = "header_$sectionName") {
                        SectionHeader(
                            name = sectionName,
                            checkedCount = checkedCount,
                            total = sectionItems.size,
                            isExpanded = isExpanded,
                            onToggle = {
                                collapsedSections = if (isExpanded)
                                    collapsedSections + sectionName
                                else
                                    collapsedSections - sectionName
                            }
                        )
                    }
                    if (isExpanded) {
                        items(sectionItems, key = { it.id }) { item ->
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
            } else {
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
}

@Composable
private fun SectionHeader(
    name: String,
    checkedCount: Int,
    total: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Text("$checkedCount/$total", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.width(8.dp))
        Icon(
            if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = if (isExpanded) "Collapse" else "Expand"
        )
    }
    HorizontalDivider()
}
