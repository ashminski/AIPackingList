package com.ashleykaminski.aipackinglist

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun TemplateCard(
    template: PackingListTemplate,
    onClick: () -> Unit,
    onRename: (newName: String) -> Unit,
    onUseTemplate: () -> Unit,
    onDelete: () -> Unit
) {
    var isEditing by rememberSaveable { mutableStateOf(false) }
    var editableName by rememberSaveable(template.name) { mutableStateOf(template.name) }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(template.name) {
        if (!isEditing) {
            editableName = template.name
        }
    }

    Card(
        onClick = { if (!isEditing) onClick() },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isEditing) {
                    OutlinedTextField(
                        value = editableName,
                        onValueChange = { editableName = it },
                        label = { Text("Template Name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done, capitalization = KeyboardCapitalization.Sentences),
                        keyboardActions = KeyboardActions(onDone = {
                            if (editableName.isNotBlank()) onRename(editableName.trim())
                            isEditing = false
                            keyboardController?.hide()
                        }),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = {
                        if (editableName.isNotBlank()) onRename(editableName.trim())
                        isEditing = false
                        keyboardController?.hide()
                    }) {
                        Icon(Icons.Filled.Done, contentDescription = "Save name")
                    }
                    IconButton(onClick = {
                        isEditing = false
                        editableName = template.name
                        keyboardController?.hide()
                    }) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel edit")
                    }
                } else {
                    Text(
                        template.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onClick)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = {
                        editableName = template.name
                        isEditing = true
                    }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit template name")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete template")
                    }
                    Text("${template.items.size} items")
                }
            }
            if (!isEditing) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onUseTemplate,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Use this template")
                }
            }
        }
    }
}
