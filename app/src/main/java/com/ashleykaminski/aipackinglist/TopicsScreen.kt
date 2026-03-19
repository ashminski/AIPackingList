package com.ashleykaminski.aipackinglist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicsScreen(
    topics: List<TripTopic>,
    onSelectTopic: (Int) -> Unit,
    onAddNewTopic: () -> Unit,
    onDeleteTopic: (Int) -> Unit,
    onCreateListFromTopics: (topicIds: List<Int>, listName: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var selectedTopicIds by remember { mutableStateOf(setOf<Int>()) }
    var showNameDialog by remember { mutableStateOf(false) }
    var listName by remember { mutableStateOf("") }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Name your list") },
            text = {
                OutlinedTextField(
                    value = listName,
                    onValueChange = { listName = it },
                    label = { Text("List name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Sentences)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (listName.isNotBlank()) {
                            onCreateListFromTopics(selectedTopicIds.toList(), listName.trim())
                            showNameDialog = false
                            listName = ""
                            selectedTopicIds = emptySet()
                        }
                    },
                    enabled = listName.isNotBlank()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Topics") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddNewTopic) {
                Icon(Icons.Filled.Add, contentDescription = "Add new topic")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (selectedTopicIds.isNotEmpty()) {
                Button(
                    onClick = {
                        listName = ""
                        showNameDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("Create list from ${selectedTopicIds.size} selected topic(s)")
                }
            }
            if (topics.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No topics yet. Tap '+' to create one!")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(topics, key = { it.id }) { topic ->
                        TopicCard(
                            topic = topic,
                            isSelected = topic.id in selectedTopicIds,
                            onToggleSelected = {
                                selectedTopicIds = if (topic.id in selectedTopicIds) {
                                    selectedTopicIds - topic.id
                                } else {
                                    selectedTopicIds + topic.id
                                }
                            },
                            onEditTopic = { onSelectTopic(topic.id) },
                            onDelete = { onDeleteTopic(topic.id) }
                        )
                    }
                }
            }
        }
    }
}
