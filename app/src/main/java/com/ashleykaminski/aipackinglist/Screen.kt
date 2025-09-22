package com.ashleykaminski.aipackinglist

import androidx.compose.runtime.saveable.Saver

// Originally from MainActivity.kt
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
