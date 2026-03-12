package com.ashleykaminski.aipackinglist

import android.content.Context
import androidx.datastore.dataStore
import kotlinx.serialization.Serializable

// Originally from MainActivity.kt
@Serializable
data class UserPreferences(
    val packingLists: List<PackingList> = emptyList(),
    val nextPackingListId: Int = 1,
    val nextItemId: Int = 1,
    val templates: List<PackingListTemplate> = emptyList()
)

// Originally from MainActivity.kt
// Ensure UserPreferencesSerializer is defined and imported if it's in a different file.
// For now, assuming it's available in this scope or will be created/moved here.
val Context.userPreferencesDataStore: androidx.datastore.core.DataStore<UserPreferences> by dataStore(
    fileName = "user_prefs.pb", // File name for storing the data
    serializer = UserPreferencesSerializer // This will need to be resolvable
)
