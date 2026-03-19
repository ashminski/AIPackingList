package com.ashleykaminski.aipackinglist

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PackingListViewModelTopicsTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var fakeDataStore: InMemoryUserPreferencesDataStore
    private lateinit var viewModel: PackingListViewModel

    @Before
    fun setUp() {
        fakeDataStore = InMemoryUserPreferencesDataStore()
        viewModel = PackingListViewModel(fakeDataStore)
    }

    @Test
    fun `default topics are seeded when topics list is empty`() = runTest {
        val state = viewModel.userPreferencesFlow.value
        assertThat(state.topics).isEqualTo(PackingListViewModel.DEFAULT_TOPICS)
    }

    @Test
    fun `user topics are preserved during additive seeding`() = runTest {
        val existingTopic = TripTopic(id = 99, name = "My Custom Topic")
        fakeDataStore.setInitialData(UserPreferences(topics = listOf(existingTopic)))
        viewModel = PackingListViewModel(fakeDataStore)

        val state = viewModel.userPreferencesFlow.value
        // User's custom topic must still be present
        assertThat(state.topics.any { it.id == 99 && it.name == "My Custom Topic" }).isTrue()
    }

    @Test
    fun `missing default topics are added additively`() = runTest {
        val existingTopic = TripTopic(id = 1, name = "Swimming") // matches a default topic name
        fakeDataStore.setInitialData(UserPreferences(topics = listOf(existingTopic)))
        viewModel = PackingListViewModel(fakeDataStore)

        val state = viewModel.userPreferencesFlow.value
        // Swimming already existed, so only the other defaults should be added
        val topicNames = state.topics.map { it.name }
        assertThat(topicNames).contains("Swimming")
        assertThat(topicNames).hasSize(PackingListViewModel.DEFAULT_TOPICS.size)
    }

    @Test
    fun `addNewTopic adds a topic with default name`() = runTest {
        fakeDataStore.setInitialData(UserPreferences(topics = listOf(TripTopic(id = 1, name = "Existing"))))
        viewModel = PackingListViewModel(fakeDataStore, defaultTopics = emptyList())

        viewModel.addNewTopic()

        val topics = viewModel.userPreferencesFlow.value.topics
        assertThat(topics).hasSize(2)
        assertThat(topics.last().name).isEqualTo("New Topic 2")
        assertThat(topics.last().items).isEmpty()
    }

    @Test
    fun `renameTopic updates the name of the correct topic`() = runTest {
        val topic = TripTopic(id = 5, name = "Old Name")
        fakeDataStore.setInitialData(UserPreferences(topics = listOf(topic)))
        viewModel = PackingListViewModel(fakeDataStore, defaultTopics = emptyList())

        viewModel.renameTopic(topicId = 5, newName = "New Name")

        val updatedTopic = viewModel.userPreferencesFlow.value.topics.first()
        assertThat(updatedTopic.name).isEqualTo("New Name")
        assertThat(updatedTopic.id).isEqualTo(5)
    }

    @Test
    fun `updateItemsForTopic updates only the target topic`() = runTest {
        val t1 = TripTopic(id = 1, name = "T1", items = emptyList())
        val t2 = TripTopic(id = 2, name = "T2", items = emptyList())
        fakeDataStore.setInitialData(UserPreferences(topics = listOf(t1, t2)))
        viewModel = PackingListViewModel(fakeDataStore, defaultTopics = emptyList())

        val newItems = listOf(TemplateItem(id = 1, text = "Swimsuit"))
        viewModel.updateItemsForTopic(topicId = 1, items = newItems)

        val state = viewModel.userPreferencesFlow.value
        assertThat(state.topics.find { it.id == 1 }?.items).isEqualTo(newItems)
        assertThat(state.topics.find { it.id == 2 }?.items).isEmpty()
    }

    @Test
    fun `deleteTopic removes the correct topic`() = runTest {
        val t1 = TripTopic(id = 1, name = "T1")
        val t2 = TripTopic(id = 2, name = "T2")
        fakeDataStore.setInitialData(UserPreferences(topics = listOf(t1, t2)))
        viewModel = PackingListViewModel(fakeDataStore, defaultTopics = emptyList())

        viewModel.deleteTopic(topicId = 1)

        val topics = viewModel.userPreferencesFlow.value.topics
        assertThat(topics).hasSize(1)
        assertThat(topics.first().id).isEqualTo(2)
    }

    @Test
    fun `createListFromTopics creates a list with deduped items and emits new list id`() = runTest {
        val topic1 = TripTopic(
            id = 1, name = "Swimming",
            items = listOf(TemplateItem(1, "Swimsuit"), TemplateItem(2, "Towel"))
        )
        val topic2 = TripTopic(
            id = 2, name = "Beach",
            items = listOf(TemplateItem(1, "Sunscreen"), TemplateItem(2, "Towel")) // "Towel" duplicates topic1
        )
        fakeDataStore.setInitialData(UserPreferences(topics = listOf(topic1, topic2), nextPackingListId = 5))
        viewModel = PackingListViewModel(fakeDataStore, defaultTopics = emptyList())

        viewModel.newListCreatedEvent.test {
            viewModel.createListFromTopics(topicIds = listOf(1, 2), listName = "Beach Trip")
            val emittedId = awaitItem()
            assertThat(emittedId).isEqualTo(5)
            cancelAndIgnoreRemainingEvents()
        }

        val state = viewModel.userPreferencesFlow.value
        val newList = state.packingLists.find { it.id == 5 }
        assertThat(newList).isNotNull()
        assertThat(newList!!.name).isEqualTo("Beach Trip")
        // Swimsuit, Towel, Sunscreen — "Towel" deduped
        assertThat(newList.items).hasSize(3)
        assertThat(newList.items.map { it.text }).containsExactly("Swimsuit", "Towel", "Sunscreen")
        assertThat(newList.items.all { !it.isSelected }).isTrue()
    }

    @Test
    fun `createListFromTopics deduplicates case-insensitively`() = runTest {
        val topic1 = TripTopic(id = 1, name = "T1", items = listOf(TemplateItem(1, "sunscreen")))
        val topic2 = TripTopic(id = 2, name = "T2", items = listOf(TemplateItem(1, "Sunscreen")))
        fakeDataStore.setInitialData(UserPreferences(topics = listOf(topic1, topic2), nextPackingListId = 1))
        viewModel = PackingListViewModel(fakeDataStore, defaultTopics = emptyList())

        viewModel.createListFromTopics(topicIds = listOf(1, 2), listName = "Test List")

        val state = viewModel.userPreferencesFlow.value
        val newList = state.packingLists.find { it.id == 1 }
        assertThat(newList!!.items).hasSize(1)
    }

    @Test
    fun `generateNewTopicItemId returns 1 for an empty topic`() {
        val topic = TripTopic(id = 1, name = "Empty", items = emptyList())
        assertThat(viewModel.generateNewTopicItemId(topic)).isEqualTo(1)
    }

    @Test
    fun `generateNewTopicItemId returns max id plus 1`() {
        val items = listOf(TemplateItem(3, "A"), TemplateItem(7, "B"), TemplateItem(1, "C"))
        val topic = TripTopic(id = 1, name = "T", items = items)
        assertThat(viewModel.generateNewTopicItemId(topic)).isEqualTo(8)
    }
}
