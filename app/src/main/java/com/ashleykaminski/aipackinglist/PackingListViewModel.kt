package com.ashleykaminski.aipackinglist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PackingListViewModel(
    private val dataStore: DataStore<UserPreferences>,
    private val defaultTemplates: List<PackingListTemplate> = DEFAULT_TEMPLATES,
    private val defaultTopics: List<TripTopic> = DEFAULT_TOPICS,
    val questions: List<TripQuestion> = DEFAULT_QUESTIONS
) : ViewModel() {

    val userPreferencesFlow: StateFlow<UserPreferences> = dataStore.data.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UserPreferences()
    )

    private val _newListCreatedEvent = MutableSharedFlow<Int>()
    val newListCreatedEvent: SharedFlow<Int> = _newListCreatedEvent.asSharedFlow()

    private val _listDeletedEvent = MutableSharedFlow<Unit>()
    val listDeletedEvent: SharedFlow<Unit> = _listDeletedEvent.asSharedFlow()

    private var lastDeletedList: PackingList? = null

    init {
        viewModelScope.launch {
            dataStore.updateData { prefs ->
                var updated = prefs
                if (updated.templates.isEmpty()) updated = updated.copy(templates = defaultTemplates)
                // Additive: seed missing topics by name so new topics.txt entries appear without wiping user edits
                val storedTopicNames = updated.topics.map { it.name.trim().lowercase() }.toSet()
                val missingTopics = defaultTopics.filter { it.name.trim().lowercase() !in storedTopicNames }
                if (missingTopics.isNotEmpty()) {
                    var nextId = (updated.topics.maxOfOrNull { it.id } ?: 0) + 1
                    val reIdedTopics = missingTopics.map { it.copy(id = nextId++) }
                    updated = updated.copy(topics = updated.topics + reIdedTopics)
                }
                updated
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

    fun addNewPackingList() {
        viewModelScope.launch {
            dataStore.updateData { currentUserPreferences ->
                val currentLists = currentUserPreferences.packingLists
                val newId = (currentLists.maxOfOrNull { it.id } ?: 0) + 1
                val newListName = "New List ${currentLists.size + 1}"
                val newList = PackingList(id = newId, name = newListName, items = emptyList())

                currentUserPreferences.copy(
                    packingLists = currentLists + newList
                )
            }
        }
    }

    fun updatePackingListsOnly(newLists: List<PackingList>) {
        viewModelScope.launch {
            dataStore.updateData { currentPreferences ->
                Log.d("ViewModel", "updatePackingListsOnly: Updating lists. New lists count=${newLists.size}")
                currentPreferences.copy(packingLists = newLists)
            }
        }
    }

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

    fun saveUserPreferences(preferences: UserPreferences) {
        viewModelScope.launch {
            Log.d("ViewModel", "saveUserPreferences: Saving full preferences object. nextItemId=${preferences.nextItemId}, nextPackingListId=${preferences.nextPackingListId}")
            dataStore.updateData {
                preferences
            }
        }
    }

    fun deletePackingList(listId: Int) {
        viewModelScope.launch {
            dataStore.updateData { prefs ->
                lastDeletedList = prefs.packingLists.find { it.id == listId }
                prefs.copy(packingLists = prefs.packingLists.filter { it.id != listId })
            }
            if (lastDeletedList != null) _listDeletedEvent.emit(Unit)
        }
    }

    fun undoDeletePackingList() {
        val list = lastDeletedList ?: return
        viewModelScope.launch {
            dataStore.updateData { prefs ->
                prefs.copy(packingLists = prefs.packingLists + list)
            }
            lastDeletedList = null
        }
    }

    fun renamePackingList(listId: Int, newName: String) {
        viewModelScope.launch {
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

    fun generateNewItemId(list: PackingList): Int {
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
                var nextItemId = prefs.nextItemId
                val copiedItems = template.items.map { SelectableItem(id = nextItemId++, text = it.text, topicName = it.topicName) }
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

    fun createTemplateFromTopics(topicNames: List<String>, templateName: String) {
        val normalizedNames = topicNames.map { it.trim().lowercase() }.toSet()
        viewModelScope.launch {
            dataStore.updateData { prefs ->
                val selectedTopics = prefs.topics.filter { it.name.trim().lowercase() in normalizedNames }
                var nextId = 1
                val seenTexts = mutableSetOf<String>()
                val items = selectedTopics.flatMap { topic ->
                    topic.items.mapNotNull { item ->
                        if (seenTexts.add(item.text.trim().lowercase()))
                            TemplateItem(id = nextId++, text = item.text, topicName = topic.name)
                        else null
                    }
                }
                val newId = (prefs.templates.maxOfOrNull { it.id } ?: 0) + 1
                val newTemplate = PackingListTemplate(id = newId, name = templateName, items = items)
                prefs.copy(templates = prefs.templates + newTemplate)
            }
        }
    }

    // --- Topic CRUD ---

    fun addNewTopic() {
        viewModelScope.launch {
            dataStore.updateData { prefs ->
                val newId = (prefs.topics.maxOfOrNull { it.id } ?: 0) + 1
                val newTopic = TripTopic(id = newId, name = "New Topic ${prefs.topics.size + 1}")
                prefs.copy(topics = prefs.topics + newTopic)
            }
        }
    }

    fun renameTopic(topicId: Int, newName: String) {
        viewModelScope.launch {
            dataStore.updateData { prefs ->
                val updatedTopics = prefs.topics.map { topic ->
                    if (topic.id == topicId) topic.copy(name = newName) else topic
                }
                prefs.copy(topics = updatedTopics)
            }
        }
    }

    fun updateItemsForTopic(topicId: Int, items: List<TemplateItem>) {
        viewModelScope.launch {
            dataStore.updateData { prefs ->
                val updatedTopics = prefs.topics.map { topic ->
                    if (topic.id == topicId) topic.copy(items = items) else topic
                }
                prefs.copy(topics = updatedTopics)
            }
        }
    }

    fun deleteTopic(topicId: Int) {
        viewModelScope.launch {
            dataStore.updateData { prefs ->
                prefs.copy(topics = prefs.topics.filter { it.id != topicId })
            }
        }
    }

    fun generateNewTopicItemId(topic: TripTopic): Int {
        return (topic.items.maxOfOrNull { it.id } ?: 0) + 1
    }

    fun createListFromTopicNames(topicNames: List<String>, listName: String) {
        val normalizedNames = topicNames.map { it.trim().lowercase() }.toSet()
        viewModelScope.launch {
            var newListId = -1
            dataStore.updateData { prefs ->
                val selectedTopics = prefs.topics.filter { it.name.trim().lowercase() in normalizedNames }
                var nextItemId = prefs.nextItemId
                val seenTexts = mutableSetOf<String>()
                val copiedItems = selectedTopics.flatMap { topic ->
                    topic.items.mapNotNull { item ->
                        if (seenTexts.add(item.text.trim().lowercase()))
                            SelectableItem(id = nextItemId++, text = item.text, topicName = topic.name)
                        else null
                    }
                }
                val listId = prefs.nextPackingListId
                val newList = PackingList(id = listId, name = listName, items = copiedItems)
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

    fun createListFromTopics(topicIds: List<Int>, listName: String) {
        viewModelScope.launch {
            var newListId = -1
            dataStore.updateData { prefs ->
                val selectedTopics = prefs.topics.filter { it.id in topicIds }
                var nextItemId = prefs.nextItemId
                val seenTexts = mutableSetOf<String>()
                val copiedItems = selectedTopics.flatMap { topic ->
                    topic.items.mapNotNull { item ->
                        if (seenTexts.add(item.text.trim().lowercase()))
                            SelectableItem(id = nextItemId++, text = item.text, topicName = topic.name)
                        else null
                    }
                }
                val listId = prefs.nextPackingListId
                val newList = PackingList(id = listId, name = listName, items = copiedItems)
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

    companion object {
        val DEFAULT_TEMPLATES: List<PackingListTemplate> = emptyList()

        val DEFAULT_TOPICS = listOf(
            TripTopic(
                id = 1,
                name = "Swimming",
                isBuiltIn = true,
                items = listOf(
                    TemplateItem(1, "Swimsuit"),
                    TemplateItem(2, "Goggles"),
                    TemplateItem(3, "Swim cap"),
                    TemplateItem(4, "Waterproof sunscreen"),
                    TemplateItem(5, "Towel")
                )
            ),
            TripTopic(
                id = 2,
                name = "Hiking",
                isBuiltIn = true,
                items = listOf(
                    TemplateItem(1, "Hiking boots"),
                    TemplateItem(2, "Trekking poles"),
                    TemplateItem(3, "Backpack"),
                    TemplateItem(4, "Water bottle"),
                    TemplateItem(5, "Trail snacks"),
                    TemplateItem(6, "First aid kit"),
                    TemplateItem(7, "Map or GPS device"),
                    TemplateItem(8, "Sunscreen"),
                    TemplateItem(9, "Hat")
                )
            ),
            TripTopic(
                id = 3,
                name = "Cycling",
                isBuiltIn = true,
                items = listOf(
                    TemplateItem(1, "Helmet"),
                    TemplateItem(2, "Cycling gloves"),
                    TemplateItem(3, "Padded shorts"),
                    TemplateItem(4, "Water bottle"),
                    TemplateItem(5, "Repair kit"),
                    TemplateItem(6, "Sunglasses"),
                    TemplateItem(7, "Bike lock")
                )
            ),
            TripTopic(
                id = 4,
                name = "Cold Weather",
                isBuiltIn = true,
                items = listOf(
                    TemplateItem(1, "Thermal base layers"),
                    TemplateItem(2, "Warm jacket"),
                    TemplateItem(3, "Gloves"),
                    TemplateItem(4, "Beanie hat"),
                    TemplateItem(5, "Scarf"),
                    TemplateItem(6, "Wool socks"),
                    TemplateItem(7, "Hand warmers"),
                    TemplateItem(8, "Lip balm")
                )
            ),
            TripTopic(
                id = 5,
                name = "Business Travel",
                isBuiltIn = true,
                items = listOf(
                    TemplateItem(1, "Laptop"),
                    TemplateItem(2, "Laptop charger"),
                    TemplateItem(3, "Business cards"),
                    TemplateItem(4, "Dress clothes"),
                    TemplateItem(5, "Dress shoes"),
                    TemplateItem(6, "Notebook and pen"),
                    TemplateItem(7, "Portable charger"),
                    TemplateItem(8, "Travel adapter")
                )
            ),
            TripTopic(
                id = 6,
                name = "Beach",
                isBuiltIn = true,
                items = listOf(
                    TemplateItem(1, "Sunscreen"),
                    TemplateItem(2, "Beach towel"),
                    TemplateItem(3, "Sunglasses"),
                    TemplateItem(4, "Flip flops"),
                    TemplateItem(5, "Hat"),
                    TemplateItem(6, "Beach bag"),
                    TemplateItem(7, "Snacks"),
                    TemplateItem(8, "Water bottle")
                )
            ),
            TripTopic(
                id = 7,
                name = "Camping",
                isBuiltIn = true,
                items = listOf(
                    TemplateItem(1, "Tent"),
                    TemplateItem(2, "Sleeping bag"),
                    TemplateItem(3, "Sleeping pad"),
                    TemplateItem(4, "Flashlight or headlamp"),
                    TemplateItem(5, "Camp stove"),
                    TemplateItem(6, "Cooking utensils"),
                    TemplateItem(7, "Bug repellent"),
                    TemplateItem(8, "Fire starter"),
                    TemplateItem(9, "Rope or paracord")
                )
            ),
            TripTopic(
                id = 8,
                name = "City Sightseeing",
                isBuiltIn = true,
                items = listOf(
                    TemplateItem(1, "Comfortable walking shoes"),
                    TemplateItem(2, "Day bag or backpack"),
                    TemplateItem(3, "Camera"),
                    TemplateItem(4, "City map or guidebook"),
                    TemplateItem(5, "Portable charger"),
                    TemplateItem(6, "Umbrella"),
                    TemplateItem(7, "Snacks"),
                    TemplateItem(8, "Water bottle")
                )
            )
        )

        val DEFAULT_QUESTIONS = listOf(
            TripQuestion(id = 1, text = "Will you be swimming?", topicNames = listOf("Swimming")),
            TripQuestion(id = 2, text = "Will you be hiking?", topicNames = listOf("Hiking")),
            TripQuestion(id = 3, text = "Will you be cycling?", topicNames = listOf("Cycling")),
            TripQuestion(id = 4, text = "Will you experience cold weather?", topicNames = listOf("Cold Weather")),
            TripQuestion(id = 5, text = "Is this a business trip?", topicNames = listOf("Business Travel")),
            TripQuestion(id = 6, text = "Will you spend time at the beach?", topicNames = listOf("Swimming", "Beach")),
            TripQuestion(id = 7, text = "Will you be camping?", topicNames = listOf("Camping")),
            TripQuestion(id = 8, text = "Will you be doing city sightseeing?", topicNames = listOf("City Sightseeing"))
        )
    }
}
