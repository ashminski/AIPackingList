// /app/src/test/java/com/ashleykaminski/aipackinglist/PackingListViewModelTest.kt

package com.ashleykaminski.aipackinglist

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PackingListViewModelTest {

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
    fun `initial state is correct`() = runTest {
        val initialState = viewModel.userPreferencesFlow.value
        assertThat(initialState.packingLists).isEmpty()
    }

    @Test
    fun `addNewPackingList adds a new list with correct default name`() = runTest {
        viewModel.addNewPackingList()

        val lists = viewModel.userPreferencesFlow.value.packingLists
        assertThat(lists).hasSize(1)
        assertThat(lists.first().name).isEqualTo("New List 1")
        assertThat(lists.first().items).isEmpty()
    }

    @Test
    fun `addNewPackingList increments default name correctly`() = runTest {
        viewModel.addNewPackingList() // Adds "New List 1"
        viewModel.addNewPackingList() // Should add "New List 2"

        val lists = viewModel.userPreferencesFlow.value.packingLists
        assertThat(lists).hasSize(2)
        assertThat(lists.map { it.name }).containsExactly("New List 1", "New List 2")
    }

    @Test
    fun `renamePackingList updates the name of the correct list`() = runTest {
        val initialList = PackingList(id = 123, name = "Old Name")
        fakeDataStore.setInitialData(UserPreferences(packingLists = listOf(initialList)))
        viewModel = PackingListViewModel(fakeDataStore)

        val newName = "Vacation Trip"
        viewModel.renamePackingList(listId = 123, newName = newName)

        val updatedList = viewModel.userPreferencesFlow.value.packingLists.first()
        assertThat(updatedList.name).isEqualTo(newName)
        assertThat(updatedList.id).isEqualTo(123)
    }

    @Test
    fun `updateItemsForList updates items for the correct list`() = runTest {
        val listToUpdate = PackingList(id = 1, name = "List 1", items = emptyList())
        val otherList = PackingList(id = 2, name = "List 2", items = emptyList())
        fakeDataStore.setInitialData(UserPreferences(packingLists = listOf(listToUpdate, otherList)))
        viewModel = PackingListViewModel(fakeDataStore)

        // **FIXED HERE**: Changed 'name' to 'text'
        val newItems = listOf(SelectableItem(id = 1, text = "Passport", isSelected = true))
        viewModel.updateItemsForList(listId = 1, updatedItems = newItems)

        val updatedState = viewModel.userPreferencesFlow.value
        val updatedList1 = updatedState.packingLists.find { it.id == 1 }
        val updatedList2 = updatedState.packingLists.find { it.id == 2 }

        assertThat(updatedList1?.items).isEqualTo(newItems)
        assertThat(updatedList2?.items).isEmpty()
    }

    @Test
    fun `generateNewItemId returns 1 for an empty list`() {
        val list = PackingList(id = 1, name = "Test", items = emptyList())
        val newId = viewModel.generateNewItemId(list)
        assertThat(newId).isEqualTo(1)
    }

    @Test
    fun `generateNewItemId returns max id plus 1 for a non-empty list`() {
        // **FIXED HERE**: Changed 'name' to 'text' in all items
        val items = listOf(
            SelectableItem(id = 1, text = "A"),
            SelectableItem(id = 5, text = "B"),
            SelectableItem(id = 2, text = "C")
        )
        val list = PackingList(id = 1, name = "Test", items = items)
        val newId = viewModel.generateNewItemId(list)
        // max(1, 5, 2) is 5, so the next ID should be 6
        assertThat(newId).isEqualTo(6)
    }

    @Test
    fun `userPreferencesFlow emits updates correctly using turbine`() = runTest {
        viewModel.userPreferencesFlow.test {
            // 1. Initial emission is an empty list
            assertThat(awaitItem().packingLists).isEmpty()

            // 2. Add a list and await the new state
            viewModel.addNewPackingList()
            val itemAfterAdd = awaitItem()
            assertThat(itemAfterAdd.packingLists).hasSize(1)
            val addedList = itemAfterAdd.packingLists.first()
            assertThat(addedList.name).isEqualTo("New List 1")

            // 3. Rename the list and await the final state
            viewModel.renamePackingList(addedList.id, "Holiday")
            val itemAfterRename = awaitItem()
            assertThat(itemAfterRename.packingLists.first().name).isEqualTo("Holiday")

            // Cancel and ignore any remaining events
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- Template tests ---

    @Test
    fun `default templates are seeded when templates list is empty`() = runTest {
        // setUp() creates a fresh ViewModel with empty DataStore, triggering seeding in init
        val state = viewModel.userPreferencesFlow.value
        // The init block is a coroutine; advance time to let it run
        // UnconfinedTestDispatcher runs coroutines eagerly; no need to advance scheduler
        val seededState = viewModel.userPreferencesFlow.value
        assertThat(seededState.templates).isEqualTo(PackingListViewModel.DEFAULT_TEMPLATES)
    }

    @Test
    fun `default templates are not re-seeded when templates already exist`() = runTest {
        val existingTemplate = PackingListTemplate(id = 99, name = "My Custom Template")
        fakeDataStore.setInitialData(UserPreferences(templates = listOf(existingTemplate)))
        viewModel = PackingListViewModel(fakeDataStore)
        // UnconfinedTestDispatcher runs coroutines eagerly; no need to advance scheduler

        val state = viewModel.userPreferencesFlow.value
        assertThat(state.templates).hasSize(1)
        assertThat(state.templates.first().id).isEqualTo(99)
    }

    @Test
    fun `addNewTemplate adds a template with default name`() = runTest {
        fakeDataStore.setInitialData(UserPreferences(templates = listOf(PackingListTemplate(id = 1, name = "Existing"))))
        viewModel = PackingListViewModel(fakeDataStore)

        viewModel.addNewTemplate()
        // UnconfinedTestDispatcher runs coroutines eagerly; no need to advance scheduler

        val templates = viewModel.userPreferencesFlow.value.templates
        assertThat(templates).hasSize(2)
        assertThat(templates.last().name).isEqualTo("New Template 2")
        assertThat(templates.last().items).isEmpty()
    }

    @Test
    fun `renameTemplate updates the name of the correct template`() = runTest {
        val template = PackingListTemplate(id = 5, name = "Old Name")
        fakeDataStore.setInitialData(UserPreferences(templates = listOf(template)))
        viewModel = PackingListViewModel(fakeDataStore)

        viewModel.renameTemplate(templateId = 5, newName = "New Name")
        // UnconfinedTestDispatcher runs coroutines eagerly; no need to advance scheduler

        val updatedTemplate = viewModel.userPreferencesFlow.value.templates.first()
        assertThat(updatedTemplate.name).isEqualTo("New Name")
        assertThat(updatedTemplate.id).isEqualTo(5)
    }

    @Test
    fun `updateItemsForTemplate updates only the target template`() = runTest {
        val t1 = PackingListTemplate(id = 1, name = "T1", items = emptyList())
        val t2 = PackingListTemplate(id = 2, name = "T2", items = emptyList())
        fakeDataStore.setInitialData(UserPreferences(templates = listOf(t1, t2)))
        viewModel = PackingListViewModel(fakeDataStore)

        val newItems = listOf(TemplateItem(id = 1, text = "Sunscreen"))
        viewModel.updateItemsForTemplate(templateId = 1, items = newItems)
        // UnconfinedTestDispatcher runs coroutines eagerly; no need to advance scheduler

        val state = viewModel.userPreferencesFlow.value
        assertThat(state.templates.find { it.id == 1 }?.items).isEqualTo(newItems)
        assertThat(state.templates.find { it.id == 2 }?.items).isEmpty()
    }

    @Test
    fun `deleteTemplate removes the correct template`() = runTest {
        val t1 = PackingListTemplate(id = 1, name = "T1")
        val t2 = PackingListTemplate(id = 2, name = "T2")
        fakeDataStore.setInitialData(UserPreferences(templates = listOf(t1, t2)))
        viewModel = PackingListViewModel(fakeDataStore)

        viewModel.deleteTemplate(templateId = 1)
        // UnconfinedTestDispatcher runs coroutines eagerly; no need to advance scheduler

        val templates = viewModel.userPreferencesFlow.value.templates
        assertThat(templates).hasSize(1)
        assertThat(templates.first().id).isEqualTo(2)
    }

    @Test
    fun `createListFromTemplate creates a list with copied items and emits new list id`() = runTest {
        val templateItems = listOf(TemplateItem(1, "Sunscreen"), TemplateItem(2, "Towel"))
        val template = PackingListTemplate(id = 3, name = "Beach Trip", items = templateItems)
        fakeDataStore.setInitialData(UserPreferences(templates = listOf(template), nextPackingListId = 10))
        viewModel = PackingListViewModel(fakeDataStore)

        viewModel.newListCreatedEvent.test {
            viewModel.createListFromTemplate(templateId = 3)
            val emittedId = awaitItem()
            assertThat(emittedId).isEqualTo(10)
            cancelAndIgnoreRemainingEvents()
        }

        val state = viewModel.userPreferencesFlow.value
        val newList = state.packingLists.find { it.id == 10 }
        assertThat(newList).isNotNull()
        assertThat(newList!!.name).isEqualTo("Beach Trip")
        assertThat(newList.items).hasSize(2)
        assertThat(newList.items.map { it.text }).containsExactly("Sunscreen", "Towel")
        assertThat(newList.items.all { !it.isSelected }).isTrue()
        // Items are assigned fresh global IDs (starting from nextItemId=1 by default)
        assertThat(newList.items.map { it.id }).containsExactly(1, 2).inOrder()
        // nextItemId should be advanced past the copied items
        assertThat(state.nextItemId).isEqualTo(3)
        // Original template should be unchanged
        assertThat(state.templates.first().items).isEqualTo(templateItems)
    }

    @Test
    fun `generateNewTemplateItemId returns 1 for an empty template`() {
        val template = PackingListTemplate(id = 1, name = "Empty", items = emptyList())
        assertThat(viewModel.generateNewTemplateItemId(template)).isEqualTo(1)
    }

    @Test
    fun `generateNewTemplateItemId returns max id plus 1`() {
        val items = listOf(TemplateItem(3, "A"), TemplateItem(7, "B"), TemplateItem(1, "C"))
        val template = PackingListTemplate(id = 1, name = "T", items = items)
        assertThat(viewModel.generateNewTemplateItemId(template)).isEqualTo(8)
    }
}
