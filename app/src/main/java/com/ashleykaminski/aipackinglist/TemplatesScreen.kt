package com.ashleykaminski.aipackinglist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    templates: List<PackingListTemplate>,
    onSelectTemplate: (Int) -> Unit,
    onAddNewTemplate: () -> Unit,
    onRenameTemplate: (templateId: Int, newName: String) -> Unit,
    onUseTemplate: (Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Templates") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddNewTemplate) {
                Icon(Icons.Filled.Add, contentDescription = "Add new template")
            }
        }
    ) { paddingValues ->
        if (templates.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No templates yet. Tap '+' to create one!")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(templates, key = { it.id }) { template ->
                    TemplateCard(
                        template = template,
                        onClick = { onSelectTemplate(template.id) },
                        onRename = { newName -> onRenameTemplate(template.id, newName) },
                        onUseTemplate = { onUseTemplate(template.id) }
                    )
                }
            }
        }
    }
}
