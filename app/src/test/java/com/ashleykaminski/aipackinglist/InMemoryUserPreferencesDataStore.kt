package com.ashleykaminski.aipackinglist

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A fake in-memory implementation of DataStore for testing. It uses a
 * MutableStateFlow to hold the data, allowing you to easily set and
 * check the state in your tests.
 */
class InMemoryUserPreferencesDataStore(
    initialData: UserPreferences = UserPreferences()
) : DataStore<UserPreferences> {

    private val flow = MutableStateFlow(initialData)

    override val data: Flow<UserPreferences>
        get() = flow

    override suspend fun updateData(transform: suspend (t: UserPreferences) -> UserPreferences): UserPreferences {
        val updatedData = transform(flow.value)
        flow.value = updatedData
        return updatedData
    }

    // Helper to manually set the data for test setup
    fun setInitialData(data: UserPreferences) {
        flow.value = data
    }
}