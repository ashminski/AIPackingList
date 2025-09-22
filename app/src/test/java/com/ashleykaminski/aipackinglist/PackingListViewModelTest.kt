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
}
