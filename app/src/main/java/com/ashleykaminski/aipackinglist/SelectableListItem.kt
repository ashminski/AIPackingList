package com.ashleykaminski.aipackinglist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

@Composable
fun SelectableListItem(
    item: SelectableItem,
    isCurrentlyActiveForDelete: Boolean,
    onItemClick: () -> Unit,
    onItemSelected: (SelectableItem) -> Unit,
    onDeleteItem: (SelectableItem) -> Unit
) {
    val itemTextColor = if (item.isSelected) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    } else {
        LocalContentColor.current
    }

    val textDecoration = if (item.isSelected) {
        TextDecoration.LineThrough
    } else {
        null
    }

    val itemBackgroundColor = when {
        isCurrentlyActiveForDelete -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(itemBackgroundColor)
            .clickable { onItemClick() }
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.isSelected,
            onCheckedChange = { isChecked ->
                onItemSelected(item.copy(isSelected = isChecked))
            },
            colors = CheckboxDefaults.colors(
                checkedColor = if (item.isSelected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                checkmarkColor = MaterialTheme.colorScheme.surface
            )
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = item.text,
            style = MaterialTheme.typography.bodyLarge,
            color = itemTextColor,
            textDecoration = textDecoration,
            modifier = Modifier.weight(1f)
        )

        if (isCurrentlyActiveForDelete) { // Show delete if item is active (regardless of checked state)
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { onDeleteItem(item) }) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete item",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        } else {
            Spacer(modifier = Modifier.width(48.dp)) // Approx. width of an IconButton
        }
    }
}
