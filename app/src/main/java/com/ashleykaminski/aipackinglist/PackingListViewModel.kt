package com.ashleykaminski.aipackinglist

import android.content.Context
import android.util.Log // For logging
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.datastore.core.DataStore // Ensure correct import
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
// import kotlinx.coroutines.flow.map // Not used directly in this snippet but often useful
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PackingListViewModel(private val dataStore: DataStore<UserPreferences>) : ViewModel() {

    val userPreferencesFlow: StateFlow<UserPreferences> = dataStore.data.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UserPreferences()
    )

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
}
