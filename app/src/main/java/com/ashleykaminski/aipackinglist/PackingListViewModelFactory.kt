package com.ashleykaminski.aipackinglist

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class PackingListViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PackingListViewModel::class.java)) {
            val appContext = context.applicationContext
            val templates = try {
                DefaultsParser.parseTemplates(appContext)
            } catch (e: Exception) {
                PackingListViewModel.DEFAULT_TEMPLATES
            }
            val topics = try {
                DefaultsParser.parseTopics(appContext)
            } catch (e: Exception) {
                PackingListViewModel.DEFAULT_TOPICS
            }
            val questions = try {
                DefaultsParser.parseQuestions(appContext)
            } catch (e: Exception) {
                PackingListViewModel.DEFAULT_QUESTIONS
            }
            @Suppress("UNCHECKED_CAST")
            return PackingListViewModel(
                dataStore = appContext.userPreferencesDataStore,
                defaultTemplates = templates,
                defaultTopics = topics,
                questions = questions
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
