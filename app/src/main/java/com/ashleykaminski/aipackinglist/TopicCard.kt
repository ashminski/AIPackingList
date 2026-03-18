package com.ashleykaminski.aipackinglist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun TopicCard(
    topic: TripTopic,
    isSelected: Boolean,
    onToggleSelected: () -> Unit,
    onEditTopic: () -> Unit,
    onRename: (newName: String) -> Unit,
    onDelete: () -> Unit
) {
    var isEditing by rememberSaveable { mutableStateOf(false) }
    var editableName by rememberSaveable(topic.name) { mutableStateOf(topic.name) }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(topic.name) {
        if (!isEditing) {
            editableName = topic.name
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = isSelected, onCheckedChange = { onToggleSelected() })
            if (isEditing) {
                OutlinedTextField(
                    value = editableName,
                    onValueChange = { editableName = it },
                    label = { Text("Topic Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (editableName.isNotBlank()) onRename(editableName.trim())
                        isEditing = false
                        keyboardController?.hide()
                    }),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    if (editableName.isNotBlank()) onRename(editableName.trim())
                    isEditing = false
                    keyboardController?.hide()
                }) {
                    Icon(Icons.Filled.Done, contentDescription = "Save name")
                }
                IconButton(onClick = {
                    isEditing = false
                    editableName = topic.name
                    keyboardController?.hide()
                }) {
                    Icon(Icons.Filled.Close, contentDescription = "Cancel edit")
                }
            } else {
                Text(
                    text = topic.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onToggleSelected)
                )
                Text("${topic.items.size}")
                IconButton(onClick = { isEditing = true; editableName = topic.name }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Rename topic")
                }
                IconButton(onClick = onEditTopic) {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Edit topic items")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete topic")
                }
            }
        }
    }
}
