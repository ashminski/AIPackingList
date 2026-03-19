package com.ashleykaminski.aipackinglist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllPackingListsScreen(
    packingLists: List<PackingList>,
    onSelectList: (Int) -> Unit,
    onAddNewList: () -> Unit,
    onRenameList: (listId: Int, newName: String) -> Unit,
    onDeleteList: (Int) -> Unit,
    onNavigateToTemplates: () -> Unit,
    onNavigateToTopics: () -> Unit,
    onNavigateToTripWizard: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    var fabExpanded by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Settings",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                NavigationDrawerItem(
                    label = { Text("Topics") },
                    icon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToTopics()
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Templates") },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigateToTemplates()
                    }
                )
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("My Packing Lists") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    }
                )
            },
            floatingActionButton = {
                Column(horizontalAlignment = Alignment.End) {
                    AnimatedVisibility(visible = fabExpanded) {
                        Column(horizontalAlignment = Alignment.End) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Text("Build from trip", style = MaterialTheme.typography.labelLarge)
                                SmallFloatingActionButton(
                                    onClick = {
                                        fabExpanded = false
                                        onNavigateToTripWizard()
                                    }
                                ) {
                                    Icon(Icons.Filled.Build, contentDescription = "Build from trip")
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Text("Build from template", style = MaterialTheme.typography.labelLarge)
                                SmallFloatingActionButton(
                                    onClick = {
                                        fabExpanded = false
                                        onNavigateToTemplates()
                                    }
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Build from template")
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Text("New list", style = MaterialTheme.typography.labelLarge)
                                SmallFloatingActionButton(
                                    onClick = {
                                        fabExpanded = false
                                        onAddNewList()
                                    }
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "New list")
                                }
                            }
                        }
                    }
                    FloatingActionButton(onClick = { fabExpanded = !fabExpanded }) {
                        Icon(
                            if (fabExpanded) Icons.Filled.Close else Icons.Filled.Add,
                            contentDescription = if (fabExpanded) "Close menu" else "Add"
                        )
                    }
                }
            }
        ) { paddingValues ->
            if (packingLists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No packing lists yet. Tap '+' to add one!")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(packingLists, key = { it.id }) { list ->
                        PackingListCard(
                            list = list,
                            onClick = { onSelectList(list.id) },
                            onRename = { newName -> onRenameList(list.id, newName) },
                            onDelete = { onDeleteList(list.id) }
                        )
                    }
                }
            }
        }
    }
}
