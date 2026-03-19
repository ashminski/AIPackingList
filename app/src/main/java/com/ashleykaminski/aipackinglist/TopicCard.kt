package com.ashleykaminski.aipackinglist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TopicCard(
    topic: TripTopic,
    isSelected: Boolean,
    onToggleSelected: () -> Unit,
    onEditTopic: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = isSelected, onCheckedChange = { onToggleSelected() })
            Text(
                text = topic.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onToggleSelected)
            )
            Text("${topic.items.size}")
            IconButton(onClick = onEditTopic) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Edit topic items")
            }
            if (!topic.isBuiltIn) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete topic")
                }
            }
        }
    }
}
