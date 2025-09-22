package com.ashleykaminski.aipackinglist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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

// Originally from MainActivity.kt
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun PackingListCard(
    list: PackingList,
    onClick: () -> Unit,
    onRename: (newName: String) -> Unit
) {
    var isEditing by rememberSaveable { mutableStateOf(false) }
    var editableName by rememberSaveable(list.name) { mutableStateOf(list.name) }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(list.name) {
        if (!isEditing) { // If not editing, ensure editableName is up-to-date with list.name
            editableName = list.name
        }
    }

    Card(
        // Make the entire card clickable to navigate, except when editing
        onClick = { if (!isEditing) onClick() },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
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
                        if (editableName.isNotBlank()) {
                            onRename(editableName.trim())
                        }
                        isEditing = false
                        keyboardController?.hide()
                    }),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = {
                    if (editableName.isNotBlank()) {
                        onRename(editableName.trim())
                    }
                    isEditing = false
                    keyboardController?.hide()
                }) {
                    Icon(Icons.Filled.Done, contentDescription = "Save name")
                }
                IconButton(onClick = {
                    isEditing = false
                    editableName = list.name // Reset to original name
                    keyboardController?.hide()
                }) {
                    Icon(Icons.Filled.Close, contentDescription = "Cancel edit")
                }
            } else {
                Text(
                    list.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onClick) // Make text itself clickable too
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = {
                    editableName = list.name // Initialize editableName with current list name
                    isEditing = true
                }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit list name")
                }
                Text("${list.items.count { it.isSelected }}/${list.items.size} packed")
            }
        }
    }
}
