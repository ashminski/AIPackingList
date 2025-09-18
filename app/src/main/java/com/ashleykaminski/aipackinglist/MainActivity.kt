// MainActivity.kt or a new App.kt
package com.ashleykaminski.aipackinglist

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.copy
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // Correct import for LazyColumn items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.datastore.dataStore
import com.ashleykaminski.aipackinglist.SelectableItem
import com.ashleykaminski.aipackinglist.SelectableListScreen
import com.ashleykaminski.aipackinglist.ui.theme.AIPackingListTheme
import kotlinx.coroutines.launch // For Snackbar
import kotlinx.serialization.Serializable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.platform.LocalSoftwareKeyboardController // For hiding keyboard


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
    object PackingListsScreen : Screen() // This is an object
    data class ItemsScreen(val listId: Int) : Screen()

    companion object {
        // Define String constants for screen types to avoid typos
        private const val TYPE_PACKING_LISTS = "PackingListsScreen"
        private const val TYPE_ITEMS = "ItemsScreen"
        private const val KEY_LIST_ID = "listId"

        val Saver: Saver<Screen, Any> = Saver(
            save = { screen ->
                when (screen) {
                    is PackingListsScreen -> mapOf("type" to TYPE_PACKING_LISTS) // Value is String
                    is ItemsScreen -> mapOf("type" to TYPE_ITEMS, KEY_LIST_ID to screen.listId) // Values are String, Int
                }
            },
            restore = { savedValue ->
                // savedValue will be the Map from above, which is 'Any' but we expect a Map
                val map = savedValue as? Map<String, Any> ?: return@Saver null
                when (map["type"] as? String) {
                    TYPE_PACKING_LISTS -> PackingListsScreen
                    TYPE_ITEMS -> {
                        val listId = map[KEY_LIST_ID] as? Int
                        if (listId != null) {
                            ItemsScreen(listId)
                        } else {
                            PackingListsScreen // Fallback
                        }
                    }
                    else -> null // Fallback or throw for unknown type
                }
            }
        )
    }
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
    val userPreferences by viewModel.userPreferencesFlow.collectAsState()
    var currentScreen by rememberSaveable(stateSaver = Screen.Saver) {
        mutableStateOf<Screen>(Screen.PackingListsScreen)
    }

    val addNewListHandler = {
        // These are calculated based on the *current* state from userPreferencesFlow
        val idForThisList = userPreferences.nextPackingListId
        val newNextPackingListIdToStore = userPreferences.nextPackingListId + 1

        val newList = PackingList(
            id = idForThisList,
            name = "Packing List ${idForThisList}"
            // items will default to emptyList as per PackingList definition
        )

        // Call the ViewModel function, passing the new list and the next ID to store
        viewModel.addNewListAndUpdateIds(newList, newNextPackingListIdToStore)

        currentScreen = Screen.ItemsScreen(newList.id) // Navigate after initiating the save
    }

    val updateItemsHandler: (Int, List<SelectableItem>) -> Unit = { listId, newItems ->
        val updatedGlobalLists = userPreferences.packingLists.map {
            if (it.id == listId) it.copy(items = newItems) else it
        }
        viewModel.updatePackingListsOnly(updatedGlobalLists)
    }

    val generateItemIdHandler: () -> Int = {
        val idToUse = userPreferences.nextItemId
        Log.d(
            "ID_DEBUG",
            "generateItemIdHandler: idToUse = $idToUse, current userPreferences.nextItemId = ${userPreferences.nextItemId}"
        )
        val newNextItemIdToStore = userPreferences.nextItemId + 1
        Log.d("ID_DEBUG", "generateItemIdHandler: newNextItemIdToStore = $newNextItemIdToStore")
        viewModel.updateNextItemId(newNextItemIdToStore)
        idToUse
    }

    // This is the navigation logic passed to SelectableListScreen
    val navigateToPackingListsScreen = {
        currentScreen = Screen.PackingListsScreen
    }

    val renameListHandler = { listId: Int, newName: String ->
        viewModel.renamePackingList(listId, newName)
    }

    Crossfade(targetState = currentScreen, label = "screen_crossfade") { screen ->
        when (screen) {
            is Screen.PackingListsScreen -> {
                AllPackingListsScreen(
                    packingLists = userPreferences.packingLists,
                    onSelectList = { listId -> currentScreen = Screen.ItemsScreen(listId) },
                    onAddNewList = addNewListHandler,
                    onRenameList = renameListHandler // Pass the new handler
                )
            }
            is Screen.ItemsScreen -> {
                val list = userPreferences.packingLists.find { it.id == screen.listId }
                if (list != null) {
                    SelectableListScreen(
                        packingList = list, // Pass the whole list
                        onUpdateItems = { updatedItems ->
                            updateItemsHandler(list.id, updatedItems)
                        },
                        generateItemId = generateItemIdHandler,
                        onNavigateBack = navigateToPackingListsScreen,
                        onRenameListTitle = { newName -> // Add this
                            viewModel.renamePackingList(list.id, newName)
                        }
                    )

                    // Add BackHandler here for the ItemsScreen
                    BackHandler(enabled = true) { // enabled = true means it will intercept back presses
                        navigateToPackingListsScreen() // Perform your custom back navigation
                    }

                } else {
                    Text("Error: List not found. Navigating back.")
                    LaunchedEffect(Unit) {
                        navigateToPackingListsScreen()
                    }
                    // Also handle back press here if error state is shown
                    BackHandler(enabled = true) {
                        navigateToPackingListsScreen()
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
    onAddNewList: () -> Unit,
    onRenameList: (listId: Int, newName: String) -> Unit // Add this handler
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

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun PackingListCard(
    list: PackingList,
    onClick: () -> Unit,
    onRename: (newName: String) -> Unit // Add this handler
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