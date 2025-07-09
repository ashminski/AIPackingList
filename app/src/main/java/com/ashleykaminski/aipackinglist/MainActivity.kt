// MainActivity.kt or a new App.kt
package com.ashleykaminski.aipackinglist

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.datastore.dataStore
import com.ashleykaminski.aipackinglist.SelectableItem
import com.ashleykaminski.aipackinglist.SelectableListScreen
import com.ashleykaminski.aipackinglist.ui.theme.AIPackingListTheme
import kotlinx.coroutines.launch // For Snackbar
import kotlinx.serialization.Serializable
import androidx.lifecycle.viewmodel.compose.viewModel

// --- Data Classes (SelectableItem, PackingList) ---
// New data class to represent a Packing List
@Serializable
data class PackingList(
    val id: Int,
    val name: String,
    val items: List<SelectableItem> = emptyList() // Each packing list has its own items
)
// A wrapper class for your list of PackingLists, useful for DataStore
@Serializable
data class UserPreferences(
    val packingLists: List<PackingList> = emptyList(),
    val nextPackingListId: Int = 1,
    val nextItemId: Int = 1
)

// (Already defined above, ensure they are in scope)

// --- Screen States for Navigation ---
sealed class Screen {
    object PackingListsScreen : Screen()
    data class ItemsScreen(val listId: Int) : Screen()
}

// Create the DataStore instance
val Context.userPreferencesDataStore: androidx.datastore.core.DataStore<UserPreferences> by dataStore(
    fileName = "user_prefs.pb", // File name for storing the data
    serializer = UserPreferencesSerializer
)

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

// MainActivity.kt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackingListApp(viewModel: PackingListViewModel = viewModel(factory = PackingListViewModelFactory(LocalContext.current))) {
    // Collect the entire UserPreferences state
    val userPreferences by viewModel.userPreferencesFlow.collectAsState()

    // Now use userPreferences.packingLists, userPreferences.nextPackingListId, etc.
    // instead of local remember states for this top-level data.
    var currentScreen by remember { mutableStateOf<Screen>(Screen.PackingListsScreen) }

    // --- Update Callbacks ---
    // Example: When adding a new list
    val addNewListHandler = {
        val idForThisList = userPreferences.nextPackingListId // ID for the current new list
        val newNextPackingListIdToStore = userPreferences.nextPackingListId + 1 // ID for the *next* list

        val newList = PackingList(
            id = idForThisList, // Use the captured ID
            name = "Packing List ${idForThisList}" // Use the captured ID
        )
        val updatedLists = userPreferences.packingLists + newList

        viewModel.saveUserPreferences(
            userPreferences.copy(
                packingLists = updatedLists,
                nextPackingListId = newNextPackingListIdToStore // Store the incremented ID
            )
        )
        currentScreen = Screen.ItemsScreen(newList.id)
    }

    // Example: When updating items in a specific list
    val updateItemsHandler: (Int, List<SelectableItem>) -> Unit = { listId, newItems ->
        val updatedGlobalLists = userPreferences.packingLists.map {
            if (it.id == listId) it.copy(items = newItems) else it
        }
        viewModel.saveUserPreferences(
            userPreferences.copy(packingLists = updatedGlobalLists)
        )
    }

    // Example: When generating a new item ID
    val generateItemIdHandler: () -> Int = {
        val idToUse = userPreferences.nextItemId // Get the current ID to use for *this* item
        val newNextItemIdToStore = userPreferences.nextItemId + 1 // Calculate the ID for the *next* item
        viewModel.updateNextItemId(newNextItemIdToStore) // Tell ViewModel to store the incremented ID
        idToUse // Return the ID that was just allocated for the current item
    }


    Crossfade(targetState = currentScreen, label = "screen_crossfade") { screen ->
        when (screen) {
            is Screen.PackingListsScreen -> {
                AllPackingListsScreen(
                    packingLists = userPreferences.packingLists, // Use from ViewModel
                    onSelectList = { listId -> currentScreen = Screen.ItemsScreen(listId) },
                    onAddNewList = addNewListHandler
                )
            }
            is Screen.ItemsScreen -> {
                val list = userPreferences.packingLists.find { it.id == screen.listId }
                if (list != null) {
                    SelectableListScreen(
                        packingList = list, // Pass the specific list
                        onUpdateItems = { updatedItems ->
                            updateItemsHandler(list.id, updatedItems)
                        },
                        generateItemId = generateItemIdHandler, // Pass the updated ID generator
                        onNavigateBack = { currentScreen = Screen.PackingListsScreen }
                    )
                } else {
                    Text("Error: List not found. Navigating back.")
                    LaunchedEffect(Unit) {
                        currentScreen = Screen.PackingListsScreen
                    }
                }
            }
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