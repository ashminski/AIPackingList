package com.ashleykaminski.aipackinglist

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PackingListViewModel(private val dataStore: DataStore<UserPreferences>) :
    ViewModel() {

    // Expose UserPreferences as a StateFlow
    val userPreferencesFlow: StateFlow<UserPreferences> = dataStore.data.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly, // Or Lazily, depending on your needs
        initialValue = UserPreferences() // Initial default value
    )

    fun updatePackingLists(newLists: List<PackingList>) {
        viewModelScope.launch {
            dataStore.updateData { currentPreferences ->
                currentPreferences.copy(packingLists = newLists)
            }
        }
    }

    fun updateNextPackingListId(newId: Int) {
        viewModelScope.launch {
            dataStore.updateData { currentPreferences ->
                currentPreferences.copy(nextPackingListId = newId)
            }
        }
    }

    fun updateNextItemId(newId: Int) {
        viewModelScope.launch {
            dataStore.updateData { currentPreferences ->
                currentPreferences.copy(nextItemId = newId)
            }
        }
    }


    fun saveUserPreferences(preferences: UserPreferences) {
        viewModelScope.launch {
            dataStore.updateData { preferences }
        }
    }
}
