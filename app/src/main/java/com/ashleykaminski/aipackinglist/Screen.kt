package com.ashleykaminski.aipackinglist

import androidx.compose.runtime.saveable.Saver

// Originally from MainActivity.kt
sealed class Screen {
    object PackingListsScreen : Screen() // This is an object
    data class ItemsScreen(val listId: Int) : Screen()
    object TemplatesScreen : Screen()
    data class TemplateDetailScreen(val templateId: Int) : Screen()

    companion object {
        // Define String constants for screen types to avoid typos
        private const val TYPE_PACKING_LISTS = "PackingListsScreen"
        private const val TYPE_ITEMS = "ItemsScreen"
        private const val TYPE_TEMPLATES = "TemplatesScreen"
        private const val TYPE_TEMPLATE_DETAIL = "TemplateDetailScreen"
        private const val KEY_LIST_ID = "listId"
        private const val KEY_TEMPLATE_ID = "templateId"

        val Saver: Saver<Screen, Any> = Saver(
            save = { screen ->
                when (screen) {
                    is PackingListsScreen -> mapOf("type" to TYPE_PACKING_LISTS)
                    is ItemsScreen -> mapOf("type" to TYPE_ITEMS, KEY_LIST_ID to screen.listId)
                    is TemplatesScreen -> mapOf("type" to TYPE_TEMPLATES)
                    is TemplateDetailScreen -> mapOf("type" to TYPE_TEMPLATE_DETAIL, KEY_TEMPLATE_ID to screen.templateId)
                }
            },
            restore = { savedValue ->
                val map = savedValue as? Map<String, Any> ?: return@Saver null
                when (map["type"] as? String) {
                    TYPE_PACKING_LISTS -> PackingListsScreen
                    TYPE_ITEMS -> {
                        val listId = map[KEY_LIST_ID] as? Int
                        if (listId != null) ItemsScreen(listId) else PackingListsScreen
                    }
                    TYPE_TEMPLATES -> TemplatesScreen
                    TYPE_TEMPLATE_DETAIL -> {
                        val templateId = map[KEY_TEMPLATE_ID] as? Int
                        if (templateId != null) TemplateDetailScreen(templateId) else TemplatesScreen
                    }
                    else -> null
                }
            }
        )
    }
}
