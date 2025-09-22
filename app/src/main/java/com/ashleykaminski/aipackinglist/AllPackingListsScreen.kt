package com.ashleykaminski.aipackinglist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // Correct import for LazyColumn items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Originally from MainActivity.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllPackingListsScreen(
    packingLists: List<PackingList>,
    onSelectList: (Int) -> Unit,
    onAddNewList: () -> Unit,
    onRenameList: (listId: Int, newName: String) -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("My Packing Lists") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddNewList) {
                Icon(Icons.Filled.Add, contentDescription = "Add new packing list")
            }
        }
    ) { paddingValues ->
        if (packingLists.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No packing lists yet. Tap '+' to add one!")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(packingLists, key = { it.id }) { list ->
                    PackingListCard(
                        list = list,
                        onClick = { onSelectList(list.id) },
                        onRename = { newName -> onRenameList(list.id, newName) } // Pass handler
                    )
                }
            }
        }
    }
}
