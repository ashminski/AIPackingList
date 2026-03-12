package com.ashleykaminski.aipackinglist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TemplateDetailScreen(
    template: PackingListTemplate,
    onUpdateItems: (List<TemplateItem>) -> Unit,
    generateItemId: () -> Int,
    onNavigateBack: () -> Unit,
    onRenameTitle: (newName: String) -> Unit
) {
    var rememberedItems by remember(template.items) { mutableStateOf(template.items) }
    var newItemText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(rememberedItems) {
        if (rememberedItems != template.items) {
            onUpdateItems(rememberedItems)
        }
    }

    var isEditingTitle by rememberSaveable { mutableStateOf(false) }
    var editableTitle by rememberSaveable(template.name) { mutableStateOf(template.name) }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(template.name) {
        if (!isEditingTitle) {
            editableTitle = template.name
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
                            label = { Text("Template Name") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (editableTitle.isNotBlank()) onRenameTitle(editableTitle.trim())
                                isEditingTitle = false
                                keyboardController?.hide()
                            }),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(template.name)
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
                            if (editableTitle.isNotBlank()) onRenameTitle(editableTitle.trim())
                            isEditingTitle = false
                            keyboardController?.hide()
                        }) {
                            Icon(Icons.Filled.Done, contentDescription = "Save title")
                        }
                        IconButton(onClick = {
                            isEditingTitle = false
                            editableTitle = template.name
                            keyboardController?.hide()
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel edit title")
                        }
                    } else {
                        IconButton(onClick = {
                            editableTitle = template.name
                            isEditingTitle = true
                        }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit template title")
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
                        val newId = generateItemId()
                        val newItem = TemplateItem(id = newId, text = newItemText)
                        rememberedItems = rememberedItems + newItem
                        newItemText = ""
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
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPaddingValues)
                .padding(horizontal = 16.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {},
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(rememberedItems, key = { it.id }) { item ->
                TemplateItemRow(
                    item = item,
                    onDeleteItem = { itemToDelete ->
                        rememberedItems = rememberedItems - itemToDelete
                        scope.launch {
                            snackbarHostState.showSnackbar("Item deleted: ${itemToDelete.text}")
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }
}
