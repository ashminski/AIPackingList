package com.ashleykaminski.aipackinglist

// You'll need a ViewModel Factory to pass the DataStore instance
// (Or use a dependency injection framework like Hilt)
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class PackingListViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PackingListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PackingListViewModel(context.applicationContext.userPreferencesDataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}