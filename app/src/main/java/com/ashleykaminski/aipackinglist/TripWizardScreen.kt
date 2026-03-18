package com.ashleykaminski.aipackinglist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripWizardScreen(
    questions: List<TripQuestion>,
    onCreateList: (topicIds: List<Int>, listName: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var answers by remember { mutableStateOf(mapOf<Int, Boolean>()) }
    var showNameDialog by remember { mutableStateOf(false) }
    var listName by remember { mutableStateOf("My Trip") }

    val allAnswered = questions.all { it.id in answers }
    val activatedTopicIds = questions
        .filter { answers[it.id] == true }
        .flatMap { it.topicIds }
        .distinct()

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Name your list") },
            text = {
                OutlinedTextField(
                    value = listName,
                    onValueChange = { listName = it },
                    label = { Text("List name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (listName.isNotBlank()) {
                            onCreateList(activatedTopicIds, listName.trim())
                            showNameDialog = false
                        }
                    },
                    enabled = listName.isNotBlank()
                ) {
                    Text("Build List")
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
                title = { Text("Plan Your Trip") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(questions, key = { it.id }) { question ->
                    val answer = answers[question.id]
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = question.text,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { answers = answers + (question.id to true) },
                                    modifier = Modifier.weight(1f),
                                    colors = if (answer == true) ButtonDefaults.outlinedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    ) else ButtonDefaults.outlinedButtonColors()
                                ) {
                                    Text("YES")
                                }
                                OutlinedButton(
                                    onClick = { answers = answers + (question.id to false) },
                                    modifier = Modifier.weight(1f),
                                    colors = if (answer == false) ButtonDefaults.outlinedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    ) else ButtonDefaults.outlinedButtonColors()
                                ) {
                                    Text("NO")
                                }
                            }
                        }
                    }
                }
            }
            Button(
                onClick = { showNameDialog = true },
                enabled = allAnswered,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Build My List")
            }
        }
    }
}
