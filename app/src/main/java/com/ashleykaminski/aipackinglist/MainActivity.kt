package com.ashleykaminski.aipackinglist

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ashleykaminski.aipackinglist.ui.theme.AIPackingListTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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

    // Navigate to ItemsScreen when a list is created from a template or topics
    LaunchedEffect(Unit) {
        viewModel.newListCreatedEvent.collectLatest { newListId ->
            currentScreen = Screen.ItemsScreen(newListId)
        }
    }

    // Show "List deleted / Undo" snackbar after any list deletion
    LaunchedEffect(Unit) {
        viewModel.listDeletedEvent.collectLatest {
            val result = snackbarHostState.showSnackbar(
                message = "List deleted",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDeletePackingList()
            }
        }
    }

    Crossfade(targetState = currentScreen, label = "screen_crossfade") { screen ->
        when (screen) {
            is Screen.PackingListsScreen -> {
                AllPackingListsScreen(
                    packingLists = userPreferences.packingLists,
                    onSelectList = { listId -> currentScreen = Screen.ItemsScreen(listId) },
                    onAddNewList = addNewListHandler,
                    onRenameList = renameListHandler,
                    onDeleteList = { listId -> viewModel.deletePackingList(listId) },
                    onNavigateToTemplates = { currentScreen = Screen.TemplatesScreen },
                    onNavigateToTopics = { currentScreen = Screen.TopicsScreen },
                    onNavigateToTripWizard = { currentScreen = Screen.TripWizardScreen },
                    snackbarHostState = snackbarHostState
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
                        },
                        onDeleteList = {
                            viewModel.deletePackingList(list.id)
                            navigateToPackingListsScreen()
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
            is Screen.TemplatesScreen -> {
                TemplatesScreen(
                    templates = userPreferences.templates,
                    onSelectTemplate = { templateId -> currentScreen = Screen.TemplateDetailScreen(templateId) },
                    onAddNewTemplate = { viewModel.addNewTemplate() },
                    onRenameTemplate = { templateId, newName -> viewModel.renameTemplate(templateId, newName) },
                    onUseTemplate = { templateId ->
                        viewModel.createListFromTemplate(templateId)
                    },
                    onNavigateBack = navigateToPackingListsScreen
                )
                BackHandler(enabled = true) {
                    navigateToPackingListsScreen()
                }
            }
            is Screen.TemplateDetailScreen -> {
                val template = userPreferences.templates.find { it.id == screen.templateId }
                val navigateToTemplatesScreen = { currentScreen = Screen.TemplatesScreen }
                if (template != null) {
                    TemplateDetailScreen(
                        template = template,
                        onUpdateItems = { updatedItems -> viewModel.updateItemsForTemplate(template.id, updatedItems) },
                        generateItemId = { viewModel.generateNewTemplateItemId(template) },
                        onNavigateBack = navigateToTemplatesScreen,
                        onRenameTitle = { newName -> viewModel.renameTemplate(template.id, newName) }
                    )
                    BackHandler(enabled = true) {
                        navigateToTemplatesScreen()
                    }
                } else {
                    Text("Error: Template not found. Navigating back.")
                    LaunchedEffect(Unit) {
                        navigateToTemplatesScreen()
                    }
                    BackHandler(enabled = true) {
                        navigateToTemplatesScreen()
                    }
                }
            }
            is Screen.TopicsScreen -> {
                TopicsScreen(
                    topics = userPreferences.topics,
                    onSelectTopic = { topicId -> currentScreen = Screen.TopicDetailScreen(topicId) },
                    onAddNewTopic = { viewModel.addNewTopic() },
                    onRenameTopic = { topicId, newName -> viewModel.renameTopic(topicId, newName) },
                    onDeleteTopic = { topicId -> viewModel.deleteTopic(topicId) },
                    onCreateListFromTopics = { topicIds, listName ->
                        viewModel.createListFromTopics(topicIds, listName)
                    },
                    onNavigateBack = navigateToPackingListsScreen
                )
                BackHandler(enabled = true) {
                    navigateToPackingListsScreen()
                }
            }
            is Screen.TopicDetailScreen -> {
                val topic = userPreferences.topics.find { it.id == screen.topicId }
                val navigateToTopicsScreen = { currentScreen = Screen.TopicsScreen }
                if (topic != null) {
                    TopicDetailScreen(
                        topic = topic,
                        onUpdateItems = { updatedItems -> viewModel.updateItemsForTopic(topic.id, updatedItems) },
                        generateItemId = { viewModel.generateNewTopicItemId(topic) },
                        onNavigateBack = navigateToTopicsScreen,
                        onRenameTitle = { newName -> viewModel.renameTopic(topic.id, newName) }
                    )
                    BackHandler(enabled = true) {
                        navigateToTopicsScreen()
                    }
                } else {
                    Text("Error: Topic not found. Navigating back.")
                    LaunchedEffect(Unit) {
                        navigateToTopicsScreen()
                    }
                    BackHandler(enabled = true) {
                        navigateToTopicsScreen()
                    }
                }
            }
            is Screen.TripWizardScreen -> {
                TripWizardScreen(
                    questions = PackingListViewModel.DEFAULT_QUESTIONS,
                    onCreateList = { topicIds, listName ->
                        viewModel.createListFromTopics(topicIds, listName)
                    },
                    onNavigateBack = navigateToPackingListsScreen
                )
                BackHandler(enabled = true) {
                    navigateToPackingListsScreen()
                }
            }
        }
    }
}
