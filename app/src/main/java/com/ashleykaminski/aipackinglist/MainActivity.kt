// MainActivity.kt or a new App.kt
package com.ashleykaminski.aipackinglist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.copy
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // Correct import for LazyColumn items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ashleykaminski.aipackinglist.SelectableItem
import com.ashleykaminski.aipackinglist.SelectableListScreen
import com.ashleykaminski.aipackinglist.ui.theme.AIPackingListTheme
import kotlinx.coroutines.launch // For Snackbar

// --- Data Classes (SelectableItem, PackingList) ---
// New data class to represent a Packing List
data class PackingList(
    val id: Int,
    val name: String,
    val items: List<SelectableItem> = emptyList() // Each packing list has its own items
)
// (Already defined above, ensure they are in scope)

// --- Screen States for Navigation ---
sealed class Screen {
    object PackingListsScreen : Screen()
    data class ItemsScreen(val listId: Int) : Screen()
}

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AIPackingListTheme {
                PackingListApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackingListApp() {
    var packingLists by remember { mutableStateOf<List<PackingList>>(emptyList()) }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.PackingListsScreen) }
    var nextPackingListId by remember { mutableStateOf(1) }
    var nextItemId by remember { mutableStateOf(1) } // Global item ID counter for simplicity

    // Function to update a specific packing list's items
    val updatePackingListItems: (Int, List<SelectableItem>) -> Unit = { listId, newItems ->
        packingLists = packingLists.map {
            if (it.id == listId) it.copy(items = newItems) else it
        }
    }

    // Function to generate unique item IDs for a given list
    // More robust ID generation might be needed for complex apps
    val generateNextItemId: () -> Int = { nextItemId++ }


    Crossfade(targetState = currentScreen, label = "screen_crossfade") { screen ->
        when (screen) {
            is Screen.PackingListsScreen -> {
                AllPackingListsScreen(
                    packingLists = packingLists,
                    onSelectList = { listId -> currentScreen = Screen.ItemsScreen(listId) },
                    onAddNewList = {
                        val newList = PackingList(id = nextPackingListId++, name = "Packing List ${nextPackingListId -1 }")
                        packingLists = packingLists + newList
                        currentScreen = Screen.ItemsScreen(newList.id) // Navigate to the new list
                    }
                )
            }
            is Screen.ItemsScreen -> {
                val list = packingLists.find { it.id == screen.listId }
                if (list != null) {
                    // Pass a lambda to generate item IDs specific to this list context
                    SelectableListScreen(
                        packingList = list,
                        onUpdateItems = { updatedItems ->
                            updatePackingListItems(list.id, updatedItems)
                        },
                        generateItemId = generateNextItemId, // Pass the ID generator
                        onNavigateBack = { currentScreen = Screen.PackingListsScreen }
                    )
                } else {
                    // Handle case where list is not found (e.g., navigate back or show error)
                    Text("Error: List not found. Navigating back.")
                    LaunchedEffect(Unit) {
                        currentScreen = Screen.PackingListsScreen
                    }
                }
            }

            else -> {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllPackingListsScreen(
    packingLists: List<PackingList>,
    onSelectList: (Int) -> Unit,
    onAddNewList: () -> Unit
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
                    PackingListCard(list = list, onClick = { onSelectList(list.id) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackingListCard(list: PackingList, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(list.name, style = MaterialTheme.typography.titleMedium)
            Text("${list.items.count { it.isSelected }}/${list.items.size} packed")
        }
    }
}