package com.ashleykaminski.aipackinglist

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ashleykaminski.aipackinglist.ui.theme.AIPackingListTheme

// Data classes (PackingList, UserPreferences), Screen sealed class, userPreferencesDataStore,
// AllPackingListsScreen, and PackingListCard have been moved to their own files.
// Ensure necessary imports if they are in different packages or not automatically resolved.

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
fun PackingListApp(viewModel: PackingListViewModel = viewModel(factory = PackingListViewModelFactory(LocalContext.current))) {
    val userPreferences by viewModel.userPreferencesFlow.collectAsState()
    var currentScreen by rememberSaveable(stateSaver = Screen.Saver) {
        mutableStateOf<Screen>(Screen.PackingListsScreen)
    }

    val addNewListHandler = {
        val idForThisList = userPreferences.nextPackingListId
        val newNextPackingListIdToStore = userPreferences.nextPackingListId + 1

        val newList = PackingList(
            id = idForThisList,
            name = "Packing List ${idForThisList}"
        )

        viewModel.addNewListAndUpdateIds(newList, newNextPackingListIdToStore)
        currentScreen = Screen.ItemsScreen(newList.id)
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
                    onRenameList = renameListHandler
                )
            }
            is Screen.ItemsScreen -> {
                val list = userPreferences.packingLists.find { it.id == screen.listId }
                if (list != null) {
                    SelectableListScreen(
                        packingList = list,
                        onUpdateItems = { updatedItems ->
                            updateItemsHandler(list.id, updatedItems)
                        },
                        generateItemId = generateItemIdHandler,
                        onNavigateBack = navigateToPackingListsScreen,
                        onRenameListTitle = { newName ->
                            viewModel.renamePackingList(list.id, newName)
                        }
                    )

                    BackHandler(enabled = true) {
                        navigateToPackingListsScreen()
                    }

                } else {
                    Text("Error: List not found. Navigating back.")
                    LaunchedEffect(Unit) {
                        navigateToPackingListsScreen()
                    }
                    BackHandler(enabled = true) {
                        navigateToPackingListsScreen()
                    }
                }
            }
        }
    }
}
