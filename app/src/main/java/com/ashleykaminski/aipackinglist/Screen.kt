package com.ashleykaminski.aipackinglist

import androidx.compose.runtime.saveable.Saver

// Originally from MainActivity.kt
sealed class Screen {
    object PackingListsScreen : Screen()
    data class ItemsScreen(val listId: Int) : Screen()
    object TemplatesScreen : Screen()
    data class TemplateDetailScreen(val templateId: Int) : Screen()
    object TopicsScreen : Screen()
    data class TopicDetailScreen(val topicId: Int) : Screen()
    object TripWizardScreen : Screen()
    object TemplateWizardScreen : Screen()

    companion object {
        private const val TYPE_PACKING_LISTS = "PackingListsScreen"
        private const val TYPE_ITEMS = "ItemsScreen"
        private const val TYPE_TEMPLATES = "TemplatesScreen"
        private const val TYPE_TEMPLATE_DETAIL = "TemplateDetailScreen"
        private const val TYPE_TOPICS = "TopicsScreen"
        private const val TYPE_TOPIC_DETAIL = "TopicDetailScreen"
        private const val TYPE_TRIP_WIZARD = "TripWizardScreen"
        private const val TYPE_TEMPLATE_WIZARD = "TemplateWizardScreen"
        private const val KEY_LIST_ID = "listId"
        private const val KEY_TEMPLATE_ID = "templateId"
        private const val KEY_TOPIC_ID = "topicId"

        val Saver: Saver<Screen, Any> = Saver(
            save = { screen ->
                when (screen) {
                    is PackingListsScreen -> mapOf("type" to TYPE_PACKING_LISTS)
                    is ItemsScreen -> mapOf("type" to TYPE_ITEMS, KEY_LIST_ID to screen.listId)
                    is TemplatesScreen -> mapOf("type" to TYPE_TEMPLATES)
                    is TemplateDetailScreen -> mapOf("type" to TYPE_TEMPLATE_DETAIL, KEY_TEMPLATE_ID to screen.templateId)
                    is TopicsScreen -> mapOf("type" to TYPE_TOPICS)
                    is TopicDetailScreen -> mapOf("type" to TYPE_TOPIC_DETAIL, KEY_TOPIC_ID to screen.topicId)
                    is TripWizardScreen -> mapOf("type" to TYPE_TRIP_WIZARD)
                    is TemplateWizardScreen -> mapOf("type" to TYPE_TEMPLATE_WIZARD)
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
                    TYPE_TOPICS -> TopicsScreen
                    TYPE_TOPIC_DETAIL -> {
                        val topicId = map[KEY_TOPIC_ID] as? Int
                        if (topicId != null) TopicDetailScreen(topicId) else TopicsScreen
                    }
                    TYPE_TRIP_WIZARD -> TripWizardScreen
                    TYPE_TEMPLATE_WIZARD -> TemplateWizardScreen
                    else -> null
                }
            }
        )
    }
}
