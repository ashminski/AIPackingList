package com.ashleykaminski.aipackinglist

import android.content.Context
import android.util.Log // For logging
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.datastore.core.DataStore // Ensure correct import
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PackingListViewModel(private val dataStore: DataStore<UserPreferences>) : ViewModel() {

    val userPreferencesFlow: StateFlow<UserPreferences> = dataStore.data.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UserPreferences()
    )

    private val _newListCreatedEvent = MutableSharedFlow<Int>()
    val newListCreatedEvent: SharedFlow<Int> = _newListCreatedEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            dataStore.updateData { prefs ->
                if (prefs.templates.isEmpty()) prefs.copy(templates = DEFAULT_TEMPLATES) else prefs
            }
        }
    }

    // Function to add a new list and update the nextPackingListId
    fun addNewListAndUpdateIds(newList: PackingList, newNextPackingListId: Int) {
        viewModelScope.launch {
            dataStore.updateData { currentPreferences ->
                Log.d("ViewModel", "addNewListAndUpdateIds: Current nextPackingListId=${currentPreferences.nextPackingListId}, newNextPackingListId=$newNextPackingListId")
                Log.d("ViewModel", "addNewListAndUpdateIds: Current lists count=${currentPreferences.packingLists.size}, adding list: ${newList.name}")

                val updatedLists = currentPreferences.packingLists + newList

                currentPreferences.copy(
                    packingLists = updatedLists,
                    nextPackingListId = newNextPackingListId
                )
            }
        }
    }

    // ADD THIS FUNCTION
    fun addNewPackingList() {
        viewModelScope.launch {
            dataStore.updateData { currentUserPreferences ->
                val currentLists = currentUserPreferences.packingLists
                // Generate a new unique ID for the list
                val newId = (currentLists.maxOfOrNull { it.id } ?: 0) + 1
                val newListName = "New List ${currentLists.size + 1}"
                val newList = PackingList(id = newId, name = newListName, items = emptyList())

                currentUserPreferences.copy(
                    packingLists = currentLists + newList
                )
            }
        }
    }

    // Only updates packingLists, using the latest other preferences from DataStore
    fun updatePackingListsOnly(newLists: List<PackingList>) {
        viewModelScope.launch {
            dataStore.updateData { currentPreferences ->
                Log.d("ViewModel", "updatePackingListsOnly: Updating lists. New lists count=${newLists.size}")
                currentPreferences.copy(packingLists = newLists)
            }
        }
    }

    // Updates only nextItemId
    fun updateNextItemId(newId: Int) {
        viewModelScope.launch {
            dataStore.updateData { currentPreferences ->
                Log.d("ViewModel", "updateNextItemId: Current nextItemId=${currentPreferences.nextItemId}, newNextItemId=$newId")
                if (currentPreferences.nextItemId == newId) {
                    Log.w("ViewModel", "updateNextItemId: newId ($newId) is the same as current nextItemId. This might be an issue or redundant call.")
                }
                currentPreferences.copy(nextItemId = newId)
            }
        }
    }

    // This function can be kept if you have scenarios where you want to overwrite everything
    // but be cautious with it if other specific update functions are being used concurrently.
    fun saveUserPreferences(preferences: UserPreferences) {
        viewModelScope.launch {
            Log.d("ViewModel", "saveUserPreferences: Saving full preferences object. nextItemId=${preferences.nextItemId}, nextPackingListId=${preferences.nextPackingListId}")
            dataStore.updateData {
                // Simply returns the new preferences object, replacing the old one.
                preferences
            }
        }
    }
    fun renamePackingList(listId: Int, newName: String) {
        viewModelScope.launch {
            // Use the 'dataStore' instance that's already a property of this ViewModel
            dataStore.updateData { currentUserPreferences ->
                val updatedLists = currentUserPreferences.packingLists.map { list ->
                    if (list.id == listId) {
                        list.copy(name = newName)
                    } else {
                        list
                    }
                }
                currentUserPreferences.copy(packingLists = updatedLists)
            }
        }
    }

    // ADD THIS FUNCTION
    fun updateItemsForList(listId: Int, updatedItems: List<SelectableItem>) {
        viewModelScope.launch {
            dataStore.updateData { currentUserPreferences ->
                val updatedLists = currentUserPreferences.packingLists.map { list ->
                    if (list.id == listId) {
                        list.copy(items = updatedItems)
                    } else {
                        list
                    }
                }
                currentUserPreferences.copy(packingLists = updatedLists)
            }
        }
    }

    // ADD THIS FUNCTION
    fun generateNewItemId(list: PackingList): Int {
        // Find the maximum ID among the existing items and add 1.
        // If the list is empty, start with 1.
        return (list.items.maxOfOrNull { it.id } ?: 0) + 1
    }

    fun addNewTemplate() {
        viewModelScope.launch {
            dataStore.updateData { prefs ->
                val newId = (prefs.templates.maxOfOrNull { it.id } ?: 0) + 1
                val newTemplate = PackingListTemplate(id = newId, name = "New Template ${prefs.templates.size + 1}")
                prefs.copy(templates = prefs.templates + newTemplate)
            }
        }
    }

    fun renameTemplate(templateId: Int, newName: String) {
        viewModelScope.launch {
            dataStore.updateData { prefs ->
                val updatedTemplates = prefs.templates.map { template ->
                    if (template.id == templateId) template.copy(name = newName) else template
                }
                prefs.copy(templates = updatedTemplates)
            }
        }
    }

    fun updateItemsForTemplate(templateId: Int, items: List<TemplateItem>) {
        viewModelScope.launch {
            dataStore.updateData { prefs ->
                val updatedTemplates = prefs.templates.map { template ->
                    if (template.id == templateId) template.copy(items = items) else template
                }
                prefs.copy(templates = updatedTemplates)
            }
        }
    }

    fun deleteTemplate(templateId: Int) {
        viewModelScope.launch {
            dataStore.updateData { prefs ->
                prefs.copy(templates = prefs.templates.filter { it.id != templateId })
            }
        }
    }

    fun createListFromTemplate(templateId: Int) {
        viewModelScope.launch {
            var newListId = -1
            dataStore.updateData { prefs ->
                val template = prefs.templates.find { it.id == templateId } ?: return@updateData prefs
                val listId = prefs.nextPackingListId
                // Assign fresh global item IDs so they don't collide with future items added via generateItemIdHandler
                var nextItemId = prefs.nextItemId
                val copiedItems = template.items.map { SelectableItem(id = nextItemId++, text = it.text, isSelected = false) }
                val newList = PackingList(id = listId, name = template.name, items = copiedItems)
                newListId = listId
                prefs.copy(
                    packingLists = prefs.packingLists + newList,
                    nextPackingListId = listId + 1,
                    nextItemId = nextItemId
                )
            }
            if (newListId != -1) {
                _newListCreatedEvent.emit(newListId)
            }
        }
    }

    fun generateNewTemplateItemId(template: PackingListTemplate): Int {
        return (template.items.maxOfOrNull { it.id } ?: 0) + 1
    }

    companion object {
        val DEFAULT_TEMPLATES = listOf(
            PackingListTemplate(
                id = 1,
                name = "Beach Trip",
                items = listOf(
                    TemplateItem(1, "Sunscreen"),
                    TemplateItem(2, "Swimsuit"),
                    TemplateItem(3, "Beach towel"),
                    TemplateItem(4, "Sunglasses"),
                    TemplateItem(5, "Flip flops"),
                    TemplateItem(6, "Hat"),
                    TemplateItem(7, "Water bottle"),
                    TemplateItem(8, "Beach bag"),
                    TemplateItem(9, "Snacks"),
                    TemplateItem(10, "Book or e-reader")
                )
            ),
            PackingListTemplate(
                id = 2,
                name = "Ski Trip",
                items = listOf(
                    TemplateItem(1, "Ski jacket"),
                    TemplateItem(2, "Ski pants"),
                    TemplateItem(3, "Thermal base layers"),
                    TemplateItem(4, "Ski socks"),
                    TemplateItem(5, "Gloves"),
                    TemplateItem(6, "Goggles"),
                    TemplateItem(7, "Helmet"),
                    TemplateItem(8, "Neck gaiter"),
                    TemplateItem(9, "Lip balm with SPF"),
                    TemplateItem(10, "Hand warmers")
                )
            ),
            PackingListTemplate(
                id = 3,
                name = "Weekend Away",
                items = listOf(
                    TemplateItem(1, "Change of clothes"),
                    TemplateItem(2, "Toiletries bag"),
                    TemplateItem(3, "Phone charger"),
                    TemplateItem(4, "Medications"),
                    TemplateItem(5, "Wallet and ID"),
                    TemplateItem(6, "Snacks"),
                    TemplateItem(7, "Headphones"),
                    TemplateItem(8, "Pajamas")
                )
            ),
            PackingListTemplate(
                id = 4,
                name = "City Break",
                items = listOf(
                    TemplateItem(1, "Comfortable walking shoes"),
                    TemplateItem(2, "City map or guidebook"),
                    TemplateItem(3, "Day bag or backpack"),
                    TemplateItem(4, "Camera"),
                    TemplateItem(5, "Portable charger"),
                    TemplateItem(6, "Umbrella"),
                    TemplateItem(7, "Smart casual outfit"),
                    TemplateItem(8, "Travel adapter"),
                    TemplateItem(9, "Wallet and ID"),
                    TemplateItem(10, "Medications")
                )
            )
        )
    }
}
