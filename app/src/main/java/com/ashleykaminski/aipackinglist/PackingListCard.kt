package com.ashleykaminski.aipackinglist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun PackingListCard(
    list: PackingList,
    onClick: () -> Unit,
    onRename: (newName: String) -> Unit,
    onDelete: () -> Unit
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    var isEditing by rememberSaveable { mutableStateOf(false) }
    var editableName by rememberSaveable(list.name) { mutableStateOf(list.name) }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(list.name) {
        if (!isEditing) {
            editableName = list.name
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        when {
                            isEditing -> {}
                            isExpanded -> isExpanded = false
                            else -> onClick()
                        }
                    },
                    onLongClick = { if (!isEditing) isExpanded = true }
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (isEditing) {
                OutlinedTextField(
                    value = editableName,
                    onValueChange = { editableName = it },
                    label = { Text("List Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (editableName.isNotBlank()) onRename(editableName.trim())
                        isEditing = false
                        isExpanded = false
                        keyboardController?.hide()
                    }),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = {
                    if (editableName.isNotBlank()) onRename(editableName.trim())
                    isEditing = false
                    isExpanded = false
                    keyboardController?.hide()
                }) {
                    Icon(Icons.Filled.Done, contentDescription = "Save name")
                }
                IconButton(onClick = {
                    isEditing = false
                    isExpanded = false
                    editableName = list.name
                    keyboardController?.hide()
                }) {
                    Icon(Icons.Filled.Close, contentDescription = "Cancel edit")
                }
            } else if (isExpanded) {
                Text(
                    list.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = {
                    editableName = list.name
                    isEditing = true
                }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Rename list")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete list")
                }
            } else {
                Text(
                    list.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text("${list.items.count { it.isSelected }}/${list.items.size} packed")
            }
        }
    }
}
